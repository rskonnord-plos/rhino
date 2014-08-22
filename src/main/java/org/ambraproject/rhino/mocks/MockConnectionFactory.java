package org.ambraproject.rhino.mocks;

import com.google.common.base.Preconditions;
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
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Created by jkrzemien on 8/8/14.
 *
 * THIS CLASS IS NOT FINISHED YET. IT JUST MAKES RHINO THINK THERE IS A QUEUE, BUT THERE IS NO WAY
 * OF RETRIEVING THE MESSAGES FROM THE OUTSIDE WORLD YET (TESTS)
 */
public class MockConnectionFactory extends MockQueueConnectionFactory {

  private final static Logger log = LoggerFactory.getLogger(MockConnectionFactory.class);
  private Set<MockQueue> queues = new HashSet<MockQueue>();

  public MockConnectionFactory(DestinationManager destinationManager, ConfigurationManager configurationManager) {
    super(destinationManager, configurationManager);
  }

  public void setQueueNames(List<String> queueNames) {
    for (String queue : checkNotNull(queueNames)) {
      queues.add(destinationManager().createQueue(queue));
      log.info(format("Created mock queue for %s", queue));
    }
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
        log.info("Message arrived! = " + message.getJMSMessageID());
      } catch (JMSException e) {
        e.printStackTrace();
      }
    }
  }
}
