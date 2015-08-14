package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import org.ambraproject.models.ArticleList;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.util.response.Transceiver;

import java.util.Collection;
import java.util.Set;

public interface ArticleListCrudService {

  /**
   * Create a new article list.
   *
   * @param identity    the identity of the list to create
   * @param displayName the list's display name
   * @param articleIds  the non-empty set of articles that will display the link
   * @return the created collection
   */
  ArticleList create(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds);

  ArticleList update(ArticleListIdentity identity, Optional<String> displayName,
                     Optional<? extends Set<ArticleIdentity>> articleIds);

  Transceiver read(ArticleListIdentity identity, boolean includeArticleMetadata);

  Collection<ArticleList> getContainingLists(ArticleIdentity articleId);

}