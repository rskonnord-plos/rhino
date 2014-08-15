package org.ambraproject.rhino.mocks;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderGroup;
import org.apache.commons.httpclient.StatusLine;

import java.util.Vector;

/**
 * Created by jkrzemien on 8/6/14.
 */
public class MockTransaction {

  public final Request request;
  public final Response response;

  public MockTransaction() {
    this.request = new Request();
    this.response = new Response();
  }

  class Request {
    public String uri;
    public String query;
    public Header[] headers;
    public Vector params;
    public String body = "";
    public String charset = "UTF-8";
    public String contentType = "application/xml; charset=UTF-8";
    public Long contentLength = 0L;

    public Request() {
    }
  }

  class Response {
    public Header[] headers = new HeaderGroup().getAllHeaders();
    public int status = 200;
    public String body;

    public Response() {
    }
  }
}