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

import org.ambraproject.rhino.config.mocks.SimulatorConfig;
import org.ambraproject.rhino.config.mocks.SimulatorConfigImpl;
import org.ambraproject.rhino.mocks.MockHttpClient;
import org.apache.commons.httpclient.HttpClient;
import org.hibernate.dialect.HSQLDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
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

  private static URL getResource(String resource) {
    return RhinoConfigurationTest.class.getResource(resource);
  }

  @Bean
  public File fileStorePath(SimulatorConfig simConfig) {
    return new File(getResource(simConfig.getDatastorePath()).getPath());
  }

  @Bean
  public String fileStoreDomain(SimulatorConfig simConfig) {
    return simConfig.getDatastoreDomain();
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory(DataSource hibernateDataSource) throws IOException {
    LocalSessionFactoryBean localSessionFactory = new LocalSessionFactoryBean();
    localSessionFactory.setDataSource(hibernateDataSource);
    List<Resource> mappings = getAmbraMappings();
    mappings.add(new FileSystemResource(getResource("/ambra/configuration/Simulator.hbm.xml").getPath()));
    localSessionFactory.setMappingLocations(mappings.toArray(new Resource[mappings.size()]));
    localSessionFactory.setHibernateProperties(hibernateProperties());
    return localSessionFactory;
  }

  @Bean
  public HttpClient httpClient(SimulatorConfig simConfig) {
    return new MockHttpClient(simConfig);
  }

  @Bean
  public Object yamlConfigOverrider(RuntimeConfiguration yamlConfig, SimulatorConfig simConfig) throws URISyntaxException {
    YamlConfiguration.UserFields uf = getFieldValue(yamlConfig, "uf", YamlConfiguration.UserFields.class);
    uf.setcontentRepoAddress(new URI(simConfig.getContentRepoAddress()));
    uf.setRepoBucketName(simConfig.getContentRepoBucketName());
    return new Object(); // Nobody cares...
  }

  @Bean
  public Object ambraConfigOverrider(org.apache.commons.configuration.Configuration ambraConfiguration, SimulatorConfig simConfig) throws URISyntaxException {
    ambraConfiguration.setProperty(INGEST_SOURCE_DIR_KEY, getResource(simConfig.getIngestionSrcFolder()).getPath());
    ambraConfiguration.setProperty(INGEST_DEST_DIR_KEY, getResource(simConfig.getIngestionDestFolder()).getPath());
    return new Object(); // Nobody cares...
  }

  @Bean
  public SimulatorConfig loadSimulatorConfig(HibernateTransactionManager txManager) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new UrlResource(getResource("/sql/minimum.sql")));
    DatabasePopulatorUtils.execute(populator, txManager.getDataSource());
    SimulatorConfigImpl.SINGLETON.setTxManager(txManager);
    return SimulatorConfigImpl.SINGLETON;
  }

  Properties hibernateProperties() {
    return new Properties() {
      {
        setProperty("hibernate.dialect", HSQLDialect.class.getName());
        setProperty("hibernate.show_sql", Boolean.FALSE.toString());
        setProperty("hibernate.format_sql", Boolean.FALSE.toString());
        setProperty("hibernate.hbm2ddl.auto", "create"); //create-drop
        // setProperty("hibernate.cache.use_query_cache", Boolean.FALSE.toString());
        // setProperty("hibernate.cache.use_second_level_cache", Boolean.FALSE.toString());
      }
    };
  }
}


