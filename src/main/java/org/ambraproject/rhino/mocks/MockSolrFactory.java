package org.ambraproject.rhino.mocks;

import org.ambraproject.service.search.SolrServerFactory;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by jkrzemien on 8/12/14.
 *
 * THIS CLASS IS NOT FINISHED YET. IT JUST MAKES RHINO THINK THERE IS A SOLR SERVER, BUT THERE IS NO WAY
 * OF RETRIEVING THE DOCUMENTS FROM THE OUTSIDE WORLD YET (TESTS)
 */
public class MockSolrFactory extends SolrServerFactory {

  private static final Logger log = LoggerFactory.getLogger(MockSolrFactory.class);
  private static MockSolrFactory instance;
  private final MockSolrServer solrServer;

  static {
    final Object lock = new Object();
    synchronized (lock) {
      try {
        Configuration configuration = new CombinedConfiguration();
        configuration.setProperty("ambra.services.search.server.url", "http://nobodycares.com/");
        instance = new MockSolrFactory(configuration);
        log.info("Mock SOLR server FACTORY instantiated! **chills**");
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
  }

  private MockSolrFactory(Configuration configuration) throws MalformedURLException {
    super(configuration);
    this.solrServer = new MockSolrServer();
  }

  public static MockSolrFactory getInstance() {
    return instance;
  }

  @Override
  public SolrServer getServer() {
    log.info("Mock SOLR server requested");
    return solrServer;
  }

  public class MockSolrServer extends SolrServer {

    private final Logger log = LoggerFactory.getLogger(MockSolrServer.class);

    private int numberOfCommits;

    private int numberOfOptimize;

    private List<SolrInputDocument> addedDocuments;

    private Map<String, NamedList<Object>> requestsResponses;

    public MockSolrServer() {
      super();
      this.addedDocuments = new LinkedList<SolrInputDocument>();
      this.requestsResponses = new HashMap<String, NamedList<Object>>();
    }

    @Override
    public UpdateResponse commit() throws SolrServerException, IOException {
      log.info("Commiting to mock SOLR server");
      numberOfCommits++;
      UpdateResponse response = new UpdateResponse();
      return response;
    }

    @Override
    public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
      log.info("Adding document to mock SOLR server");
      addedDocuments.add(doc);
      UpdateResponse response = new UpdateResponse();
      return response;
    }

    @Override
    public NamedList<Object> request(SolrRequest request) throws SolrServerException, IOException {
      log.info("Request arrived to mock SOLR server");
      return requestsResponses.get(request.getPath());
    }

    @Override
    public void shutdown() {
      log.info("Shutting down mock SOLR server");
      // Meh...
      addedDocuments.clear();
      requestsResponses.clear();
    }

    @Override
    public UpdateResponse optimize() throws SolrServerException, IOException {
      log.info("Optimizing mock SOLR server...yeah, right...");
      numberOfOptimize++;
      UpdateResponse response = new UpdateResponse();
      return response;
    }

    public List<SolrInputDocument> getAddedDocuments() {
      return addedDocuments;
    }

    public void setAddedDocuments(List<SolrInputDocument> addedDocuments) {
      this.addedDocuments = addedDocuments;
    }

    public int getNumberOfOptimize() {
      return numberOfOptimize;
    }

    public int getNumberOfCommits() {
      return numberOfCommits;
    }

    public void setNumberOfCommits(int numberOfCommits) {
      this.numberOfCommits = numberOfCommits;
    }

    public void setResponseToRequest(String request, NamedList<Object> response) {
      requestsResponses.put(request, response);
    }

  }
}
