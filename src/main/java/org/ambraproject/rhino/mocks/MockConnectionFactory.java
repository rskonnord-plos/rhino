package org.ambraproject.rhino.mocks;

import com.mockrunner.jms.ConfigurationManager;
import com.mockrunner.jms.DestinationManager;
import com.mockrunner.mock.jms.MockConnection;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jkrzemien on 8/8/14.
 */
public class MockConnectionFactory extends MockQueueConnectionFactory {

  private final static Logger log = LoggerFactory.getLogger(MockConnectionFactory.class);
  private Set<MockQueue> queues = new HashSet<MockQueue>();

  public MockConnectionFactory(DestinationManager destinationManager, ConfigurationManager configurationManager) {
    super(destinationManager, configurationManager);

    queues.add(destinationManager.createQueue("plos.solr.article.index"));
    log.info("Created mock queue for SOLR article index");
    queues.add(destinationManager.createQueue("plos.updatedCitedArticles"));
    log.info("Created mock queue for updatedCitedArticles");
    queues.add(destinationManager.createQueue("CROSSREF"));
    log.info("Created mock queue for CROSSREF");
  }

  @Override
  public Connection createConnection() throws JMSException {
    MockConnection conn = (MockConnection) super.createQueueConnection();
    for (MockQueue mq : queues) {
      registerMessageListenerForQueue(mq, conn);
    }
    return conn;
  }

  private void registerMessageListenerForQueue(MockQueue queue, MockConnection mockConnection) {
    try {
      MockSession session = (MockSession) mockConnection.createSession(false, 0);
      MessageConsumer consumer = session.createConsumer(queue, "test");
      consumer.setMessageListener(new MockQueueListener());
      mockConnection.start();
    } catch (JMSException e) {
      e.printStackTrace();
    }
  }

  private class MockQueueListener implements MessageListener {
    @Override
    public void onMessage(Message message) {
      try {
        log.info("ME LLEGO UN FUCKING MENSAJE! = " + message.getJMSMessageID());
      } catch (JMSException e) {
        e.printStackTrace();
      }

    }
  }
}
