package org.ambraproject.rhino.mocks;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderGroup;

import java.util.Vector;

/**
 * Created by jkrzemien on 8/6/14.
 */
public class MockTransaction {

  public Request request;
  public Response response;

  public MockTransaction() {
    this.request = new Request();
    this.response = new Response();
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  public Request getRequest() {
    return request;
  }

  public Response getResponse() {
    return response;
  }

  class Request {
    private String uri;
    private String query;
    private Header[] headers;
    private Vector params;
    private String body = "";
    private String charset = "UTF-8";
    private String contentType = "application/xml; charset=UTF-8";
    private Long contentLength = 0L;

    public Request() {
    }

    public void setBody(String body) {
      this.body = body;
    }

    public void setHeaders(Header[] headers) {
      this.headers = headers;
    }

    public void setCharset(String charset) {
      this.charset = charset;
    }

    public void setContentLength(Long contentLength) {
      this.contentLength = contentLength;
    }

    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    public void setParams(Vector params) {
      this.params = params;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public void setUri(String uri) {
      this.uri = uri;
    }

    public String getCharset() {
      return charset;
    }

    public String getBody() {
      return body;
    }

    public Header[] getHeaders() {
      return headers;
    }

    public Long getContentLength() {
      return contentLength;
    }

    public String getContentType() {
      return contentType;
    }

    public String getQuery() {
      return query;
    }

    public String getUri() {
      return uri;
    }

    public Vector getParams() {
      return params;
    }
  }

  class Response {
    private Header[] headers = new HeaderGroup().getAllHeaders();
    private int status = 200;
    private String body;

    public Response() {
    }

    public void setHeaders(Header[] headers) {
      this.headers = headers;
    }

    public void setStatus(int status) {
      this.status = status;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public Header[] getHeaders() {
      return headers;
    }

    public int getStatus() {
      return status;
    }

    public String getBody() {
      return body;
    }
  }
}