package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.rhino.service.BlobCrudService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BlobCrudServiceImpl implements BlobCrudService {

  @Autowired
  private FileStoreService fileStoreService;

  private static String internalFsid(String id) {
    return "__rhino" + File.separator + Preconditions.checkNotNull(id);
  }

  @Override
  public void write(String id, InputStream input) throws IOException {
    Preconditions.checkNotNull(input);
    String fsid;
    byte[] data;
    boolean threw = true;
    try {
      fsid = internalFsid(id);

      /*
       * Copying stream-to-stream would be nice, but fileStoreService needs to know
       * the total size before opening the OutputStream. So dump it into memory.
       */
      data = IOUtils.toByteArray(input); // buffered
      threw = false;
    } finally {
      Closeables.close(input, threw);
    }

    OutputStream outputStream = null;
    threw = true;
    try {
      outputStream = fileStoreService.getFileOutStream(fsid, data.length);
      outputStream = new BufferedOutputStream(outputStream);
      outputStream.write(data);
      threw = false;
    } catch (FileStoreException e) {
      throw new IOException(e);
    } finally {
      Closeables.close(outputStream, threw);
    }
  }

  @Override
  public InputStream read(String id) throws IOException {
    String fsid = internalFsid(id);
    try {
      return fileStoreService.getFileInStream(fsid);
    } catch (FileStoreException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void delete(String id) throws IOException {
    String fsid = internalFsid(id);
    try {
      fileStoreService.deleteFile(fsid);
    } catch (FileStoreException e) {
      throw new IOException(e);
    }
  }

}
