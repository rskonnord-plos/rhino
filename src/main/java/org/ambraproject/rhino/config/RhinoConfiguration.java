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

import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.configuration.ConfigurationStore;
import org.ambraproject.rhino.config.json.AdapterRegistry;
import org.ambraproject.rhino.config.json.ExclusionSpecialCase;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.ClassificationService;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.service.impl.AnnotationCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleStateServiceImpl;
import org.ambraproject.rhino.service.impl.AssetCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ClassificationServiceImpl;
import org.ambraproject.rhino.service.impl.ConfigurationReadServiceImpl;
import org.ambraproject.rhino.service.impl.IngestibleServiceImpl;
import org.ambraproject.rhino.service.impl.IssueCrudServiceImpl;
import org.ambraproject.rhino.service.impl.JournalReadServiceImpl;
import org.ambraproject.rhino.service.impl.PingbackReadServiceImpl;
import org.ambraproject.rhino.service.impl.VolumeCrudServiceImpl;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.service.crossref.CrossRefLookupService;
import org.ambraproject.service.crossref.CrossRefLookupServiceImpl;
import org.ambraproject.service.taxonomy.TaxonomyService;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

/**
 * Bean configuration for the application.
 * <p/>
 * This augments some other bean configurations located at {@code src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml}.
 */
@Configuration
public class RhinoConfiguration extends BaseConfiguration {

  /**
   * Static stuff outside the Spring framework that needs to be run on startup. Ideally this should be empty.
   */
  private static void doStartupKludges() {
    // Required by Ambra; org.ambraproject.configuration.ConfigurationStore, org.ambraproject.util.URIGenerator
    System.setProperty("SYSTEM_OBJECT_ID_PREFIX", "info:doi/0.0/");
  }

  static {
    doStartupKludges();
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory(DataSource hibernateDataSource) throws IOException {
    LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
    bean.setDataSource(hibernateDataSource);
    setAmbraMappings(bean);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
    hibernateProperties.setProperty("hibernate.show_sql", Boolean.FALSE.toString());
    hibernateProperties.setProperty("hibernate.format_sql", Boolean.FALSE.toString());
    bean.setHibernateProperties(hibernateProperties);

    return bean;
  }

  @Bean
  public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
    return new HibernateTemplate(sessionFactory);
  }

  /**
   * Gson instance for serializing Ambra entities into human-friendly JSON.
   */
  @Bean
  public Gson entityGson(RuntimeConfiguration runtimeConfiguration) {
    GsonBuilder builder = JsonAdapterUtil.makeGsonBuilder();
    if (runtimeConfiguration.prettyPrintJson()) {
      builder.setPrettyPrinting();
    }

    // Bulk-apply special cases defined in org.ambraproject.rhino.config.json
    builder.setExclusionStrategies(ExclusionSpecialCase.values());
    for (Class<? extends JsonOutputView> viewClass : AdapterRegistry.getOutputViewClasses()) {
      builder.registerTypeAdapter(viewClass, JsonOutputView.SERIALIZER);
    }
    for (Map.Entry<Type, Object> entry : AdapterRegistry.getCustomAdapters().entrySet()) {
      builder.registerTypeAdapter(entry.getKey(), entry.getValue());
    }

    return builder.create();
  }

  @Bean
  public org.apache.commons.configuration.Configuration ambraConfiguration() {
    // Fetch from Ambra's custom container
    return ConfigurationStore.getInstance().getConfiguration();
  }

  @Bean
  public HttpClient httpClient() {
    HttpConnectionManagerParams params = new HttpConnectionManagerParams();
    params.setSoTimeout(30000);
    params.setConnectionTimeout(30000);
    MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
    manager.setParams(params);
    return new HttpClient(manager);
  }

  @Bean
  public CrossRefLookupService crossRefLookupService(HttpClient httpClient,
                                                     org.apache.commons.configuration.Configuration ambraConfiguration) {
    CrossRefLookupServiceImpl service = new CrossRefLookupServiceImpl();
    service.setHttpClient(httpClient);
    service.setCrossRefUrl((String) ambraConfiguration.getProperty("ambra.services.crossref.query.url"));
    return service;
  }


  @Bean
  public ArticleCrudService articleCrudService() {
    ArticleCrudService service = new ArticleCrudServiceImpl();
    service.setAssetService(assetCrudService());
    return service;
  }

  @Bean
  public AssetCrudService assetCrudService() {
    return new AssetCrudServiceImpl();
  }

  @Bean
  public VolumeCrudService volumeCrudService() {
    return new VolumeCrudServiceImpl();
  }

  @Bean
  public IssueCrudService issueCrudService() {
    return new IssueCrudServiceImpl();
  }

  @Bean
  public IngestibleService ingestibleService() {
    return new IngestibleServiceImpl();
  }

  @Bean
  public ArticleStateService articleStateService() {
    return new ArticleStateServiceImpl();
  }

  @Bean
  ConfigurationReadService configurationReadService() {
    return new ConfigurationReadServiceImpl();
  }

  @Bean
  public PingbackReadService pingbackReadService() {
    return new PingbackReadServiceImpl();
  }

  @Bean
  public JournalReadService journalReadService() {
    return new JournalReadServiceImpl();
  }

  @Bean
  public AnnotationCrudService annotationCrudService() {
    return new AnnotationCrudServiceImpl();
  }

  @Bean
  public ClassificationService classificationService(TaxonomyService taxonomyService) {
    return new ClassificationServiceImpl(taxonomyService);
  }

  @Bean
  public XpathReader xpathReader() {
    return new XpathReader();
  }

  @Bean
  public Yaml yaml() {
    return new Yaml();
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml) throws Exception {
    final File configPath = new File("/etc/ambra/rhino.yaml");
    if (!configPath.exists()) {
      throw new RuntimeConfigurationException(configPath.getAbsolutePath() + " not found");
    }

    YamlConfiguration runtimeConfiguration;
    Reader reader = null;
    boolean threw = true;
    try {
      reader = new BufferedReader(new FileReader(configPath));
      runtimeConfiguration = new YamlConfiguration(yaml.loadAs(reader, YamlConfiguration.UserFields.class));
      threw = false;
    } finally {
      Closeables.close(reader, threw);
    }

    return runtimeConfiguration;
  }
}
