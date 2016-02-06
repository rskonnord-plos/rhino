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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.DoiAssociation;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.ambraproject.rhino.util.response.EntityCollectionTransceiver;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleVisibility;
import org.ambraproject.rhino.view.asset.groomed.GroomedImageView;
import org.ambraproject.rhino.view.asset.groomed.UncategorizedAssetException;
import org.ambraproject.rhino.view.asset.raw.RawAssetFileCollectionView;
import org.ambraproject.rhino.view.asset.raw.RawAssetFileView;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class AssetCrudServiceImpl extends AmbraService implements AssetCrudService {

  private static final Logger log = LoggerFactory.getLogger(AssetCrudServiceImpl.class);

  /**
   * Copy all fields defined by article XML into a new asset and return it.
   *
   * @param oldAsset the asset to copy from
   * @return the new asset
   */
  private static ArticleAsset copyArticleFields(ArticleAsset oldAsset) {
    Preconditions.checkNotNull(oldAsset);
    ArticleAsset newAsset = new ArticleAsset();
    newAsset.setDoi(oldAsset.getDoi());
    newAsset.setTitle(oldAsset.getTitle());
    newAsset.setDescription(oldAsset.getDescription());
    newAsset.setContextElement(oldAsset.getContextElement());
    return newAsset;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(AssetFileIdentity assetId) {
    try {
      return contentRepoService.getLatestRepoObject(assetId.toString());
    } catch (ContentRepoException | NotFoundException e) {
      String message = String.format("Asset not found at DOI \"%s\" with extension \"%s\"",
          assetId.getIdentifier(), assetId.getFileExtension());
      throw new RestClientException(message, HttpStatus.NOT_FOUND, e);
    }
  }

  /**
   * Find article asset objects.
   *
   * @param id the asset ID
   * @return a collection of all {@code ArticleAsset} objects whose DOI matches the ID
   * @throws RestClientException if no asset files exist with that ID
   */
  private abstract class ArticleAssetsRetriever extends EntityCollectionTransceiver<ArticleAsset> {
    private final AssetIdentity id;

    protected ArticleAssetsRetriever(AssetIdentity id) {
      this.id = Preconditions.checkNotNull(id);
    }

    @Override
    protected final Calendar getLastModifiedDate() throws IOException {
      Date lastModified = (Date) DataAccessUtils.uniqueResult(hibernateTemplate.find(
          "select max(lastModified) from ArticleAsset where doi = ?", id.getKey()));
      return (lastModified == null) ? null : copyToCalendar(lastModified);
    }

    @Override
    protected final Collection<? extends ArticleAsset> fetchEntities() {
      @SuppressWarnings("unchecked") List<ArticleAsset> assets = ((List<ArticleAsset>)
          hibernateTemplate.findByCriteria(DetachedCriteria
                  .forClass(ArticleAsset.class)
                  .add(Restrictions.eq("doi", id.getKey()))
                  .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
          ));
      if (assets.isEmpty()) {
        throw reportNotFound(id);
      }
      return assets;
    }
  }

  @Override
  public Transceiver readMetadata(final AssetIdentity id)
      throws IOException {
    return new ArticleAssetsRetriever(id) {
      @Override
      protected Object getView(Collection<? extends ArticleAsset> assets) {
        ArticleVisibility parentArticle = findArticleFor(id);
        return new RawAssetFileCollectionView(assets, parentArticle);
      }
    };
  }

  @Override
  public Transceiver readFigureMetadata(final AssetIdentity id)
      throws IOException {
    return new ArticleAssetsRetriever(id) {
      @Override
      protected Object getView(Collection<? extends ArticleAsset> assets) {
        GroomedImageView figureView;
        try {
          figureView = GroomedImageView.create(assets);
        } catch (UncategorizedAssetException e) {
          String message = "Not a figure asset: " + id.getIdentifier();
          throw new RestClientException(message, HttpStatus.BAD_REQUEST, e);
        }
        figureView.setParentArticle(findArticleFor(figureView.getIdentity()));
        return figureView;
      }
    };
  }

  @Override
  public Transceiver readFileMetadata(final AssetFileIdentity id)
      throws IOException {
    return new EntityTransceiver<ArticleAsset>() {
      @Override
      protected ArticleAsset fetchEntity() {
        @SuppressWarnings("unchecked") List<ArticleAsset> assets = ((List<ArticleAsset>)
            hibernateTemplate.findByCriteria(DetachedCriteria
                    .forClass(ArticleAsset.class)
                    .add(Restrictions.eq("doi", id.getKey()))
                    .add(Restrictions.eq("extension", id.getFileExtension()))
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            ));
        if (assets.isEmpty()) {
          throw reportNotFound(id);
        }
        return DataAccessUtils.requiredUniqueResult(assets);
      }

      @Override
      protected Object getView(ArticleAsset asset) {
        ArticleVisibility parentArticle = findArticleFor(id.forAsset());
        return new RawAssetFileView(asset, parentArticle);
      }
    };
  }

  private ArticleVisibility findArticleFor(AssetIdentity id) {
    Object[] articleResult = (Object[]) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.find(
            "select distinct a.doi, a.state "
                + "from Article a join a.assets b "
                + "where b.doi = ?",
            id.getKey()
        ));
    String articleDoi = (String) articleResult[0];
    Integer articleState = (Integer) articleResult[1];
    if (articleDoi == null) {
      throw new RestClientException("Asset not found for: " + id.getIdentifier(), HttpStatus.NOT_FOUND);
    }

    List<Journal> journalResult = (List<Journal>) hibernateTemplate.find(
        "select journals from Article where doi = ?", articleDoi);

    return new ArticleVisibility(articleDoi, articleState, journalResult);
  }

  @Override
  public ArticleIdentity getParentArticle(DoiBasedIdentity identity) {
    String parentArticleDoi = (String) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(DoiAssociation.class)
            .setProjection(Projections.property("parentArticleDoi"))
            .add(Restrictions.eq("doi", identity.getIdentifier()))));
    return (parentArticleDoi == null) ? null : ArticleIdentity.create(parentArticleDoi);
  }

  @Override
  public RepoObjectMetadata getAssetObject(ArticleIdentity parentArticleId,
                                           AssetIdentity assetId,
                                           Optional<Integer> versionNumber,
                                           String fileType) {
    RepoCollectionMetadata articleCollection;
    if (versionNumber.isPresent()) {
      articleCollection = contentRepoService.getCollection(new RepoVersionNumber(
          parentArticleId.getIdentifier(), versionNumber.get()));
    } else {
      articleCollection = contentRepoService.getLatestCollection(parentArticleId.getIdentifier());
    }

    AssetTable<RepoVersion> assetTable = AssetTable.buildFromAssetMetadata(articleCollection);
    RepoVersion fileVersion = assetTable.lookup(assetId, fileType);
    return contentRepoService.getRepoObjectMetadata(fileVersion);
  }

}
