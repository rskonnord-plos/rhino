package org.ambraproject.rhino.mocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.rhino.config.mocks.SimulatorConfig;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.regex.Pattern.*;
import static org.ambraproject.rhino.config.ReflectionUtils.getFieldValue;
import static org.ambraproject.rhino.config.ReflectionUtils.setFieldValue;

/**
 * Created by jkrzemien on 8/6/14.
 */
public class MockHttpClient extends HttpClient {

  private final static Logger log = LoggerFactory.getLogger(MockHttpClient.class);

  private final File mockDataDir;
  private Set<MockTransaction> mocks = new HashSet<MockTransaction>();
  private static long count = 1;
  private SimulatorConfig simulatorConfig;
  private HttpClient delegate = new HttpClient();
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public MockHttpClient(SimulatorConfig simulatorConfig) {
    this.simulatorConfig = simulatorConfig;
    this.mockDataDir = new File(getClass().getResource(simulatorConfig.getMockDataFolder()).getPath());
    if (!mockDataDir.exists()) {
      mockDataDir.mkdirs();
    }
  }

  private void loadMockData() {
    mocks.clear();
    FileReader fr;
    for (File mockFile : mockDataDir.listFiles()) {
      if (!mockFile.getName().endsWith(".json")) {
        continue;
      }
      log.info(format("Loading mock data from file: %s", mockFile.getName()));
      try {
        fr = new FileReader(mockFile);
        MockTransaction fromMock = gson.fromJson(fr, MockTransaction.class);
        mocks.add(checkNotNull(fromMock));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void addMockData(MockTransaction mockTransaction) {
    mocks.add(checkNotNull(mockTransaction));
  }

  public void captureMockData(HttpMethod method) {
    FileWriter fw = null;
    try {
      fw = new FileWriter(format("%s/httpMethod%s.json", mockDataDir.getAbsolutePath(), count++));

      MockTransaction transaction = new MockTransaction();
      transaction.getRequest().setUri(method.getURI().getURI());
      transaction.getRequest().setHeaders(method.getRequestHeaders());
      transaction.getRequest().setParams(getFieldValue(((PostMethod) method), "params", Vector.class));
      transaction.getRequest().setQuery(method.getQueryString());
      StringRequestEntity entity = (StringRequestEntity) ((PostMethod) method).getRequestEntity();
      transaction.getRequest().setCharset(getFieldValue(entity, "charset", transaction.getRequest().getCharset().getClass()));
      transaction.getRequest().setContentType(getFieldValue(entity, "contentType", transaction.getRequest().getContentType().getClass()));
      transaction.getRequest().setContentLength(getFieldValue(entity, "contentLength", transaction.getRequest().getContentLength().getClass()));
      transaction.getRequest().setBody(new String(getFieldValue(entity, "content", byte[].class), transaction.getRequest().getCharset()));

      transaction.getResponse().setStatus(method.getStatusLine().getStatusCode());
      transaction.getResponse().setHeaders(method.getResponseHeaders());
      transaction.getResponse().setBody(method.getResponseBodyAsString());

      fw.write(gson.toJson(transaction));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      if (fw != null) {
        try {
          fw.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
    }
  }

  private boolean uriMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.getUri() != null) {
      try {
        if (!m.getURI().getURI().equals(r.getUri()))
          return false;
      } catch (URIException urie) {
        urie.printStackTrace();
        return false;
      }
    }
    return true;
  }

  private boolean queryMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.getQuery() != null) {
      if (m.getQueryString() != null && !m.getQueryString().equals(r.getQuery()))
        return false;
    }
    return true;
  }

  private boolean contentMatch(MockTransaction.Request r, PostMethod m) {
    RequestEntity entity = ((PostMethod) m).getRequestEntity();
    byte[] actualBody = getFieldValue(entity, "content", byte[].class);
    String actualCharset = getFieldValue(entity, "charset", String.class);

    if (r.getBody() != null && actualBody != null) {
      if (!r.getBody().isEmpty()) {
        String actualContent;
        try {
          actualContent = new String(actualBody, actualCharset);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return false;
        }
        if (!r.getBody().equalsIgnoreCase(actualContent)) {
          Pattern p = Pattern.compile(r.getBody(), DOTALL + MULTILINE + CASE_INSENSITIVE);
          Matcher matcher = p.matcher(actualContent);
          if (!matcher.matches()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean paramsMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.getParams() != null && m.getParams() != null) {
      for (Object param : r.getParams()) {
        if (m.getParams().getParameter(param.toString()) == null) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean headersMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.getHeaders() != null && m.getRequestHeaders() != null) {
      for (Header actualHeader : m.getRequestHeaders()) {
        boolean found = false;
        for (Header mockHeader : r.getHeaders()) {
          if (actualHeader.equals(mockHeader)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean equals(MockTransaction.Request r, HttpMethod m) {
    if (r == null || m == null) return false;

    if (!uriMatch(r, m)) return false;

    if (!queryMatch(r, m)) return false;

    if (!headersMatch(r, m)) return false;

    if (!paramsMatch(r, m)) return false;

    if (!contentMatch(r, (PostMethod) m)) return false;

    return true;
  }

  public void clearMockData() {
    mocks.clear();
  }

  private void setResponseHeaders(HttpMethod method, MockTransaction mock) {
    HeaderGroup headerGroup = new HeaderGroup();
    if (mock.getResponse().getHeaders() != null) {
      for (Header header : mock.getResponse().getHeaders()) {
        headerGroup.addHeader(header);
      }
    }
    setFieldValue(method, "responseHeaders", headerGroup);
  }

  @Override
  public int executeMethod(HttpMethod method) throws IOException, HttpException {
    log.info("HTTP request received!");
    if (simulatorConfig.isCaptureMockData()) {
      log.info("Capture mode ACTIVE, forwarding call to real HTTP client...");
      int code = delegate.executeMethod(method);
      log.info("Capturing transaction as mock data...");
      captureMockData(method);
      log.info("Returning to callee");
      return code;
    }
    loadMockData();
    log.info(format("Attempting to match request against %s known mock transactions...", mocks.size()));
    for (MockTransaction mock : mocks) {
      if (equals(mock.getRequest(), method)) {
        log.info("Mock transaction matched request! Building HTTP response from mock...");
        if (mock.getResponse().getBody() != null) {
          setFieldValue(method, "responseBody", mock.getResponse().getBody().getBytes());
        }
        setResponseHeaders(method, mock);
        setFieldValue(method, "statusLine", new StatusLine(format("HTTP/1.1 %s DA_CODE", mock.getResponse().getStatus())));
        return method.getStatusCode();
      }
    }
    log.info("No mock transaction matched the request! Maybe you are missing some mock data files or need to capture/create new ones for this scenario...");
    return 666; // CHAN!
  }

  @Override
  public int executeMethod(HostConfiguration hostConfiguration, HttpMethod method) throws IOException, HttpException {
    return executeMethod(method);
  }

  @Override
  public int executeMethod(HostConfiguration hostconfig, HttpMethod method, HttpState state) throws IOException, HttpException {
    return executeMethod(method);
  }

}
