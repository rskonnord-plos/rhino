package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.UUID;

@Controller
public class ArticleRevisionController extends RestController {

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Autowired
  private ArticleRevisionService articleRevisionService;

  @RequestMapping(value = "articles/revisions/create", method = RequestMethod.GET)
  public void createRevision(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam(value = "id", required = true) String doi,
                             @RequestParam(value = "r", required = true) Integer revisionNumber)
      throws IOException {

    // Just a dumb placeholder. TODO: Remove
    // This violates the idempotence of GET, obviously.

    ArticleRevision r = new ArticleRevision();
    r.setDoi(doi);
    r.setRevisionNumber(revisionNumber);
    r.setCrepoUuid(UUID.randomUUID().toString());
    hibernateTemplate.persist(r);
  }


  @Transactional(readOnly = true)
  @RequestMapping(value = "articles/revision", method = RequestMethod.GET)
  public void findRevision(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = "id", required = true) final String doi,
                           @RequestParam(value = "r", required = false) final Integer revisionNumber)
      throws IOException {

    // TODO: Pull into service class
    // For prototype, just using this method as a development sandbox

    String uuid;
    if (revisionNumber == null) {
      uuid = hibernateTemplate.execute(new HibernateCallback<String>() {
        @Override
        public String doInHibernate(Session session) throws HibernateException, SQLException {
          Query query = session.createQuery("select crepoUuid from ArticleRevision where doi=? order by revisionNumber desc");
          query.setString(0, doi);
          query.setMaxResults(1);
          return (String) DataAccessUtils.singleResult(query.list());
        }
      });
    } else {
      uuid = (String) DataAccessUtils.uniqueResult(
          hibernateTemplate.find("select crepoUuid from ArticleRevision where doi=? and revisionNumber=?",
              doi, revisionNumber));
    }
    if (uuid == null) {
      StringBuilder message = new StringBuilder().append("No revision exists with id=").append(doi);
      if (revisionNumber != null) {
        message.append(" and r=").append(revisionNumber);
      }
      throw new RestClientException(message.toString(), HttpStatus.NOT_FOUND);
    }

    try (PrintWriter writer = response.getWriter()) {
      writer.println(uuid);
    }
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/revision/ingest", method = RequestMethod.POST)
  public void ingest(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam("archive") MultipartFile requestFile)
      throws IOException {
    try (InputStream requestStream = requestFile.getInputStream()) {
      articleRevisionService.ingest(requestStream);
    } catch (XmlContentException e) {
      throw new RestClientException("Invalid XML", HttpStatus.BAD_REQUEST, e);
    }
  }

}
