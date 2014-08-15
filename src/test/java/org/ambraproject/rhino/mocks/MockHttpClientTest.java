package org.ambraproject.rhino.mocks;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;

/**
 * Created by jkrzemien on 8/13/14.
 */

public class MockHttpClientTest {

  private MockHttpClient classUnderTest = new MockHttpClient("/tmp/mockHttpClientTests");
  private HttpMethod method;

  @Before
  public void setUp() {
    this.method = new PostMethod();
    classUnderTest.clearMockData();
  }

  @Test
  public void no_mock_data_returns_evil_http_code() throws IOException {
    int code = classUnderTest.executeMethod(method);
    assertEquals(code, 666);
  }

  @Test
  public void empty_transactions_always_matches() throws IOException {
    MockTransaction mt = new MockTransaction();
    classUnderTest.addMockData(mt);
    int code = classUnderTest.executeMethod(method);
    assertEquals(code, mt.response.status);
    assertEquals(method.getResponseHeaders(), mt.response.headers);
    assertEquals(method.getResponseBody(), mt.response.body);
  }

  @Test
  public void mock_response_headers_get_inserted_correctly_on_method() throws IOException {
    HeaderGroup headers = new HeaderGroup();
    headers.addHeader(new Header("a", "1"));

    MockTransaction mt = new MockTransaction();
    mt.response.headers = headers.getAllHeaders();

    classUnderTest.addMockData(mt);
    int code = classUnderTest.executeMethod(method);

    assertEquals(code, mt.response.status);
    assertEquals(method.getResponseHeaders(), mt.response.headers);
  }

  @Test
  public void request_headers_matches_empty_mock_headers() throws IOException {
    MockTransaction mt = new MockTransaction();
    classUnderTest.addMockData(mt);
    method.addRequestHeader(new Header("a", "1"));
    int code = classUnderTest.executeMethod(method);

    assertEquals(code, mt.response.status);
    assertEquals(method.getResponseHeaders(), mt.response.headers);
  }

  @Test
  public void request_headers_matches_equal_mock_headers() throws IOException {
    MockTransaction mt = new MockTransaction();
    classUnderTest.addMockData(mt);
    Header requestHeader = new Header("a", "1");
    HeaderGroup hg = new HeaderGroup();
    hg.addHeader(requestHeader);
    method.addRequestHeader(requestHeader);
    mt.request.headers = hg.getAllHeaders();
    int code = classUnderTest.executeMethod(method);

    assertEquals(code, mt.response.status);
    assertEquals(method.getResponseHeaders(), mt.response.headers);
  }

  @Test
  public void request_headers_matches_does_not_equal_mock_headers() throws IOException {
    Header requestHeader = new Header("a", "1");
    method.addRequestHeader(requestHeader);

    Header mockHeader = new Header("b", "2");
    HeaderGroup hg = new HeaderGroup();
    hg.addHeader(mockHeader);

    MockTransaction mt = new MockTransaction();
    mt.request.headers = hg.getAllHeaders();

    classUnderTest.addMockData(mt);
    int code = classUnderTest.executeMethod(method);

    assertEquals(code, 666);
  }

  @Test
  public void request_body_matches_mock_body() throws IOException {
    MockTransaction mt = new MockTransaction();

    classUnderTest.addMockData(mt);
    int code = classUnderTest.executeMethod(method);

    assertEquals(code, 666);
  }

}
