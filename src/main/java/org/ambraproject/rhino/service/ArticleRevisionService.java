package org.ambraproject.rhino.service;

import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.service.impl.AmbraService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArticleRevisionService extends AmbraService {

  public void ingest(InputStream archiveStream) throws IOException {
    String prefix = "ingest_" + new Date().getTime() + "_";
    Map<String, File> extracted = new HashMap<>();

    try {
      File manifestFile = null;
      try (ZipInputStream zipStream = new ZipInputStream(archiveStream)) {
        ZipEntry entry;
        while ((entry = zipStream.getNextEntry()) != null) {
          File tempFile = File.createTempFile(prefix, null);
          try (OutputStream tempFileStream = new FileOutputStream(tempFile)) {
            ByteStreams.copy(zipStream, tempFileStream);
          }

          String name = entry.getName();
          extracted.put(name, tempFile);
          if (name.equalsIgnoreCase("manifest.xml")) {
            manifestFile = tempFile;
          }
        }
      } finally {
        archiveStream.close();
      }
      if (manifestFile == null) {
        // TODO complain
      }

      ingest(extracted, manifestFile);
    } finally {
      for (File file : extracted.values()) {
        file.delete();
      }
    }
  }

  private void ingest(Map<String, File> files, File manifestFile) throws IOException {
    ManifestXml manifestXml;
    try (InputStream manifestStream = new BufferedInputStream(new FileInputStream(manifestFile))) {
      manifestXml = new ManifestXml(parseXml(manifestStream));
    }
    String manuscriptName = manifestXml.getArticleXml();
  }

}
