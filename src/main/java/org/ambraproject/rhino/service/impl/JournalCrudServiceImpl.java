package org.ambraproject.rhino.service.impl;

import com.google.common.io.Closeables;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class JournalCrudServiceImpl extends AmbraService implements JournalCrudService {

  @Autowired
  private HibernateTemplate hibernateTemplate;
  @Autowired
  private FileStoreService fileStoreService;

  private static DetachedCriteria journalCriteria() {
    return DetachedCriteria.forClass(Journal.class)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .addOrder(Order.asc("journalKey"))
        ;
  }

  @Override
  public void listJournals(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    List<Journal> journals = hibernateTemplate.findByCriteria(journalCriteria());
    KeyedListView<JournalNonAssocView> view = JournalNonAssocView.wrapList(journals);
    writeJson(receiver, view);
  }

  @Override
  public void read(ResponseReceiver receiver, String journalKey, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    Journal journal = (Journal) DataAccessUtils.singleResult((List<?>)
        hibernateTemplate.findByCriteria(journalCriteria()
            .add(Restrictions.eq("journalKey", journalKey))
            .setFetchMode("volumes", FetchMode.JOIN)
            .setFetchMode("volumes.issues", FetchMode.JOIN)
        ));
    if (journal == null) {
      throw new RestClientException("No journal found with key: " + journalKey, HttpStatus.NOT_FOUND);
    }
    writeJson(receiver, new JournalOutputView(journal));
  }

  private static String journalFsid(String journalKey) {
    return internalFsid("journal_" + journalKey);
  }

  @Override
  public void writeFrontEndBundle(String journalKey, InputStream input) throws IOException {
    byte[] data = IOUtils.toByteArray(input);
    input.close(); // just closing it early; we still expect the caller to close it
    String fsid = journalFsid(journalKey);

    OutputStream outputStream = null;
    boolean threw = true;
    try {
      outputStream = fileStoreService.getFileOutStream(fsid, data.length);
      outputStream = new BufferedOutputStream(outputStream);
      outputStream.write(data);
    } catch (FileStoreException e) {
      throw new IOException(e);
    } finally {
      Closeables.close(outputStream, threw);
    }
  }

  @Override
  public InputStream readFrontEndBundle(String journalKey) throws IOException {
    String fsid = journalFsid(journalKey);
    try {
      return fileStoreService.getFileInStream(fsid);
    } catch (FileStoreException e) {
      throw new IOException(e);
    }
  }

}
