package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import org.jooq.DSLContext;
import static org.ambraproject.rhino.model.generatedclasses.Tables.*;

@Controller
public class TestJooqController extends RestController {

  @Autowired
  DSLContext create;

  @Transactional(value="jooqTXM", readOnly = true)
  @ResponseBody
  @RequestMapping(value = "/jooq", method = RequestMethod.GET)
  public String testSql(HttpServletRequest request) {
    return create.selectFrom(ARTICLE).limit(15).fetch().formatJSON();
  }

}
