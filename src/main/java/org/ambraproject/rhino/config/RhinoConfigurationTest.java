/*
 * Copyright (c) 2006-2014 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.config;

import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.filestore.impl.FileSystemImpl;
import org.ambraproject.rhino.mocks.MockDataSource;
import org.ambraproject.rhino.mocks.MockHttpClient;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.hibernate.dialect.HSQLDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import static org.ambraproject.rhino.config.ReflectionUtils.getFieldValue;

/**
 * Created by jkrzemien on 8/6/14.
 */

/**
 * With the @Profile annotation, configuration classes just get imported IF the profile mentioned in the annotation is active.
 */
@Profile("testing")
@Configuration
@EnableTransactionManagement
public class RhinoConfigurationTest extends BaseConfiguration {

  private static final String INGEST_SOURCE_DIR_KEY = "ambra.services.documentManagement.ingestSourceDir";

  private static final String INGEST_DEST_DIR_KEY = "ambra.services.documentManagement.ingestDestinationDir";

  /*
  This is ONE way of overriding DataSource and FileStore.
  The OTHER way (AFAIK) is doing it like I mock out jmsConnectionFactory bean in servlet-context.xml using Spring's Profiles
  via XML.
  I'm just showing alternatives here...it is a PoC...
  */

  static {
    try {
      File dataStore = new File(getResource("/datastores").getPath());
      SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
      builder.bind("java:comp/env/jdbc/AmbraDS", MockDataSource.SINGLETON.get());
      builder.bind("java:comp/env/ambra/FileStore", new FileSystemImpl(dataStore, "test"));
      builder.activate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static URL getResource(String resource) {
    return RhinoConfigurationTest.class.getResource(resource);
  }

  @Bean
  public FileStoreService fileStoreService() throws IOException {
    File dataStore = new File(getResource("/datastores").getPath());
    return new FileSystemImpl(dataStore, "test");
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory(DataSource hibernateDataSource) throws IOException {
    LocalSessionFactoryBean localSessionFactory = new LocalSessionFactoryBean();
    localSessionFactory.setDataSource(hibernateDataSource);
    setAmbraMappings(localSessionFactory);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", HSQLDialect.class.getName());
    hibernateProperties.setProperty("hibernate.show_sql", Boolean.FALSE.toString());
    hibernateProperties.setProperty("hibernate.format_sql", Boolean.FALSE.toString());
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create"); //create-drop
//    hibernateProperties.setProperty("hibernate.cache.use_query_cache", Boolean.FALSE.toString());
//    hibernateProperties.setProperty("hibernate.cache.use_second_level_cache", Boolean.FALSE.toString());
    localSessionFactory.setHibernateProperties(hibernateProperties);

    return localSessionFactory;
  }

  @Bean
  public HttpClient httpClient() {
    return new MockHttpClient(getResource("/mockdata/httpmocks").getPath());
  }

  @Bean
  public Object configOverrider(RuntimeConfiguration yamlConfig, org.apache.commons.configuration.Configuration ambraConfiguration) throws IllegalAccessException, URISyntaxException {
    YamlConfiguration.UserFields uf = getFieldValue(yamlConfig, "uf", YamlConfiguration.UserFields.class);
    uf.setcontentRepoAddress(new URI("http://nobodycares.com/aRepoURL"));
    uf.setRepoBucketName("daRepoBucketName");

    ambraConfiguration.setProperty(INGEST_SOURCE_DIR_KEY, getResource("/datastores/src").getPath());
    ambraConfiguration.setProperty(INGEST_DEST_DIR_KEY, getResource("/datastores/dest").getPath());
    return new Object(); // Nobody cares...
  }

  @Bean
  public Object databasePopulator(DataSource ds) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new UrlResource(getResource("/sql/minimum.sql")));
    DatabasePopulatorUtils.execute(populator, ds);
    return new Object(); // Nobody cares...
  }

}


