package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.rest.RestClientException;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Criteria from an API client describing a subset of articles.
 */
public class ArticleCriteria {

  private final Optional<ImmutableSet<Integer>> publicationStates;
  private final Optional<ImmutableSet<String>> syndicationStatuses;
  private final boolean includeLastModifiedDate;

  private ArticleCriteria(Optional<ImmutableSet<Integer>> publicationStates,
                          Optional<ImmutableSet<String>> syndicationStatuses, boolean includeLastModifiedDate) {
    this.publicationStates = Objects.requireNonNull(publicationStates);
    this.syndicationStatuses = Objects.requireNonNull(syndicationStatuses);
    this.includeLastModifiedDate = includeLastModifiedDate;
  }

  /**
   * Create an object describing a set of articles and how to display them.
   *
   * @param clientPubStates         include all articles whose publication state is one of these; {@code null} to
   *                                include all articles regardless of publication state
   * @param clientSyndStatuses      include all articles whose publication state is one of these; {@code null} to
   *                                include all articles regardless of publication state
   * @param includeLastModifiedDate display lastModifiedDate with each article
   * @return
   */
  public static ArticleCriteria create(Collection<String> clientPubStates,
                                       Collection<String> clientSyndStatuses,
                                       boolean includeLastModifiedDate) {
    Optional<ImmutableSet<Integer>> publicationStateConstants;
    if (CollectionUtils.isEmpty(clientPubStates)) {
      publicationStateConstants = Optional.empty();
    } else {
      ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
      for (String clientPubState : clientPubStates) {
        Integer pubStateConstant = ArticleJsonConstants.getPublicationStateConstant(clientPubState);
        if (pubStateConstant == null) {
          throw unrecognizedInputs("publication state", clientPubStates, ArticleJsonConstants.PUBLICATION_STATE_NAMES);
        }
        builder.add(pubStateConstant);
      }
      publicationStateConstants = Optional.of(builder.build());
    }

    Optional<ImmutableSet<String>> syndicationStatusConstants;
    if (CollectionUtils.isEmpty(clientSyndStatuses)) {
      syndicationStatusConstants = Optional.empty();
    } else {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (String clientSyndStatus : clientSyndStatuses) {
        clientSyndStatus = clientSyndStatus.toUpperCase();
        if (!ArticleJsonConstants.SYNDICATION_STATUSES.contains(clientSyndStatus)) {
          throw unrecognizedInputs("syndication status", clientSyndStatuses, ArticleJsonConstants.SYNDICATION_STATUSES);
        }
        builder.add(clientSyndStatus);
      }
      syndicationStatusConstants = Optional.of(builder.build());
    }

    return new ArticleCriteria(publicationStateConstants, syndicationStatusConstants, includeLastModifiedDate);
  }

  /*
   * Put somewhere for reuse?
   */
  private static RestClientException unrecognizedInputs(String valueDescription,
                                                        Collection<?> inputValues,
                                                        Set<?> expectedValues) {
    Objects.requireNonNull(valueDescription);
    Preconditions.checkArgument(!expectedValues.isEmpty());

    Set<?> inputValueSet = (inputValues instanceof Set) ? (Set<?>) inputValues
        : ImmutableSet.copyOf(inputValues);
    Set<?> unrecognizedValues = Sets.difference(inputValueSet, expectedValues);

    String message = String.format("Unrecognized values for %s: %s. Expected: %s.",
        valueDescription, unrecognizedValues, expectedValues);
    return new RestClientException(message, HttpStatus.BAD_REQUEST);
  }


