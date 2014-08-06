package org.ambraproject.rhino.util.response;

import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;

/**
 * An object that a mock service may return to a controller under test.
 */
public abstract class MockTransceiver extends Transceiver {

  @Deprecated
  @Override
  protected final Object getData() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  protected final Calendar getLastModifiedDate() throws IOException {
    throw new UnsupportedOperationException();
  }

  // Override production behavior and instead delegate to a hook for testing behavior
  @Override
  final void doResponse(HttpServletRequest request, HttpServletResponse response, Gson gson) throws IOException {
    mockRespond();
  }

  /**
   * The action to carry out when a controller invokes this transceiver for an HTTP response. In a testing environment,
   * there is no {@link HttpServletResponse} object for it to act upon.
   *
   * @throws IOException
   */
  protected abstract void mockRespond() throws IOException;

}
