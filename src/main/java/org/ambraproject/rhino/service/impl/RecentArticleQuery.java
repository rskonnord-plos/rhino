package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.RecentArticleView;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Encapsulates a query that will list the DOIs, titles, and publication dates of all articles published after a certain
 * threshold. If a minimum result count is provided, go past the threshold to return that many if necessary.
 * <p/>
 * If a list of article types is provided, return all articles of those types published after the threshold. Articles
 * with the same publication date are ordered by type, in the same order as the list.
 * <p/>
 * The string {@code "*"} may be used as a stand-in that matches all article types. For example, place it at the end of
 * the list to get all articles past the threshold if there aren't enough of the preceding types. If {@code minimum} is
 * present and {@code articleTypes} contains more than 1 element, then {@code articleTypes} <em>must</em> contain {@code
 * "*"}.
 *
 * @see org.ambraproject.rhino.service.ArticleCrudService#listRecent
 */
public class RecentArticleQuery {

  private final String journalKey;
  private final Calendar threshold;
  private final Optional<Integer> minimum;
  private final ImmutableList<String> articleTypes;
  private final ImmutableList<String> excludedArticleTypes;

  private RecentArticleQuery(Builder builder) {
    this.journalKey = Preconditions.checkNotNull(builder.journalKey);
    this.threshold = Preconditions.checkNotNull(builder.threshold);
    this.minimum = Optional.fromNullable(builder.minimum);
    this.articleTypes = (builder.articleTypes == null) ? ImmutableList.<String>of()
        : ImmutableList.copyOf(builder.articleTypes);
    this.excludedArticleTypes = (builder.excludedArticleTypes == null) ? ImmutableList.<String>of()
        : ImmutableList.copyOf(builder.excludedArticleTypes);
  }


  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {
    }

    private String journalKey;
    private Calendar threshold;
    private Integer minimum;
    private List<String> articleTypes;
    private List<String> excludedArticleTypes;

    /**
     * @param journalKey key of the journal to search
     */
    public Builder setJournalKey(String journalKey) {
      this.journalKey = journalKey;
      return this;
    }

    /**
     * @param threshold return all articles published after this date
     */
    public Builder setThreshold(Calendar threshold) {
      this.threshold = threshold;
      return this;
    }

    /**
     * @param minimum minimum result count
     */
    public Builder setMinimum(Integer minimum) {
      this.minimum = minimum;
      return this;
    }

    /**
     * @param articleTypes the list of article types to filter, in order of preference; may be {@code null}
     */
    public Builder setArticleTypes(List<String> articleTypes) {
      this.articleTypes = articleTypes;
      return this;
    }

    /**
     * @param excludedArticleTypes the list of article types to exclude from a wildcard type ({@code "*"})
     */
    public Builder setExcludedArticleTypes(List<String> excludedArticleTypes) {
      this.excludedArticleTypes = excludedArticleTypes;
      return this;
    }

