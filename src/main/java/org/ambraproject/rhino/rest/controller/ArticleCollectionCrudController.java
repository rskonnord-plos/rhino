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

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleCollectionCrudService;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ArticleCollectionCrudController extends RestController {

  private static final String COLLECTION_ROOT = "/article-collection";
  private static final String COLLECTION_NAMESPACE = COLLECTION_ROOT + '/';
  private static final String COLLECTION_TEMPLATE = COLLECTION_NAMESPACE + "**";

  @Autowired
  private ArticleCollectionCrudService articleCollectionCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = COLLECTION_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam() String key)
      throws IOException {
    articleCollectionCrudService.read(key).respond(request, response, entityGson);
  }

//  @Transactional(rollbackFor = {Throwable.class})
//  @RequestMapping(value = COLLECTION_TEMPLATE, method = RequestMethod.PATCH)
//  public void update(HttpServletRequest request, HttpServletResponse response,
//                     @RequestParam() String key,
//                     @RequestParam() String title,
//                     @RequestParam() String[] article_dois)
//      throws IOException {
//
//    articleCollectionCrudService.update(key, title, article_dois);
//  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = COLLECTION_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<String> create(HttpServletRequest request,
                                       @RequestParam() String key,
                                       @RequestParam() String title,
                                       @RequestParam() String[] article_dois)
   throws IOException {


    return articleCollectionCrudService.create(key, title, article_dois);
  }

}
