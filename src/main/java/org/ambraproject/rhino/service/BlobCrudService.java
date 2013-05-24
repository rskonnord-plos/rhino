package org.ambraproject.rhino.service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Generic service for writing data to the file store. This interface is type-agnostic about the data; it is the
 * caller's responsibility to pass in identifiers specific to the data.
 */
public interface BlobCrudService {

  /**
   * Write a data blob to the file store. If a blob already exists at the same identifier, it will be overwritten.
   * <p/>
   * This method <em>may</em> close the stream, but the caller <em>must</em> close it in a {@code finally} block.
   *
   * @param id    an identifier for the data blob
   * @param input a stream containing the data
   * @throws IOException if there is an error writing to the file store
   */
  public abstract void write(String id, InputStream input) throws IOException;

  /**
   * Read a data blob from the file store.
   * <p/>
   * The caller is responsible for closing the returned stream.
   *
   * @param id the identifier for an existing data blob
   * @return a stream containing the data
   * @throws IOException if no file exists at the identifier or there is an error reading from the file store
   */
  public abstract InputStream read(String id) throws IOException;

  /**
   * Delete a data blob from the file store.
   *
   * @param id the identifier for an existing data blob
   * @throws IOException if there is an error modifying the file store
   */
  public abstract void delete(String id) throws IOException;

}