  /**
   * Fetch a list of DOIs of articles in the system that match this object's criteria.
   *
   * @param hibernateTemplate the system's Hibernate template
   * @return a list of article DOIs
   */
  public Object apply(HibernateTemplate hibernateTemplate) {
    Objects.requireNonNull(hibernateTemplate);
    if (syndicationStatuses.isPresent()) {
      return findBySyndication(hibernateTemplate);
    }

    ProjectionList projectionList = Projections.projectionList().add(Projections.property("doi"));
    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setProjection(projectionList)
        .addOrder(Order.asc("lastModified"));
    if (publicationStates.isPresent()) {
      criteria = criteria.add(Restrictions.in("state", publicationStates.get()));
      projectionList.add(Projections.property("state"));
      return new ArticleViewList(Lists.transform((List<Object[]>) hibernateTemplate.findByCriteria(criteria),
          ArticleCriteria::doiAndStateAsView));
    }
    if (includeLastModifiedDate) {
      projectionList.add(Projections.property("lastModified"));
      return new ArticleViewList(Lists.transform((List<Object[]>) hibernateTemplate.findByCriteria(criteria),
          ArticleCriteria::doiAndTimestampAsView));
    }
    return new DoiList((List<String>) hibernateTemplate.findByCriteria(criteria));
  }

  private static ArticleView doiAndStateAsView(Object[] input) {
    String doi = (String) input[0];
    Integer pubStateConstant = (Integer) input[1];
    String pubStateName = ArticleJsonConstants.getPublicationStateName(pubStateConstant);
    return new ArticleStateView(doi, pubStateName, null);
  }

  private static ArticleView doiAndTimestampAsView(@Nullable Object[] input) {
    String doi = (String) input[0];
    Date lastModified = (Date) input[1];
    return new TimestampedDoi(doi, lastModified);
  }

  private static class TimestampedDoi implements ArticleView {
    private final String doi;
    private final Date lastModified;

    private TimestampedDoi(String doi, Date lastModified) {
      this.doi = Objects.requireNonNull(doi);
      this.lastModified = Objects.requireNonNull(lastModified);
    }

    @Override
    public String getDoi() {
      return doi;
    }
  }


  // Optimization parameter; doesn't matter if it's off. Main use case is "CROSSREF" and "PMC".
  private static final int EXPECTED_SYNDICATION_TARGETS = 2;

  /*
   * Special-case hack requiring weird logic.
   */
  private ArticleViewList findBySyndication(HibernateTemplate hibernateTemplate) {
    List<Object[]> results = hibernateTemplate.execute(session -> {
      Query query = session.createQuery(SYND_QUERY);
      query.setParameterList("syndStatuses", syndicationStatuses.get());
      query.setParameterList("pubStates", publicationStates.orElse(ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS));
      return query.list();
    });

    List<ArticleStateView> views = Lists.newArrayListWithExpectedSize(results.size() / EXPECTED_SYNDICATION_TARGETS);
    ArticleStateViewBuilder builder = null;
    for (Object[] result : results) {
      String doi = (String) result[0];
      Integer pubStateConstant = (Integer) result[1];
      String pubStateName = ArticleJsonConstants.getPublicationStateName(pubStateConstant);
      Syndication syndication = (Syndication) result[2];

      if (builder == null || !doi.equals(builder.doi)) {
        if (builder != null) {
          views.add(builder.build());
        }
        builder = new ArticleStateViewBuilder(doi, pubStateName);
      }
      builder.syndications.add(syndication);
    }
    if (builder != null) {
      views.add(builder.build());
    }

    return new ArticleViewList(views);
  }

  private static final String SYND_QUERY = ""
      + "select a.doi, a.state, s from Article a, Syndication s "
      + "where (a.doi = s.doi) and (s.status in (:syndStatuses)) and (a.state in (:pubStates)) "
      + "order by a.lastModified asc, a.doi asc";

  private static class ArticleStateViewBuilder {
    private final String doi;
    private final String state;
    private final List<Syndication> syndications;

    public ArticleStateViewBuilder(String doi, String state) {
      this.doi = Objects.requireNonNull(doi);
      this.state = Objects.requireNonNull(state);
      this.syndications = Lists.newArrayListWithExpectedSize(EXPECTED_SYNDICATION_TARGETS);
    }

    public ArticleStateView build() {
      return new ArticleStateView(doi, state, syndications);
    }
  }

}
