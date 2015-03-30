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

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.models.ArticleCollection;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCollectionCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.ArticleCollectionOutputView;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ArticleCollectionCrudServiceImpl extends AmbraService implements ArticleCollectionCrudService {

  @Override
  public Transceiver read(final String key) throws IOException {
    return new EntityTransceiver<ArticleCollection>() {
      @Override
      protected ArticleCollection fetchEntity() {
        ArticleCollection articleCollection = (ArticleCollection) DataAccessUtils.uniqueResult((List<?>)
            hibernateTemplate.findByCriteria(DetachedCriteria
                    .forClass(ArticleCollection.class)
                    .add(Restrictions.eq("key", key))
            ));
        if (articleCollection == null) {
          throw new RestClientException("Collection not found with key=" + key, HttpStatus.NOT_FOUND);
        }
        return articleCollection;
      }

      @Override
      protected Object getView(ArticleCollection articleCollection) {
        return new ArticleCollectionOutputView(articleCollection);
      }
    };
  }


  private ArticleCollection findCollection(String key) {
    return (ArticleCollection) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(ArticleCollection.class)
                .add(Restrictions.eq("key", key))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }


  /**
   * Get a list of articles for a given collection
   *
   * @param key unique String key for the collection
   * @return a list of ArticleIssue objects that wrap each issue with its associated journal and volume objects
   */
  @Transactional(readOnly = true)
  @Override
  public List<Article> getCollectionArticles(final String key) {
    return (List<Article>) hibernateTemplate.execute(new HibernateCallback() {
      public Object doInHibernate(Session session) throws HibernateException, SQLException {
        List<Object[]> queryResults = session.createSQLQuery(
                "select a.doi " +
                        "from collection c " +
                        "join articleCollectionJoinTable acjt on c.collectionID = acjt.collectionID " +
                        "join article a on acjt.articleID = a.articleID " +
                        "where c.key = :key " +
                        "order by i.created desc ")
                .setString("key", key)
                .list();

        List<Article> articles = new ArrayList<>(queryResults.size());
        for (Object[] row : queryResults) {
          String doi = (String) row[0];
          articles.add(new Article(doi));
        }

        return articles;
      }
    });
  }

  @Override
  public void create(String key, String title, Collection<String> articleDois) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(title);

    if (findCollection(key) != null) {
      throw new RestClientException("Collection already exists with key: " + key, HttpStatus.BAD_REQUEST);
    }

  }

}
