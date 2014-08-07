package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class AssetFileCrudControllerTest {

  @Autowired
  protected WebApplicationContext context;
  @Autowired
  private AssetFileCrudController assetFileCrudController;
  @Autowired
  private AssetCrudService mockAssetCrudService;

  @Test
  public void testRead() throws Exception {
    MockMvc mockMvc = webAppContextSetup(context)
        .alwaysExpect(handler().handlerType(AssetFileCrudController.class))
        .alwaysExpect(status().isOk())
        .alwaysExpect(forwardedUrl(null))
        .alwaysExpect(redirectedUrl(null))
        .build();
    String testAssetId = "testId.txt";
    when(mockAssetCrudService.read(AssetFileIdentity.parse(testAssetId)))
        .thenReturn(new ByteArrayInputStream(new byte[0]));
    mockMvc.perform(get("/assetfiles/{0}", testAssetId)).andReturn();
  }

  @Configuration
  public static class TestConfiguration {
    @Bean
    public AssetFileCrudController assetFileCrudController() {
      return new AssetFileCrudController();
    }

    @Bean
    public Gson gson() {
      return new Gson();
    }

    @Bean
    public ArticleCrudService articleCrudService() {
      return null;
    }

    @Bean
    public AssetCrudService assetCrudService() {
      return mock(AssetCrudService.class);
    }

    @Bean
    public FileStoreService fileStoreService() {
      return null;
    }
  }

}
