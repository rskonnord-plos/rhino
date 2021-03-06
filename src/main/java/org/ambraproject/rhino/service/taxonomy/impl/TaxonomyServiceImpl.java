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

package org.ambraproject.rhino.service.taxonomy.impl;

import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleCategoryAssignmentFlag;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.WeightedTerm;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class TaxonomyServiceImpl extends AmbraService implements TaxonomyService {

  @Autowired
  private TaxonomyClassificationService taxonomyClassificationService;

  @Override
  public List<WeightedTerm> classifyArticle(Article article, Document articleXml) {
    return taxonomyClassificationService.classifyArticle(article, articleXml);
  }

  @Override
  public List<String> getRawTerms(Document articleXml, Article article, boolean isTextRequired) throws IOException {
    return taxonomyClassificationService.getRawTerms(articleXml, article, isTextRequired);
  }

  @Override
  public void populateCategories(ArticleRevision revision) {
    taxonomyClassificationService.populateCategories(revision);
  }

  @Override
  public Collection<ArticleCategoryAssignment> getAssignmentsForArticle(Article article) {
    return taxonomyClassificationService.getAssignmentsForArticle(article);
  }

  @Override
  public Collection<Category> getArticleCategoriesWithTerm(Article article, String term) {
    return taxonomyClassificationService.getArticleCategoriesWithTerm(article, term);
  }


  private List<ArticleCategoryAssignmentFlag> getArticleCategoryAssignmentFlags(Article article, Category category, Optional<Long> userProfileId) {
    return hibernateTemplate.execute(session -> {
      Query query = userProfileId
          .map(userProfileIdValue -> session.createQuery("" +
              "FROM ArticleCategoryAssignmentFlag " +
              "WHERE article = :article AND category = :category AND userProfileId = :userProfileId")
              .setParameter("userProfileId", userProfileIdValue))
          .orElseGet(() -> session.createQuery("" +
              "FROM ArticleCategoryAssignmentFlag " +
              "WHERE article = :article AND category = :category AND userProfileId IS NULL"));
      query.setParameter("article", article);
      query.setParameter("category", category);
      return (List<ArticleCategoryAssignmentFlag>) query.list();
    });
  }

  @Override
  public void flagArticleCategory(Article article, Category category, Optional<Long> userProfileId) throws IOException {
    if (userProfileId.isPresent()) {
      List<ArticleCategoryAssignmentFlag> flags = getArticleCategoryAssignmentFlags(article, category, userProfileId);
      if (!flags.isEmpty()) {
        // The system already has a flag for this assignment from this user. No need to create a redundant one.
        return;
      }
    }

    ArticleCategoryAssignmentFlag flag = new ArticleCategoryAssignmentFlag();
    flag.setArticle(article);
    flag.setCategory(category);
    flag.setUserProfileId(userProfileId.orElse(null));
    hibernateTemplate.save(flag);
  }

  @Override
  public void deflagArticleCategory(Article article, Category category, Optional<Long> userProfileId) throws IOException {
    List<ArticleCategoryAssignmentFlag> flags = getArticleCategoryAssignmentFlags(article, category, userProfileId);
    //we expect an empty list here if no flags have been added for this category/user. In this case, do nothing.
    // TODO: only allow removal of flags if there is a currently existing flag for that category
    if (!flags.isEmpty()) {
      if (userProfileId.isPresent()) {
        // An individual user deflagged the category. Under normal circumstances, they should have only one flag.
        // In case there are multiple flags for the same user, delete all of them.
        hibernateTemplate.deleteAll(flags);
      } else {
        // An anonymous user deflagged the category, in the same session in which they flagged it.
        // Delete a single anonymous flag, chosen arbitrarily, assuming that this will balance out the flag they created.
        // There is a small security vulnerability here: a user could submit many bogus "deflag" requests
        // and delete everyone else's anonymous flags.
        hibernateTemplate.delete(flags.get(0));
      }
    }
  }

  @Override
  public List<ArticleCategoryAssignmentFlag> getFlagsCreatedOn(LocalDate fromDate, LocalDate toDate) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleCategoryAssignmentFlag " +
          "WHERE created >= :fromDate AND created < :toDate");
      query.setParameter("fromDate", java.sql.Date.valueOf(fromDate));
      query.setParameter("toDate", java.sql.Date.valueOf(toDate.plusDays(1)));
      return (List<ArticleCategoryAssignmentFlag>) query.list();
    });
  }
}
