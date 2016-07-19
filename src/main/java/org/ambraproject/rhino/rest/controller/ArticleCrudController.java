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

import com.wordnik.swagger.annotations.ApiOperation;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.service.impl.RecentArticleQuery;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.SyndicationInputView;
import org.ambraproject.rhombat.HttpDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);


  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private CommentCrudService commentCrudService;

  @Autowired
  private AssetFileCrudController assetFileCrudController;

  @Autowired
  private ArticleListCrudService articleListCrudService;

  @Autowired
  private SyndicationCrudService syndicationCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET)
  public void listDois(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(value = "date", required = false) String includeLastModifiedDate,
                       @RequestParam(value = "state", required = false) String[] pubStates,
                       @RequestParam(value = "syndication", required = false) String[] syndStatuses)
      throws IOException {
    ArticleCriteria articleCriteria = ArticleCriteria.create(asList(pubStates), asList(syndStatuses),
        booleanParameter(includeLastModifiedDate));
    articleCrudService.listDois(articleCriteria).respond(request, response, entityGson);
  }

  /*
   * Null-safe utility method for Arrays.asList. Put somewhere for reuse?
   */
  private static <E> List<E> asList(E[] array) {
    return (array == null) ? null : Arrays.asList(array);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", params = {"since", "journal"}, method = RequestMethod.GET)
  public void listRecent(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(value = "since", required = true) String since,
                         @RequestParam(value = "journal", required = true) String journalKey,
                         @RequestParam(value = "min", required = false) Integer minimum,
                         @RequestParam(value = "type", required = false) String[] articleTypes,
                         @RequestParam(value = "exclude", required = false) String[] typesToExclude)
      throws IOException {
    RecentArticleQuery query = RecentArticleQuery.builder()
        .setJournalKey(journalKey)
        .setThreshold(HttpDateUtil.parse(since))
        .setArticleTypes(asList(articleTypes))
        .setExcludedArticleTypes(asList(typesToExclude))
        .setMinimum(minimum == null || minimum == 0 ? null : minimum)
        .build();
    articleCrudService.listRecent(query).respond(request, response, entityGson);
  }

  /**
   * Read article metadata.
   *
   * @throws IOException
   */
  @Deprecated
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("doi") String doi,
                   @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.resolve(doi), ingestionNumber);
    articleCrudService.readArticleMetadata(ingestionId).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi:.+}", method = RequestMethod.GET)
  public void getRevisions(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    articleCrudService.readArticleOverview(id).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing comments associated with the article. Each comment has a "replies" list
   * that contains any replies (recursively).
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/comments", method = RequestMethod.GET)
  public void readComments(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    commentCrudService.readComments(id).respond(request, response, entityGson);
  }

  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi:.+}", method = RequestMethod.GET, params = "commentCount")
  public void getCommentCount(HttpServletRequest request, HttpServletResponse response,
                              @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    ArticleTable article = articleCrudService.getArticle(id);
    commentCrudService.getCommentCount(article).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing the authors of the article. While the article metadata contains author
   * names, this list will contain more author information than the article metadata, such as author affiliations,
   * corresponding author, etc.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/authors", method = RequestMethod.GET, params = {"authors"})
  public void readAuthors(HttpServletRequest request, HttpServletResponse response,
                          @PathVariable("doi") String doi,
                          @PathVariable("number") int ingestionNumber)
      throws IOException {

    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.resolve(doi), ingestionNumber);
    articleCrudService.readAuthors(ingestionId).respond(request, response, entityGson);
  }

  /**
   * Retrieves the XML file containing the text of an article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}", method = RequestMethod.GET, params = "xml")
  public void readXml(HttpServletRequest request, HttpServletResponse response,
                      @PathVariable("doi") String doi,
                      @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.resolve(doi), ingestionNumber);
    ArticleItemIdentifier itemId = ingestionId.getItemFor();
    ArticleFileIdentifier fileId = ArticleFileIdentifier.create(itemId, "manuscript");
    assetFileCrudController.serveFile(request, response, fileId);
  }

  /**
   * Populates article category information by making a call to the taxonomy server.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.POST)
  public void populateCategories(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    articleCrudService.populateCategories(articleId);

    // Report the current categories
    articleCrudService.readCategories(articleId).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET)
  public void readCategories(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    articleCrudService.readCategories(articleId).respond(request, response, entityGson);
  }

  /**
   * A temporary endpoint for testing the creation of article relationships This functionality should ultimately be
   * subsumed under the publication and revision assignment workflow.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Deprecated
  @Transactional(readOnly = false)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/relationships", method = RequestMethod.POST)
  public void refreshArticleRelationships(HttpServletRequest request, HttpServletResponse response,
                                          @PathVariable("doi") String doi,
                                          @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.resolve(doi), ingestionNumber);
    ArticleRevisionIdentifier articleRevId = null; // TODO: Refactor ArticleCrudService.refreshArticleRelationships to take ArticleIngestionIdentifier
    articleCrudService.refreshArticleRelationships(articleRevId);
  }

  /**
   * Retrieves a list of objects representing raw taxonomy categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET, params = "raw")
  public void getRawCategories(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    articleCrudService.getRawCategories(articleId).respond(request, response, entityGson);
  }

  /**
   * Retrieves the raw taxonomy categories associated with the article along with the text that is sent to the taxonomy
   * server for classification
   *
   * @param request
   * @return a String containing the text and raw categories in the form of <text> \n\n <categories>
   * @throws IOException
   */
  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET, params = "rawCategoriesAndText")
  public ResponseEntity<String> getRawCategoriesAndText(HttpServletRequest request,
                                                        @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.resolve(doi));

    String categoriesAndText = articleCrudService.getRawCategoriesAndText(articleId);
    HttpHeaders responseHeader = new HttpHeaders();
    responseHeader.setContentType(MediaType.TEXT_HTML);
    return new ResponseEntity<>(categoriesAndText, responseHeader, HttpStatus.OK);
  }

  /**
   * Retrieves a collection of article lists that contain an article.
   */
  @Transactional(readOnly = true)
  @RequestMapping(
      // Not "/articles/{doi}/lists" because a list isn't a child object of the article. This is kind of a search query.
      value = "/articles/{doi:.+}", method = RequestMethod.GET, params = "lists")
  public void getContainingLists(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.resolve(doi));
    articleListCrudService.readContainingLists(id).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.POST)
  public ResponseEntity<Object> createSyndication(HttpServletRequest request,
                                                  @PathVariable("doi") String doi,
                                                  @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.resolve(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    syndicationCrudService.createSyndication(revisionId, input.getTargetQueue());
    return reportCreated();
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications",
      // Fold into PATCH operation so we can get rid of "?syndicate"?
      method = RequestMethod.POST, params = "syndicate")
  @ApiOperation(value = "syndicate", notes = "Send a syndication message to the queue for processing. " +
      "Will create and add a syndication to the database if none exist for current article and target.")
  public ResponseEntity<Object> syndicate(HttpServletRequest request,
                                          @PathVariable("doi") String doi,
                                          @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.resolve(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication created = syndicationCrudService.syndicate(revisionId, input.getTargetQueue());
    return reportOk(created.toString());
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.PATCH)
  public ResponseEntity<Object> patchSyndication(HttpServletRequest request,
                                                 @PathVariable("doi") String doi,
                                                 @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.resolve(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication patched = syndicationCrudService.updateSyndication(revisionId,
        input.getTargetQueue(), input.getStatus(), input.getErrorMessage());
    return reportOk(patched.toString());
  }

  /**
   * Retrieves the metadata from a random article
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @return a JSON representation of the random article
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET, params = "random")
  public void readRandom(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    articleCrudService.readRandom().respond(request, response, entityGson);
  }

}
