/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.io.Closeables;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Syndication;
import org.ambraproject.queue.MessageSender;
import org.ambraproject.rhino.content.ArticleState;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.util.DocumentBuilderFactoryCreator;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class ArticleStateServiceImpl extends AmbraService implements ArticleStateService {

  private static final String XML_NAMESPACE = "http://www.ambraproject.org/article/additionalInfo";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private MessageSender messageSender;

  @Autowired
  private Configuration ambraConfiguration;

  private DocumentBuilderFactory documentBuilderFactory
      = DocumentBuilderFactoryCreator.createFactory();

  /**
   * Helper method to set the syndication state for the appropriate target based on
   * the status property of the Syndication object.
   *
   * @param state ArticleState object that will be modified
   * @param syndication Syndication we are reading from
   */
  private void setSyndicationState(ArticleState state, Syndication syndication) {
    ArticleState.SyndicationTarget target
        = ArticleState.SyndicationTarget.valueOf(syndication.getTarget());
    ArticleState.SyndicationState syndicationState
        = ArticleState.SyndicationState.valueOf(syndication.getStatus());
    state.setSyndicationState(target, syndicationState);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void read(HttpServletResponse response, ArticleIdentity articleId, MetadataFormat format)
      throws IOException {
    Article article = loadArticle(articleId);
    ArticleState state = new ArticleState();
    state.setPublished(article.getState() == 0);
    List<Syndication> syndications;
    try {
      syndications = syndicationService.getSyndications(article.getDoi());
    } catch (NoSuchArticleIdException nsaide) {

      // Should never happen since we just loaded the article.
      throw new RuntimeException(nsaide);
    }
    for (Syndication syndication : syndications) {
      setSyndicationState(state, syndication);
    }

    assert format == MetadataFormat.JSON;
    writeJsonToResponse(response, state);
  }

  /**
   * Attaches additional XML info to an article document specifying the journals it
   * is published in.
   * <p/>
   * This is largely copied from org.ambraproject.article.service.ArticleDocumentServiceImpl
   * in the old admin codebase.
   *
   * @param doc article XML
   * @param articleId encodes DOI
   * @return doc
   */
  private Document appendJournals(Document doc, ArticleIdentity articleId) {
    List<Article> articles = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .setFetchMode("journals", FetchMode.JOIN)
            .add(Restrictions.eq("doi", articleId.getKey()))
    );
    if (articles.size() != 1) {
      throw new IllegalStateException("Invalid DOI " + articleId.getIdentifier());
    }
    Set<Journal> journals = articles.get(0).getJournals();

    Element additionalInfoElement = doc.createElementNS(XML_NAMESPACE, "ambra");
    Element journalsElement = doc.createElementNS(XML_NAMESPACE, "journals");
    doc.getDocumentElement().appendChild(additionalInfoElement);
    additionalInfoElement.appendChild(journalsElement);

    for (Journal journal : journals) {
      Element journalElement = doc.createElementNS(XML_NAMESPACE, "journal");
      Element eIssn = doc.createElementNS(XML_NAMESPACE, "eIssn");
      eIssn.appendChild(doc.createTextNode(journal.geteIssn()));
      journalElement.appendChild(eIssn);
      Element key = doc.createElementNS(XML_NAMESPACE, "key");
      key.appendChild(doc.createTextNode(journal.getJournalKey()));
      journalElement.appendChild(key);
      Element name = doc.createElementNS(XML_NAMESPACE, "name");
      name.appendChild(doc.createTextNode(journal.getTitle()));
      journalElement.appendChild(name);
      journalsElement.appendChild(journalElement);
    }
    return doc;
  }

  /**
   * Append an additional XML node to an article document specifying the striking image.
   * <p/>
   * This is largely copied from org.ambraproject.search.service.IndexingServiceImpl
   * in the old admin codebase.
   *
   * @param doc article XML
   * @param article
   * @return doc
   */
  private Document appendStrikingImage(Document doc, Article article) {
    String strikingImage = article.getStrkImgURI() == null ? "" : article.getStrkImgURI();
    NodeList metaNodeLst = doc.getElementsByTagName("article-meta");
    Node metaNode = metaNodeLst.item(0);
    Element strkImgElem = doc.createElement("article-strkImg");

    strkImgElem.setTextContent(strikingImage);
    metaNode.appendChild(strkImgElem.cloneNode(true));
    return doc;
  }

  /**
   * Updates the solr index (indirectly, via plos-queue) depending on the publication
   * state of an article.
   *
   * @param articleId wraps the DOI
   * @param article
   * @param isPublished indicates whether we are publishing or un-publishing
   * @throws FileStoreException
   * @throws IOException
   */
  private void updateSolrIndex(ArticleIdentity articleId, Article article, boolean isPublished)
      throws FileStoreException, IOException {
    if (isPublished) {
      InputStream xml = null;
      boolean threw = true;
      Document doc;
      try {
        xml = articleCrudService.readXml(articleId);
        DocumentBuilder documentBuilder;

        // TODO: is this synchronization really necessary?  Just copying and pasting from
        // the old admin code.  Nothing in the javadocs indicates it is.
        synchronized (documentBuilderFactory) {
          try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
          } catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
          }
        }
        try {
          doc = documentBuilder.parse(xml);
        } catch (SAXException se) {
          throw new RuntimeException("Bad XML retrieved from filestore for " + article.getDoi(), se);
        }
        threw = false;
      } finally {
        Closeables.close(xml, threw);
      }
      doc = appendJournals(doc, articleId);
      doc = appendStrikingImage(doc, article);
      messageSender.sendMessage(ambraConfiguration.getString(
          "ambra.services.search.articleIndexingQueue", null), doc);
    } else {

      // TODO: remove from solr index upon unpublication
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(ArticleIdentity articleId, ArticleState state)
      throws FileStoreException, IOException {
    Article article = loadArticle(articleId);
    article.setState(state.isPublished() ? Article.STATE_ACTIVE : Article.STATE_UNPUBLISHED);
    for (ArticleState.SyndicationTarget target : ArticleState.SyndicationTarget.values()) {

      // TODO: should we always re-attempt the syndication, as we do here, if it's
      // IN_PROGRESS?  Or base it on the Syndication.status of the appropriate target?
      // Not sure yet.
      if (state.getSyndicationState(target) == ArticleState.SyndicationState.IN_PROGRESS) {
        try {
          syndicationService.syndicate(article.getDoi(), target.toString());
        } catch (NoSuchArticleIdException nsaide) {

          // Should never happen since we just loaded the article.
          throw new RuntimeException(nsaide);
        }
      }

      // TODO: un-syndicate, if necessary.
    }
    updateSolrIndex(articleId, article, state.isPublished());
    hibernateTemplate.update(article);
  }

  private Article loadArticle(ArticleIdentity articleId) {
    Article result = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", articleId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (result == null) {
      throw new RestClientException("Article not found: " + articleId.getIdentifier(),
          HttpStatus.NOT_FOUND);
    }
    return result;
  }
}
