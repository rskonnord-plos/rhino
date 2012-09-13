/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.admin.controller;

import org.ambraproject.admin.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Controller that sends HTTP responses to RESTful requests.
 */
public abstract class RestController {

  private static final Logger log = LoggerFactory.getLogger(RestController.class);

  /**
   * Report that a RESTful operation succeeded. The returned object (if returned from a {@link RequestMapping}) will
   * cause the REST response to indicate an "OK" HTTP status and have an empty response body.
   *
   * @return a response indicating "OK"
   */
  protected ResponseEntity<Object> reportOk() {
    return new ResponseEntity<Object>(HttpStatus.OK);
  }

  /**
   * Report that a RESTful operation to create an entity succeeded. The returned object (if returned from a {@link
   * RequestMapping}) will cause the REST response to indicate a "Created" HTTP status and have an empty response body.
   *
   * @return a response indicating "Created"
   */
  protected ResponseEntity<Object> reportCreated() {
    return new ResponseEntity<Object>(HttpStatus.CREATED);
  }

  /**
   * Report an error condition to the REST client. The brief error message is sent as the response body, with the
   * response code specified when the exception object was created. The stack trace is not included because we generally
   * expect the client to fix the error with a simple change to input.
   *
   * @param e the exception that Spring wants to handle
   * @return the RESTful response body
   */
  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<String> reportClientError(RestClientException e) {
    log.info("Reporting error to client", e);
    return new ResponseEntity<String>(e.getMessage(), e.getResponseStatus());
  }

  /**
   * Display a server-side error to the rest client. This is meant generally to handle bugs and configuration errors.
   * Because this is assumed to be caused by programmer error, the stack trace is sent in the request body.
   *
   * @param e the exception that Spring wants to handle
   * @return the RESTful response body
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> reportServerError(Exception e) {
    log.error("Exception from controller", e);
    StringWriter report = new StringWriter();
    e.printStackTrace(new PrintWriter(report));

    return new ResponseEntity<String>(report.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

}
