/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AssetCrudService;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.List;

@Controller
public class AssetFileCrudController extends RestController {

  @Autowired
  private AssetCrudService assetCrudService;
  @Autowired
  private ContentRepoService contentRepoService;


  private static final Joiner REPROXY_URL_JOINER = Joiner.on(' ');
  private static final int REPROXY_CACHE_FOR_VALUE = 6 * 60 * 60; // TODO: Make configurable
  private static final String REPROXY_CACHE_FOR_HEADER =
      REPROXY_CACHE_FOR_VALUE + "; Last-Modified Content-Type Content-Disposition";

  private void serve(HttpServletRequest request, HttpServletResponse response, RepoObjectMetadata objMeta)
      throws IOException {
    objMeta.getContentType().ifPresent((String contentType) -> {
      response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    });

    objMeta.getDownloadName().ifPresent((String downloadName) -> {
      String contentDisposition = "attachment; filename=" + downloadName;
      response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
    });

    Timestamp timestamp = objMeta.getTimestamp();
    setLastModifiedHeader(response, timestamp);
    if (!checkIfModifiedSince(request, timestamp)) {
      response.setStatus(HttpStatus.NOT_MODIFIED.value());
      return;
    }

    List<URL> reproxyUrls = objMeta.getReproxyUrls();
    if (clientSupportsReproxy(request) && reproxyUrls != null && !reproxyUrls.isEmpty()) {
      String reproxyUrlHeader = REPROXY_URL_JOINER.join(reproxyUrls);

      response.setStatus(HttpStatus.OK.value());
      response.setHeader("X-Reproxy-URL", reproxyUrlHeader);
      response.setHeader("X-Reproxy-Cache-For", REPROXY_CACHE_FOR_HEADER);
    } else {
      try (InputStream fileStream = contentRepoService.getRepoObject(objMeta.getVersion());
           OutputStream responseStream = response.getOutputStream()) {
        ByteStreams.copy(fileStream, responseStream);
      }
    }
  }

  private boolean clientSupportsReproxy(HttpServletRequest request) {
    Enumeration headers = request.getHeaders("X-Proxy-Capabilities");
    if (headers == null) {
      return false;
    }
    while (headers.hasMoreElements()) {
      if ("reproxy-file".equals(headers.nextElement())) {
        return true;
      }
    }
    return false;
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{articleDoi}/ingestions/{number}/items/{itemDoi}/files/{filetype}", method = RequestMethod.GET)
  public void readMetadata(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable("articleDoi") String articleDoi,
                           @PathVariable("number") int ingestionNumber,
                           @PathVariable("itemDoi") String itemDoi,
                           @PathVariable("filetype") String fileType)
      throws IOException {
    ArticleFileIdentifier fileId = ArticleFileIdentifier.create(DoiEscaping.unescape(itemDoi), ingestionNumber, fileType);
    // TODO: Validate that articleDoi belongs to item's parent

    // TODO: Add support for serving metadata about individual files?
    throw new RestClientException("File metadata not supported (URL reserved)", HttpStatus.NOT_FOUND);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{articleDoi}/ingestions/{number}/items/{itemDoi}/files/{filetype}",
      params = "download", method = RequestMethod.GET)
  public void serveFile(HttpServletRequest request, HttpServletResponse response,
                        @PathVariable("articleDoi") String articleDoi,
                        @PathVariable("number") int ingestionNumber,
                        @PathVariable("itemDoi") String itemDoi,
                        @PathVariable("filetype") String fileType)
      throws IOException {
    ArticleFileIdentifier fileId = ArticleFileIdentifier.create(DoiEscaping.unescape(itemDoi), ingestionNumber, fileType);
    // TODO: Validate that articleDoi belongs to item's parent

    RepoObjectMetadata objectMetadata = assetCrudService.getArticleItemFile(fileId);
    serve(request, response, objectMetadata);
  }

}
