package org.ambraproject.rhino.mocks;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jkrzemien on 8/8/14.
 */
public class MockConnectionFactory extends MockQueueConnectionFactory {

  private final static Logger log = LoggerFactory.getLogger(MockConnectionFactory.class);

  public MockConnectionFactory(DestinationManager destinationManager, ConfigurationManager configurationManager) {
    super(destinationManager, configurationManager);

    destinationManager.createQueue("plos.solr.article.index");
    log.info("Created mock queue for SOLR article index");
    destinationManager.createQueue("plos.updatedCitedArticles");
    log.info("Created mock queue for updatedCitedArticles");
    destinationManager.createQueue("CROSSREF");
    log.info("Created mock queue for CROSSREF");
  }
}
