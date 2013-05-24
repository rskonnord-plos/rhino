package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;
import java.io.InputStream;

public interface JournalCrudService {

  public abstract void listJournals(ResponseReceiver receiver, MetadataFormat format) throws IOException;

  public abstract void read(ResponseReceiver receiver, String journalKey, MetadataFormat format) throws IOException;

  public abstract void writeFrontEndBundle(String journalKey, InputStream input) throws IOException;

  public abstract InputStream readFrontEndBundle(String journalKey) throws IOException;

}
