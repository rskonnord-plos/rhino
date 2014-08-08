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
import org.apache.commons.httpclient.HttpClient;
import org.hibernate.dialect.HSQLDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static org.ambraproject.rhino.config.ReflectionUtils.getFieldValue;
import static org.ambraproject.rhino.config.ReflectionUtils.setFieldValue;

/**
 * Created by jkrzemien on 8/6/14.
 */

/**
 * With the @Profile annotation, configuration classes just get imported if the profile mentioned in the annotation is active.
 */
@Profile("testing")
@Configuration
@EnableTransactionManagement
public class RhinoConfigurationTest extends BaseConfiguration {

  /*
  This is one way of overriding DataSource and FileStore.
  The other way is doing it like I mock out jmsConnectionFactory bean in servlet-context.xml using Spring's Profiles
  via XML.
   */

  static {
    try {
      SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
      builder.bind("java:comp/env/jdbc/AmbraDS", MockDataSource.SINGLETON.get());
      builder.bind("java:comp/env/ambra/FileStore", new FileSystemImpl(new File("/tmp"), "test"));
      builder.activate();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Bean
  public FileStoreService fileStoreService() throws IOException {
    return new FileSystemImpl(new File("/tmp"), "test");
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
    hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
//    hibernateProperties.setProperty("hibernate.cache.use_query_cache", Boolean.FALSE.toString());
//    hibernateProperties.setProperty("hibernate.cache.use_second_level_cache", Boolean.FALSE.toString());
    localSessionFactory.setHibernateProperties(hibernateProperties);

    return localSessionFactory;
  }

  @Bean
  public HttpClient httpClient() {
    return new MockHttpClient("/tmp/httpmocks");
  }

  @Bean
  public Object configOverrider(RuntimeConfiguration configuration) throws IllegalAccessException, URISyntaxException {
    YamlConfiguration.UserFields uf = getFieldValue(configuration, "uf", YamlConfiguration.UserFields.class);
    uf.setcontentRepoAddress(new URI("http://nobodycares.com/aRepoURL"));
    uf.setRepoBucketName("daRepoBucketName");
    return new Object(); // Nobody cares...
  }

}


