package org.ambraproject.rhino.rest.controller;

import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.CollectionCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import java.util.Set;

@Controller
public class CollectionCrudController extends RestController {

  @Autowired
  private CollectionCrudService collectionCrudService;

  private static Set<ArticleIdentity> asArticleIdentities(String[] articleDois) {
    if (articleDois == null) return ImmutableSet.of();
    ImmutableSet.Builder<ArticleIdentity> articleIdentities = ImmutableSet.builder();
    for (String articleDoi : articleDois) {
      articleIdentities.add(ArticleIdentity.create(articleDoi));
    }
    return articleIdentities.build();
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/collections", method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam(value = "journal", required = true) String journalKey,
                                  @RequestParam(value = "slug", required = true) String slug,
                                  @RequestParam(value = "title", required = true) String title,
                                  @RequestParam(value = "articles", required = false) // none means means empty collection
                                      String[] articleDois)
      throws IOException {
    collectionCrudService.create(journalKey, slug, title, asArticleIdentities(articleDois));
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /*
   * API design note: This, for now, is inconsistent with our other PATCH method (on article state). This one uses
   * request parameters to be consistent with its POST. The other one uses a JSON request body to be consistent with its
   * GET.
   *
   * TODO: Make PATCH requests consistent once we decide how we want to do it
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/collections/{journal}/{slug}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(@PathVariable("journal") String journalKey,
                                  @PathVariable("slug") String slug,
                                  @RequestParam(value = "title", required = false) // none means don't update
                                      String title,
                                  @RequestParam(value = "articles", required = false) // none means ?????  TODO
                                      String[] articleDois)
      throws IOException {
    Set<ArticleIdentity> articleIds = asArticleIdentities(articleDois);
    collectionCrudService.update(journalKey, slug, title, articleIds);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/collections/{journal}/{slug}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("slug") String slug)
      throws IOException {
    collectionCrudService.read(journalKey, slug).respond(request, response, entityGson);
  }

}
