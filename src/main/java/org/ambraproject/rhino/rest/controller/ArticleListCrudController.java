package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.view.article.ListInputView;
import org.ambraproject.rhino.view.journal.ArticleListView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class ArticleListCrudController extends RestController {

  @Autowired
  private ArticleListCrudService articleListCrudService;

  private static RestClientException complainAboutRequiredListIdentity(Exception cause) {
    return new RestClientException("type, journal, and key are required", HttpStatus.BAD_REQUEST, cause);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "ListInputView",
      value = "example: {\"journal\": \"PLoSONE\", \"type\": \"admin\", \"key\": \"plosone_news\", " +
          "\"title\": \"test\", \"articleDois\": [\"10.1371/journal.pone.0095668\"]}")
  public ResponseEntity<?> create(HttpServletRequest request) throws IOException {
    final ListInputView inputView;
    try {
      inputView = readJsonFromRequest(request, ListInputView.class);
    } catch (ListInputView.PartialIdentityException e) {
      throw complainAboutRequiredListIdentity(e);
    }

    Optional<ArticleListIdentity> identity = inputView.getIdentity();
    if (!identity.isPresent()) {
      throw complainAboutRequiredListIdentity(null);
    }
    Optional<String> title = inputView.getTitle();
    if (!title.isPresent()) {
      throw new RestClientException("title required", HttpStatus.BAD_REQUEST);
    }
    Optional<ImmutableSet<ArticleIdentifier>> articleDois = inputView.getArticleIds();
    if (!articleDois.isPresent()) {
      throw new RestClientException("articleDois required", HttpStatus.BAD_REQUEST);
    }

    ArticleListView listView = articleListCrudService.create(identity.get(), title.get(), articleDois.get());
    return ServiceResponse.reportCreated(listView).asJsonResponse(entityGson);
  }

  private static RestClientException complainAboutListIdentityOnPatch(Exception cause) {
    return new RestClientException("type, journal, and key cannot be changed with PATCH", HttpStatus.BAD_REQUEST, cause);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}/journals/{journal}/keys/{key}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "ListInputView",
      value = "example #1: {\"title\": \"New Title\"}<br>" +
          "example #2: {\"articleDois\": [\"10.1371/journal.pone.0012345\", \"10.1371/journal.pone.0054321\"]}")
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("type") String type,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("key") String key)
      throws IOException {
    final ListInputView inputView;
    try {
      inputView = readJsonFromRequest(request, ListInputView.class);
    } catch (ListInputView.PartialIdentityException e) {
      throw complainAboutListIdentityOnPatch(e);
    }
    if (inputView.getIdentity().isPresent()) {
      throw complainAboutListIdentityOnPatch(null);
    }

    ArticleListIdentity identity = new ArticleListIdentity(type, journalKey, key);
    articleListCrudService.update(identity, inputView.getTitle(), inputView.getArticleIds());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists", method = RequestMethod.GET)
  public ResponseEntity<?> listAll() throws IOException {
    return articleListCrudService.readAll(Optional.<String>absent(), Optional.<String>absent()).asJsonResponse(entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}", method = RequestMethod.GET)
  public ResponseEntity<?> listAll(@PathVariable("type") String type)
      throws IOException {
    return articleListCrudService.readAll(Optional.of(type), Optional.<String>absent()).asJsonResponse(entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}/journals/{journal}", method = RequestMethod.GET)
  public ResponseEntity<?> listAll(@PathVariable("type") String type,
                                   @PathVariable("journal") String journalKey)
      throws IOException {
    return articleListCrudService.readAll(Optional.of(type), Optional.of(journalKey)).asJsonResponse(entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}/journals/{journal}/keys/{key}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@PathVariable("type") String type,
                                @PathVariable("journal") String journalKey,
                                @PathVariable("key") String key)
      throws IOException {
    ArticleListIdentity identity = new ArticleListIdentity(type, journalKey, key);
    return articleListCrudService.read(identity).asJsonResponse(entityGson);
  }

}
