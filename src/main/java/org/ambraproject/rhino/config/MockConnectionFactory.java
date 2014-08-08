package org.ambraproject.rhino.config;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;

import static java.lang.System.out;

/**
 * Created by jkrzemien on 8/8/14.
 */
public class MockConnectionFactory extends MockQueueConnectionFactory {

//  private final DestinationManager destinationManager;
//  private final ConfigurationManager configurationManager;

  public MockConnectionFactory(DestinationManager destinationManager, ConfigurationManager configurationManager) {
    super(destinationManager, configurationManager);
//    this.destinationManager = destinationManager;
//    this.configurationManager = configurationManager;

    destinationManager.createQueue("plos.solr.article.index");
    out.println("CREATED MOCK QUEUE FOR SOLR article index");
    destinationManager.createQueue("plos.updatedCitedArticles");
    out.println("CREATED MOCK QUEUE FOR updatedCitedArticles");
    destinationManager.createQueue("CROSSREF");
    out.println("CREATED MOCK QUEUE FOR CROSSREF");
  }
}
