package org.ambraproject.rhino.config.mocks;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

/**
 * Created by jkrzemien on 8/15/14.
 */

public enum SimulatorConfigImpl implements SimulatorConfig {

  SINGLETON;

  private HibernateTransactionManager txmanager;
  private HibernateSimulatorConfig delegate;

  public void setTxManager(HibernateTransactionManager manager) {
    this.txmanager = manager;
  }

  private void refresh() {
    SessionFactory sessionFactory = txmanager.getSessionFactory();
    Session session;
    try {
      session = sessionFactory.getCurrentSession();
    } catch (HibernateException he) {
      session = sessionFactory.openSession();
    }
    this.delegate = (HibernateSimulatorConfig) session.createCriteria(HibernateSimulatorConfig.class).uniqueResult();
  }

  @Override
  public String getDatastorePath() {
    refresh();
    return delegate.getDatastorePath();
  }

  @Override
  public String getDatastoreDomain() {
    refresh();
    return delegate.getDatastoreDomain();
  }

  @Override
  public String getMockDataFolder() {
    refresh();
    return delegate.getMockDataFolder();
  }

  @Override
  public String getContentRepoAddress() {
    refresh();
    return delegate.getContentRepoAddress();
  }

  @Override
  public String getContentRepoBucketName() {
    refresh();
    return delegate.getContentRepoBucketName();
  }

  @Override
  public String getIngestionSrcFolder() {
    refresh();
    return delegate.getIngestionSrcFolder();
  }

  @Override
  public String getIngestionDestFolder() {
    refresh();
    return delegate.getIngestionDestFolder();
  }

  @Override
  public boolean isCaptureMockData() {
    refresh();
    return delegate.isCaptureMockData();
  }
}

