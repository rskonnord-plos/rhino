/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.config;

import com.google.common.io.Closeables;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.DummyMessageSender;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SolrIndexService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.service.impl.AssetCrudServiceImpl;
import org.ambraproject.rhino.service.impl.CommentCrudServiceImpl;
import org.ambraproject.rhino.service.impl.SolrIndexServiceImpl;
import org.ambraproject.rhino.service.impl.SyndicationCrudServiceImpl;
import org.ambraproject.rhino.service.taxonomy.DummyTaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyServiceImpl;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.plos.crepo.service.ContentRepoService;
import org.plos.crepo.service.InMemoryContentRepoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
@Import(RhinoConfiguration.class)

// TODO: get tests to work transactionally
/* @EnableTransactionManagement(proxyTargetClass = true) */
public class TestConfiguration {

  /**
   * Dummy object for sanity-checking the unit test configuration.
   *
   * @return a dummy object
   */
  @Bean
  public Object sampleBean() {
    return new Object();
  }

  @Bean
  public DataSource dataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:testdb");
    dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
    return dataSource;
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory(DataSource dataSource) throws IOException {
    LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
    bean.setDataSource(dataSource);
    bean.setSchemaUpdate(true);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    bean.setHibernateProperties(hibernateProperties);

    return bean;
  }

  @Bean
  public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
    HibernateTransactionManager manager = new HibernateTransactionManager();
    manager.setSessionFactory(sessionFactory);
    return manager;
  }

  @Bean
  public ContentRepoService contentRepoService() {
    return new InMemoryContentRepoService("testBucket");
  }

  @Bean
  public ActiveMQConnectionFactory jmsConnectionFactory(RuntimeConfiguration runtimeConfiguration) {
    return new ActiveMQConnectionFactory();
  }


  @Bean
  public TaxonomyClassificationService taxonomyClassificationService() {
    return new DummyTaxonomyClassificationService();
  }

  @Bean
  public AssetCrudService assetService() {
    return new AssetCrudServiceImpl();
  }

  @Bean
  public MessageSender messageSender() {
    return new DummyMessageSender();
  }

  @Bean
  public SyndicationCrudService syndicationService() throws Exception {
    return new SyndicationCrudServiceImpl();
  }

  @Bean
  public SolrIndexService solrIndexService() {
    return new SolrIndexServiceImpl();
  }

  @Bean
  public CommentCrudService annotationCrudService() {
    return new CommentCrudServiceImpl();
  }

  @Bean
  public TaxonomyService taxonomyService() {
    return new TaxonomyServiceImpl();
  }

  @Bean
  public XpathReader xpathReader() {
    return new XpathReader();
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml) throws Exception {
    YamlConfiguration runtimeConfiguration;
    InputStream is = null;
    boolean threw = true;
    try {
      is = TestConfiguration.class.getClassLoader().getResourceAsStream("rhino-test.yaml");
      runtimeConfiguration = new YamlConfiguration(yaml.loadAs(is, YamlConfiguration.Input.class));
      threw = false;
    } finally {
      Closeables.close(is, threw);
    }

    return runtimeConfiguration;
  }
}
