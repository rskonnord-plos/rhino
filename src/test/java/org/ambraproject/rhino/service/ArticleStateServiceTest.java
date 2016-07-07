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

package org.ambraproject.rhino.service;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.PublicationState;
import org.ambraproject.rhino.model.SyndicationStatus;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.impl.ArticleStateServiceImpl;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputViewFactory;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.service.ContentRepoService;
import org.plos.crepo.service.InMemoryContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests for {@link org.ambraproject.rhino.service.impl.ArticleStateServiceImpl}
 */
public class ArticleStateServiceTest extends BaseRhinoTest {

  private static final String TEST_DATA_DIR = "src/test/resources/articles/";

  @Autowired
  private ArticleStateService articleStateService;

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private ContentRepoService contentRepoService;

  @Autowired
  private ArticleOutputViewFactory articleOutputViewFactory;

  @Autowired
  private Gson entityGson;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @Test
  public void testServiceAutowiring() {
    assertNotNull(articleStateService);
  }

  @BeforeMethod
  public void addJournal() {
    final ImmutableSet<String> testCaseEissns = ImmutableSet.of("1932-6203");

    for (String eissn : testCaseEissns) {
      List<?> existing = hibernateTemplate.findByCriteria(DetachedCriteria
          .forClass(Journal.class)
          .add(Restrictions.eq("eIssn", eissn)));
      if (!existing.isEmpty())
        continue;
      Journal journal = RhinoTestHelper.createDummyJournal(eissn);
      hibernateTemplate.save(journal);
    }
  }

  private void checkFileExistence(AssetFileIdentity fileIdentity, boolean expectedToExist) throws IOException {
    RepoId repoId = RepoId.create(runtimeConfiguration.getCorpusStorage().getDefaultBucket(), fileIdentity.toString());
    try (InputStream stream = contentRepoService.getLatestRepoObject(repoId)) {
      assertNotNull(stream);
      assertTrue(expectedToExist);
    } catch (InMemoryContentRepoService.InMemoryContentRepoServiceException|NotFoundException nfe) {
      assertFalse(expectedToExist);
    } catch (ContentRepoException e) {
      throw e;
    }
  }

