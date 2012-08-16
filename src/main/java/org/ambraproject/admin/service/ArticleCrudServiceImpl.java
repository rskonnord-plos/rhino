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

package org.ambraproject.admin.service;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.XPathBatch;
import org.ambraproject.admin.util.NodeListAdapter;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 * <p/>
 * This is proof-of-concept code; it isn't necessarily correct for representing articles in the database and
 * (especially) the file store.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  private static final String DOI_PREFIX = "info:doi/";

  private static final XPathBatch INGESTION = XPathBatch.fromMap(ImmutableMap.<String, String>builder()
      .put("Doi", "/article/front/article-meta/article-id[@pub-id-type=\"doi\"]")
          // Just a stub
      .build());

  public static final XPath XPATH = XPathFactory.newInstance().newXPath();

  private boolean articleExistsAt(String doi) {
    Long articleCount = (Long) hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", doi))
        .setProjection(Projections.rowCount())
    ).get(0);
    return articleCount > 0L;
  }

  private RestClientException reportDoiNotFound() {
    return new RestClientException("DOI does not belong to an article", HttpStatus.NOT_FOUND);
  }

  /**
   * Produce the file store ID for an article's base XML file.
   *
   * @param doi the DOI of an article
   * @return the FSID for the article's XML file
   * @throws RestClientException if the DOI can't be parsed and converted into an FSID
   */
  private static String findFsidForArticleXml(String doi) {
    String fsid = FSIDMapper.doiTofsid(doi, "XML");
    if (fsid.isEmpty()) {
      throw new RestClientException("DOI does not match expected format", HttpStatus.BAD_REQUEST);
    }
    return fsid;
  }


  /**
   * Read a client-provided stream into memory. Report it as a client error if the stream cannot be read. Closes the
   * stream.
   *
   * @param input an input stream from a RESTful request
   * @return a byte array of the input stream contents
   */
  private byte[] readClientInput(InputStream input) {
    try {
      return IOUtils.toByteArray(input);
    } catch (IOException e) {
      throw new RestClientException("Could not read provided file", HttpStatus.BAD_REQUEST, e);
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        throw new RestClientException("Error closing file stream from client", HttpStatus.BAD_REQUEST, e);
      }
    }
  }

  /**
   * Write the base article XML to the file store. The DOI is used to generate the FSID. If something is already stored
   * for that DOI, it is overwritten; else, a new file is created.
   *
   * @param fileData the data to write, as raw bytes
   * @param doi      the article XML
   * @throws FileStoreException
   * @throws IOException
   */
  private void write(byte[] fileData, String doi) throws FileStoreException, IOException {
    String fsid = findFsidForArticleXml(doi);
    OutputStream output = null;
    try {
      output = fileStoreService.getFileOutStream(fsid, fileData.length);
      output.write(fileData);
    } finally {
      IOUtils.closeQuietly(output);
    }

  }

  private Article prepareMetadata(byte[] xmlData, String doi) {
    InputStream xmlStream = null;
    Document xml;
    try {
      xmlStream = new ByteArrayInputStream(xmlData);
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      xml = documentBuilder.parse(xmlStream);
    } catch (SAXException e) {
      throw new RestClientException("Invalid XML", HttpStatus.BAD_REQUEST, e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(xmlStream);
    }

    Article article;
    try {
      article = prepareMetadata(xml, doi);
    } catch (XPathExpressionException e) {
      throw new RestClientException("XML does not match expected format", HttpStatus.BAD_REQUEST, e);
    }

    return article;
  }

  private static List<Node> xpathQueryForNodes(String query, Document xml) throws XPathExpressionException {
    Object result = XPATH.evaluate(query, xml, XPathConstants.NODESET);
    return NodeListAdapter.wrap((NodeList) result);
  }

  /**
   * Read metadata from an XML file into a new article representation.
   * <p/>
   * TODO Clean up and finish implementing
   *
   * @param xml the XML file for the new article
   * @param doi the article's DOI, according to the action that wants to create the article
   * @return the new article object
   * @throws XPathExpressionException if the XML cannot be evaluated with the expected expressions
   */
  private Article prepareMetadata(Document xml, String doi) throws XPathExpressionException {
    Article article = new Article();
    Map<String, String> results = INGESTION.evaluate(xml);

    String xmlDoi = XPATH.evaluate("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]", xml);
    if (!doi.equals(DOI_PREFIX + xmlDoi)) {
      if (log.isWarnEnabled()) {
        log.warn("Article at DOI=" + doi + " has XML listing DOI as " + DOI_PREFIX + xmlDoi);
      }
    }
    article.setDoi(doi);

    List<Node> people = xpathQueryForNodes("//person-group", xml);

    article.setDate(new Date()); // TODO Should be defined in XML instead?

    return article;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void create(InputStream file, String doi) throws IOException, FileStoreException {
    if (articleExistsAt(doi)) {
      throw new RestClientException("Can't create article; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }
    byte[] xmlData = readClientInput(file);

    Article article = prepareMetadata(xmlData, doi);
    hibernateTemplate.save(article);

    write(xmlData, doi);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] read(String doi) throws FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    String fsid = findFsidForArticleXml(doi);

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileByteArray(fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(InputStream file, String doi) throws IOException, FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    write(readClientInput(file), doi);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String doi) throws FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }

    Article article = (Article) hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", doi))
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).get(0);
    hibernateTemplate.delete(article);

    fileStoreService.deleteFile(doi);
  }

}