    public RecentArticleQuery build() {
      return new RecentArticleQuery(this);
    }
  }


  private static final String ARTICLE_TYPE_WILDCARD = "*";


  public Transceiver execute(HibernateTemplate hibernateTemplate) {
    return new Result(hibernateTemplate);
  }

  private class Result extends Transceiver {
    private final HibernateTemplate hibernateTemplate;

    private Result(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = Preconditions.checkNotNull(hibernateTemplate);
    }

    /**
     * Execute a query.
     *
     * @param forceMinimum if false, use {@code threshold}; if true, use {@code minimum} ({@code minimum} <em>must</em>
     *                     be present if {@code forceMinimum} is true)
     * @param articleType  if absent, return results of all article types; if present, filter results for that type
     */
    private List<Object[]> query(final boolean forceMinimum, final Optional<String> articleType) {
      return hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
        @Override
        public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
          StringBuilder hql = new StringBuilder(211)
              .append("select distinct a.doi, a.title, a.date ")
              .append("from Article a, Journal j ")
              .append("where j in elements(a.journals) and j.journalKey = :journalKey");
          if (!forceMinimum) {
            hql.append(" and a.date >= :threshold");
          }
          if (articleType.isPresent()) {
            hql.append(" and :articleType in elements(a.types)");
          }
          for (int i = 0; i < excludedArticleTypes.size(); i++) {
            // No type in excludedArticleTypes should be in a.types. Until we can figure out a better way to express
            // this in HQL, just kludge in a variable number of clauses. TODO: Improve?
            hql.append(" and not (:exclude").append(i) // variable names are exclude0, exclude1, etc.
                .append(" in elements(a.types))");
          }
          hql.append(" order by a.date desc");

          Query query = session.createQuery(hql.toString());
          query.setString("journalKey", journalKey);
          if (forceMinimum) {
            query.setMaxResults(minimum.get());
          } else {
            query.setDate("threshold", threshold.getTime());
          }
          if (articleType.isPresent()) {
            query.setString("articleType", articleType.get());
          }
          for (ListIterator<String> iter = excludedArticleTypes.listIterator(); iter.hasNext(); ) {
            String paramName = "exclude" + iter.nextIndex();
            query.setParameter(paramName, iter.next());
          }

          return query.list();
        }
      });
    }

    @Override
    protected List<RecentArticleView> getData() throws IOException {
      List<Object[]> results;

      // Get all articles more recent than the threshold
      if (articleTypes.isEmpty()) {
        results = query(false, Optional.<String>absent());
      } else {
        results = new ArrayList<>();
        Set<String> uniqueDois = new HashSet<>();

        // Query for each article type separately and concatenate the results,
        // in order to preserve the "preference order" in the articleTypes list.
        for (String articleType : articleTypes) {
          Optional<String> articleTypeArg = articleType.equals(ARTICLE_TYPE_WILDCARD)
              ? Optional.<String>absent() : Optional.of(articleType);
          List<Object[]> queryResults = query(false, articleTypeArg);

          // Add each query result to 'results' only if the DOI is not already in 'uniqueDois'
          for (Object[] queryResult : queryResults) {
            if (uniqueDois.add((String) queryResult[0])) {
              results.add(queryResult);
            }
          }
        }
      }

      if (minimum.isPresent() && results.size() < minimum.get()) {
        // Not enough results. Get enough past the threshold to meet the minimum.
        // Ignore order of articleTypes and get the union of all.
        if (articleTypes.isEmpty() || articleTypes.contains(ARTICLE_TYPE_WILDCARD)) {
          results = query(true, Optional.<String>absent());
        } else if (articleTypes.size() == 1) {
          results = query(true, Optional.of(articleTypes.get(0)));
        } else {
          String message = "" +
              "Service does not support queries for a minimum number of recent articles " +
              "filtered by multiple article types. " +
              "To make a valid query, client must either " +
              "(1) omit the 'min' parameter, " +
              "(2) use no more than one 'type' parameter, or " +
              "(3) include the wildcard type parameter ('type=*').";
          throw new RestClientException(message, HttpStatus.BAD_REQUEST);
        }
      }

      // Transform into results view.
      List<RecentArticleView> views = transformResults(results);

      // Sort them by date, relying on stable sort to keep articles with the same date ordered by type
      Collections.sort(views, BY_DATE_DESCENDING);

      return views;
    }

    @Override
    protected Calendar getLastModifiedDate() throws IOException {
      return null;
    }
  }

  /**
   * Represent raw query results from {@link org.ambraproject.rhino.service.impl.RecentArticleQuery.Result#getData()} as
   * {@code RecentArticleView} objects. The returned list is mutable.
   */
  private static List<RecentArticleView> transformResults(List<Object[]> results) {
    List<RecentArticleView> views = Lists.newArrayListWithCapacity(results.size());
    for (Object[] result : results) {
      Preconditions.checkArgument(result.length == 3);
      String doi = (String) result[0];
      String title = (String) result[1];
      Date date = (Date) result[2];
      ArticleIdentity article = ArticleIdentity.create(doi);
      views.add(new RecentArticleView(article, title, date));
    }
    return views;
  }

  /**
   * Order by date. Maintain stable order for objects with the same date. Not consistent with {@link
   * RecentArticleView#equals}.
   */
  private static final Comparator<RecentArticleView> BY_DATE_DESCENDING = new Comparator<RecentArticleView>() {
    @Override
    public int compare(RecentArticleView o1, RecentArticleView o2) {
      return -o1.getDate().compareTo(o2.getDate());
    }
  };

}