  @Test(enabled = false)
  public void testPublication() throws Exception {
    final String crossref = "CROSSREF";
    final String pmc = "PMC";
    final String pubmed = "PUBMED";

    Archive archive = Archive.readZipFileIntoMemory(new File(TEST_DATA_DIR + "pone.0056489.zip"));
//    Article article = articleCrudService.writeArchive(archive,
//        Optional.empty(), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
    Article article = new Article();
    ArticleIdentity articleId = ArticleIdentity.create(article);
    assertEquals(article.getState(), PublicationState.INGESTED.getValue());
    for (ArticleAsset asset : article.getAssets()) {
      checkFileExistence(AssetFileIdentity.from(asset), true);
    }

    ArticleOutputView outputView = articleOutputViewFactory.create(article, false);
    assertEquals(outputView.getArticle().getState(), PublicationState.INGESTED.getValue());
    assertEquals(outputView.getSyndication(crossref).getStatus(), SyndicationStatus.PENDING.getLabel());
    assertEquals(outputView.getSyndication(pmc).getStatus(), SyndicationStatus.PENDING.getLabel());
    assertEquals(outputView.getSyndication(pubmed).getStatus(), SyndicationStatus.PENDING.getLabel());

    String inputJson = ""
        + "{"
        + "  \"state\": \"published\","
        + "  \"syndications\": {"
        + "    \"CROSSREF\": {"
        + "      \"status\": \"IN_PROGRESS\""
        + "    },"
        + "    \"PMC\": {"
        + "      \"status\": \"IN_PROGRESS\""
        + "    },"
        + "    \"PUBMED\": {"
        + "      \"status\": \"IN_PROGRESS\""
        + "    }"
        + "  }"
        + "}";
    ArticleInputView inputView = entityGson.fromJson(inputJson, ArticleInputView.class);
    assertEquals(inputView.getPublicationState().get().intValue(), PublicationState.PUBLISHED.getValue());
    assertEquals(inputView.getSyndicationUpdate(crossref).getStatus(), SyndicationStatus.IN_PROGRESS.getLabel());
    assertEquals(inputView.getSyndicationUpdate(pmc).getStatus(), SyndicationStatus.IN_PROGRESS.getLabel());
    assertEquals(inputView.getSyndicationUpdate(pubmed).getStatus(), SyndicationStatus.IN_PROGRESS.getLabel());
    article = articleStateService.update(articleId, inputView);

    ArticleOutputView result = articleOutputViewFactory.create(article, false);
    assertEquals(result.getArticle().getState(), PublicationState.PUBLISHED.getValue());
    assertEquals(result.getSyndication(crossref).getStatus(), SyndicationStatus.IN_PROGRESS.getLabel());
    assertEquals(result.getSyndication(pmc).getStatus(), SyndicationStatus.IN_PROGRESS.getLabel());
    assertEquals(result.getSyndication(pubmed).getStatus(), SyndicationStatus.IN_PROGRESS.getLabel());
    ArticleStateServiceImpl impl = (ArticleStateServiceImpl) articleStateService;
    DummyMessageSender dummySender = (DummyMessageSender) impl.messageSender;
    assertEquals(dummySender.messagesSent.size(), 5);

    List<String> solrMessages = dummySender.messagesSent.get("activemq:fake.indexing.queue");
    assertEquals(solrMessages.size(), 1);
    XMLUnit.compareXML(IOUtils.toString(new FileInputStream(
        TEST_DATA_DIR + "pone.0056489_solr_decorated.xml")), solrMessages.get(0));

    String expectedSyndication
        = "<ambraMessage><doi>info:doi/10.1371/journal.pone.0056489</doi><archive>pone.0056489.zip</archive></ambraMessage>";
    List<String> crossRefMessages = dummySender.messagesSent.get("activemq:fake.crossref.queue");
    assertEquals(crossRefMessages.size(), 1);
    XMLUnit.compareXML(expectedSyndication, crossRefMessages.get(0));

    List<String> pmcMessages = dummySender.messagesSent.get("activemq:fake.pmc.queue");
    assertEquals(pmcMessages.size(), 1);
    XMLUnit.compareXML(expectedSyndication, pmcMessages.get(0));

    List<String> pubmedMessages = dummySender.messagesSent.get("activemq:fake.pubmed.queue");
    assertEquals(pubmedMessages.size(), 1);
    XMLUnit.compareXML(expectedSyndication, pubmedMessages.get(0));

    // Confirm that disabling the article removes it from the solr index.
    inputView = entityGson.fromJson("{'state': 'disabled'}", ArticleInputView.class);
    article = articleStateService.update(articleId, inputView);
    assertEquals(article.getState(), PublicationState.DISABLED.getValue());
    assertEquals(dummySender.messagesSent.size(), 6);
    List<String> deletionMessages = dummySender.messagesSent.get("activemq:fake.delete.queue");
    assertEquals(deletionMessages.size(), 1);
    assertEquals(deletionMessages.get(0), article.getDoi());
    for (ArticleAsset asset : article.getAssets()) {
      checkFileExistence(AssetFileIdentity.from(asset), false);
    }

    // Attempting to publish the disabled article should fail.
    inputView = entityGson.fromJson("{'state': 'published'}", ArticleInputView.class);
    try {
      article = articleStateService.update(articleId, inputView);
      fail("Publication of disabled article succeeded");
    } catch (RestClientException expected) {
      assertEquals(expected.getResponseStatus(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    //Make sure we sent the item to the cross ref queue
    List<String> citedArticlesQueue = dummySender.messagesSent.get("activemq:fake.citedArticles.queue");
    assertEquals(citedArticlesQueue.size(), 1);
    assertEquals(citedArticlesQueue.get(0), article.getDoi());
    assertEquals(dummySender.headersSent.size(), 1);
    String[] headerKeys = dummySender.headersSent.keySet().toArray(new String[1]);
    assertEquals(headerKeys[0], ArticleStateServiceImpl.HEADER_AUTH_ID);
    assertNull(dummySender.headersSent.get(0));
  }
}
