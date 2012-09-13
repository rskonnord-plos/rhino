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
import org.ambraproject.admin.service.AssetCrudService;
import org.ambraproject.admin.service.DoiBasedCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class AssetCrudController extends FileStoreController {

  private static final String ASSET_NAMESPACE = "/asset/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";
  private static final String PARENT_PARAM = "assetOf";

  @Autowired
  private AssetCrudService assetCrudService;

  @Override
  protected DoiBasedCrudService getService() {
    return assetCrudService;
  }

  @Override
  protected String getNamespacePrefix() {
    return ASSET_NAMESPACE;
  }


  /**
   * Dispatch an action to create an article.
   *
   * @param request the HTTP request from a REST client
   * @param file    the uploaded file to use to create an article
   * @return the HTTP response, to indicate success or describe an error
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request, MultipartFile file)
      throws IOException, FileStoreException {
    DoiBasedIdentity assetId = parse(request);

    String parentId = request.getParameter(PARENT_PARAM);
    if (parentId == null) {
      String message = String.format("Creating an asset requires \"?%s=\" with the article DOI", PARENT_PARAM);
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    DoiBasedIdentity articleId = DoiBasedIdentity.forArticle(parentId);

    InputStream stream = null;
    try {
      stream = file.getInputStream();
      assetCrudService.create(stream, assetId, articleId);
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return reportCreated();
  }

  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request) throws FileStoreException, IOException {
    return super.read(request);
  }

  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> update(HttpServletRequest request, @RequestParam(FILE_ARG) MultipartFile file)
      throws IOException, FileStoreException {
    return super.update(request, file);
  }

  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    return super.delete(request);
  }

}
