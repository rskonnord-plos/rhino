/*
 * $HeadURL$
 * $Id$
 *
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

package org.ambraproject.admin;

import org.ambraproject.models.Article;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Does a sanity check on the admin application's ability to access Ambra's models and database.
 */
@Controller
public class DemoController {

  @Autowired
  private org.springframework.orm.hibernate3.LocalSessionFactoryBean hibernateSessionFactory;

  /**
   * Populate the page with data retrieved from the persistence layer.
   */
  @RequestMapping(value = "/demo", method = RequestMethod.GET)
  public String demo(Model model) {
    HibernateTemplate hibernateTemplate = new HibernateTemplate(hibernateSessionFactory.getObject());
    List<String> dois = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .setProjection(Projections.property("doi"))
    );
    model.addAttribute("articleCount", dois.size());
    model.addAttribute("articleDoiList", dois);
    return "demo";
  }

}
