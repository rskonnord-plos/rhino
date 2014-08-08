package org.ambraproject.rhino.config;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.regex.Pattern.matches;
import static org.ambraproject.rhino.config.ReflectionUtils.getFieldValue;
import static org.ambraproject.rhino.config.ReflectionUtils.setFieldValue;
import static org.codehaus.jackson.map.DeserializationConfig.Feature.AUTO_DETECT_FIELDS;
import static org.codehaus.jackson.map.DeserializationConfig.Feature.AUTO_DETECT_SETTERS;
import static org.codehaus.jackson.map.SerializationConfig.Feature.*;

/**
 * Created by jkrzemien on 8/6/14.
 */
public class MockHttpClient extends HttpClient {

  private ObjectMapper mapper = new ObjectMapper();
  private final File mockDataDir;
  private Set<MockTransaction> mocks = new HashSet<MockTransaction>();
  private static long count = 1;

  public MockHttpClient(String path) {
    this.mockDataDir = new File(path);
    if (!mockDataDir.exists()) {
      mockDataDir.mkdirs();
    }
    mapper.configure(AUTO_DETECT_FIELDS, true);
    mapper.configure(AUTO_DETECT_SETTERS, true);
    mapper.configure(AUTO_DETECT_GETTERS, true);
    mapper.configure(INDENT_OUTPUT, true);
    mapper.configure(FAIL_ON_EMPTY_BEANS, false);

    //loadMockData();
  }

  private void loadMockData() {
    FileReader fr;
    for (File mockFile : mockDataDir.listFiles()) {
      if (!mockFile.getName().endsWith(".json")) {
        continue;
      }
      try {
        fr = new FileReader(mockFile);
        MockTransaction fromMock = mapper.readValue(fr, MockTransaction.class);
        mocks.add(checkNotNull(fromMock));
      } catch (JsonMappingException e) {
        e.printStackTrace();
      } catch (JsonParseException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void CaptureMock(HttpMethod method) {
    FileWriter fw = null;
    try {
      fw = new FileWriter(format("%s/httpMethod%s.json", mockDataDir.getAbsolutePath(), count++));

      MockTransaction transaction = new MockTransaction();
      transaction.request.uri = method.getURI().getURI();
      transaction.request.headers = method.getRequestHeaders();
      transaction.request.params = getFieldValue(((PostMethod) method), "params", Vector.class);
      transaction.request.query = method.getQueryString();
      StringRequestEntity entity = (StringRequestEntity) ((PostMethod) method).getRequestEntity();
      transaction.request.charset = getFieldValue(entity, "charset", transaction.request.charset.getClass());
      transaction.request.contentType = getFieldValue(entity, "contentType", transaction.request.contentType.getClass());
      transaction.request.contentLength = getFieldValue(entity, "contentLength", transaction.request.contentLength.getClass());
      transaction.request.body = new String(getFieldValue(entity, "content", byte[].class), transaction.request.charset);


      mapper.writeValue(fw, transaction);
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
    if (r.uri != null) {
      try {
        if (!m.getURI().getURI().equals(r.uri))
          return false;
      } catch (URIException urie) {
        urie.printStackTrace();
        return false;
      }
    }
    return true;
  }

  private boolean queryMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.query != null) {
      if (m.getQueryString() != null && !m.getQueryString().equals(r.query))
        return false;
    }
    return true;
  }

  private boolean contentMatch(MockTransaction.Request r, PostMethod m) {
    RequestEntity entity = ((PostMethod) m).getRequestEntity();
    byte[] actualBody = getFieldValue(entity, "content", byte[].class);
    String actualCharset = getFieldValue(entity, "charset", String.class);

    if (r.body != null && actualBody != null) {
      if (!r.body.isEmpty()) {
        String actualContent;
        try {
          actualContent = new String(actualBody, actualCharset);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          return false;
        }
        if (!r.body.equalsIgnoreCase(actualContent)) {
          if (!matches(r.body, actualContent)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean paramsMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.params != null && m.getParams() != null) {
      for (Object param : r.params) {
        if (m.getParams().getParameter(param.toString()) == null) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean headersMatch(MockTransaction.Request r, HttpMethod m) {
    if (r.headers != null && m.getRequestHeaders() != null) {
      for (Header actualHeader : m.getRequestHeaders()) {
        boolean found = false;
        for (Header mockHeader : r.headers) {
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

  @Override
  public int executeMethod(HttpMethod method) throws IOException, HttpException {
    mocks.clear();
    loadMockData();
    //CaptureMock(method);
    for (MockTransaction mock : mocks) {
      if (equals(mock.request, method)) {
        if (mock.response.response != null) {
          setFieldValue(method, "responseBody", mock.response.response.getBytes());
        }
        setFieldValue(method, "responseHeaders", mock.response.headers);
        setFieldValue(method, "statusLine", new StatusLine(format("HTTP/1.1 %s DA_CODE", mock.response.status)));
        return method.getStatusCode();
      }
    }
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
