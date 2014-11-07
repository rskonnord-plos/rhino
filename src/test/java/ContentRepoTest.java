/*
 * Copyright (c) 2006-2012 by Public Library of Science
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

import org.ambraproject.rhino.config.TestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.plos.crepo.service.contentRepo.impl.ContentRepoServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import javax.inject.Inject;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"servlet-test-context.xml"})
@ContextConfiguration({"classpath*:servlet-test-context.xml"})
public class ContentRepoTest {

  @Autowired
  protected ContentRepoServiceImpl contentRepoServiceImpl;


  @Test
  public void test(){

    assertTrue(true);

  }

}
