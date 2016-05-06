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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Syndication;
import org.ambraproject.queue.MessageSender;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.ambraproject.routes.CrossRefLookupRoutes;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.util.DocumentBuilderFactoryCreator;
import org.apache.camel.CamelExecutionException;
import org.apache.commons.configuration.Configuration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class ArticleStateServiceImpl extends AmbraService implements ArticleStateService {

  private static final Logger log = LoggerFactory.getLogger(ArticleStateServiceImpl.class);

  private static final String XML_NAMESPACE = "http://www.ambraproject.org/article/additionalInfo";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  @VisibleForTesting
  public MessageSender messageSender;

  @Autowired
  private Configuration ambraConfiguration;

  @Autowired
  private IngestibleService ingestibleService;

  /**
   * Attaches additional XML info to an article document specifying the journals it is published in.
   * <p/>
   * This is largely copied from org.ambraproject.article.service.ArticleDocumentServiceImpl in the old admin codebase.
   *
   * @param doc       article XML
   * @param articleId encodes DOI
   * @return doc
   */
  private Document appendJournals(Document doc, ArticleIdentity articleId) {
    Article article = (Article) DataAccessUtils.uniqueResult(
        (List<?>) hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(Article.class)
                .setFetchMode("journals", FetchMode.JOIN)
                .add(Restrictions.eq("doi", articleId.getKey()))
        )
    );
    Set<Journal> journals = article.getJournals();

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
   * This is largely copied from org.ambraproject.search.service.IndexingServiceImpl in the old admin codebase.
   *
   * @param doc     article XML
   * @param article
   * @return doc
   */
  private Document appendStrikingImage(Document doc, Article article) {
    String strikingImage = Strings.nullToEmpty(article.getStrkImgURI());
    NodeList metaNodeLst = doc.getElementsByTagName("article-meta");
    Node metaNode = metaNodeLst.item(0);
    Element strkImgElem = doc.createElement("article-strkImg");

    strkImgElem.setTextContent(strikingImage);
    metaNode.appendChild(strkImgElem.cloneNode(true));
    return doc;
  }

  /**
   * Updates the solr index (indirectly, via plos-queue) depending on the publication state of an article.
   *
   * @param articleId   wraps the DOI
   * @param article
   * @param isPublished indicates whether we are publishing or un-publishing
   * @throws IOException
   */
  private void updateSolrIndex(ArticleIdentity articleId, Article article, boolean isPublished)
      throws IOException {
    MessageSender dummyMessageSender = new DummyMessageSender();
    if (isPublished) {
      Document doc;
      try (InputStream xml = articleCrudService.readXml(articleId)) {

        // TODO: is it necessary and/or performant to create a new one of these each
        // time?  The old admin codebase a DocumentBuilderFactory as an instance field
        // and synchronizes access to it.
        DocumentBuilderFactory documentBuilderFactory
            = DocumentBuilderFactoryCreator.createFactory();
        DocumentBuilder documentBuilder;
        try {
          documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
          throw new RuntimeException(pce);
        }
        try {
          doc = documentBuilder.parse(xml);
        } catch (SAXException se) {
          throw new RuntimeException("Bad XML retrieved for " + article.getDoi(), se);
        }
      }
      if (doc.getElementsByTagName("body").getLength() == 0) {
        // AOP XML, so populate create a body element with extracted text from PDF
        if (articleHasPdfRepresentation(article)) {
          // TODO: get PDF representation of article from repo
          dummyMessageSender.sendMessage("pre-insert:", doc);
          log.info("Sent pre-insert dummy message");
          String filePath = "/home/jfesenko/Documents/rhino/em_sample.pdf";
          PDFTextStripper pdfStripper = new PDFTextStripper();
          PDDocument pdDoc = PDDocument.load(new File(filePath));
          Element body = doc.createElementNS(XML_NAMESPACE, "body");
          doc.getDocumentElement().appendChild(body);
          String pdfText = pdfStripper.getText(pdDoc);
          body.appendChild(doc.createTextNode(pdfText));
          dummyMessageSender.sendMessage("post-insert:", doc);
          log.info("Sent post-insert dummy message");
        }
      }
      doc = appendJournals(doc, articleId);
      doc = appendStrikingImage(doc, article);
      messageSender.sendMessage(ambraConfiguration.getString(
          "ambra.services.search.articleIndexingQueue", null), doc);
      log.info("Sent XML doc message to indexing queue");
    } else {
      messageSender.sendMessage(ambraConfiguration.getString("ambra.services.search.articleDeleteQueue", null),
          articleId.getKey());
    }
  }

  // borrowed from rhino test code
  private class DummyMessageSender implements MessageSender {

    public ListMultimap<String, String> messagesSent = LinkedListMultimap.create();
    public Map<String, Object> headersSent;

    @Override
    public void sendMessage(String destination, String body) {
      messagesSent.put(destination, body);
    }

    @Override
    public void sendMessage(String destination, Document body) {
      try {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(body);
        transformer.transform(source, result);
        messagesSent.put(destination, writer.toString());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void sendMessage(String destination, Object body, Map<String, Object> headers) {
      messagesSent.put(destination, (String)body);
      headersSent = headers;
    }
  }
  /**
   * Iterate through an article's assets to determine if the article has a PDF representation The PDF asset DOI is equal
   * to the article DOI, and the extension is "PDF".
   *
   * @param article the article to analyze
   * @return boolean indicating existence of article PDF representation
   */
  private static boolean articleHasPdfRepresentation(Article article) {
    for (ArticleAsset articleAsset : article.getAssets()) {
      if (articleAsset.getDoi().equals(article.getDoi())
          && articleAsset.getExtension().equals("PDF")) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article update(ArticleIdentity articleId, ArticleInputView input)
      throws IOException {
    Article article = loadArticle(articleId);

    Optional<Integer> updatedState = input.getPublicationState();
    if (updatedState.isPresent()) {
      if (updatedState.get() == Article.STATE_ACTIVE && article.getState() == Article.STATE_DISABLED) {
        throw new RestClientException("A disabled article cannot be published; it must first be re-ingested",
            HttpStatus.METHOD_NOT_ALLOWED);
      } else {
        article.setState(updatedState.get());
      }

      boolean isPublished = (article.getState() == Article.STATE_ACTIVE);
      updateSolrIndex(articleId, article, isPublished);
      hibernateTemplate.update(article);

      if (updatedState.get() == Article.STATE_ACTIVE) {
        queueCrossRefRefresh(article.getDoi());
      }

      if (updatedState.get() == Article.STATE_DISABLED) {
        for (ArticleAsset asset : article.getAssets()) {
          deleteAssetFile(AssetFileIdentity.from(asset));
        }
        ingestibleService.revertArchive(articleId);
      }
    }

    for (ArticleInputView.SyndicationUpdate update : input.getSyndicationUpdates()) {

      // TODO: should we always re-attempt the syndication, as we do here, if it's
      // IN_PROGRESS?  Or base it on the Syndication.status of the appropriate target?
      // Not sure yet.
      if (update.getStatus().equals(Syndication.STATUS_IN_PROGRESS)) {
        try {
          syndicationService.syndicate(article.getDoi(), update.getTarget());
        } catch (NoSuchArticleIdException nsaide) {

          // Should never happen since we just loaded the article.
          throw new RuntimeException(nsaide);
        }
      }

      // TODO: un-syndicate, if necessary.
    }

    return article;
  }

  /**
   * Send a message to the queue to refresh the cited articles via cross ref
   */
  @SuppressWarnings("unchecked")
  private void queueCrossRefRefresh(String doi) {
    String refreshCitedArticlesQueue = ambraConfiguration.getString("ambra.services.queue.refreshCitedArticles", null);
    if (refreshCitedArticlesQueue != null) {
      try {
        messageSender.sendMessage(refreshCitedArticlesQueue, doi, new HashMap() {{
          //Appending null for the header auth ID.  It's OK because the article must be in a published
          //state and the authID should not be checked
          put(CrossRefLookupRoutes.HEADER_AUTH_ID, null);
        }});
      } catch (CamelExecutionException ex) {
        log.error(ex.getMessage(), ex);
        throw new RuntimeException("Failed to queue job for refreshing article references, is the queue running?");
      }
    } else {
      throw new RuntimeException("Refresh cited articles queue not defined. No route created.");
    }
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
