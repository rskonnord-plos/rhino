package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArticleRevisionService extends AmbraService {

  @Autowired
  private ContentRepoService versionedContentRepoService;

  public void ingest(InputStream archiveStream) throws IOException, XmlContentException {
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

      RepoCollectionMetadata articleCollection = writeCollection(extracted, manifestFile);
      writeRevision(articleCollection);
    } finally {
      for (File file : extracted.values()) {
        file.delete();
      }
    }
  }

  private RepoCollectionMetadata writeCollection(Map<String, File> files, File manifestFile) throws IOException, XmlContentException {
    ManifestXml manifestXml;
    try (InputStream manifestStream = new BufferedInputStream(new FileInputStream(manifestFile))) {
      manifestXml = new ManifestXml(parseXml(manifestStream));
    }
    ImmutableList<ManifestXml.Asset> assets = manifestXml.parse();

    ManifestXml.Asset manuscriptAsset = null;
    ManifestXml.Representation manuscriptRepr = null;
    for (ManifestXml.Asset asset : assets) {
      Optional<String> mainEntry = asset.getMainEntry();
      if (mainEntry.isPresent()) {
        for (ManifestXml.Representation representation : asset.getRepresentations()) {
          if (representation.getEntry().equals(mainEntry.get())) {
            manuscriptRepr = representation;
            break;
          }
        }
        manuscriptAsset = asset;
        break;
      }
    }
    if (manuscriptAsset == null || manuscriptRepr == null) {
      throw new IllegalArgumentException(); // TODO Better failure
    }

    File manuscriptFile = files.get(manuscriptRepr.getEntry());
    ArticleIdentity articleIdentity;
    Article articleMetadata;
    try (InputStream manuscriptStream = new BufferedInputStream(new FileInputStream(manuscriptFile))) {
      ArticleXml parsedArticle = new ArticleXml(parseXml(manuscriptStream));
      articleIdentity = parsedArticle.readDoi();
      articleMetadata = parsedArticle.build(new Article());
      articleMetadata.setDoi(articleIdentity.getKey()); // Should ArticleXml.build do this itself?
    }

    Collection<RepoObject> toUpload = new ArrayList<>(files.size());

    RepoObject manifestObject = new RepoObject.RepoObjectBuilder("manuscript/" + articleIdentity)
        .fileContent(manuscriptFile)
        .contentType(MediaType.APPLICATION_XML)
        .downloadName(articleIdentity.forXmlAsset().getFileName())
        .build();
    toUpload.add(manifestObject);

    for (ManifestXml.Asset asset : assets) {
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        File file = files.get(representation.getEntry());
        if (file.equals(manuscriptFile)) continue;
        String key = representation.getName() + "/" + AssetIdentity.create(asset.getUri());
        RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
            .fileContent(file)
                // TODO Add more metadata. Extract from articleMetadata and manifestXml as necessary.
            .build();
        toUpload.add(repoObject);
      }
    }

    // Post files
    Collection<RepoVersion> created = new ArrayList<>(toUpload.size());
    for (RepoObject repoObject : toUpload) { // Excellent candidate for parallelization! I can haz JDK8 plz?
      RepoObjectMetadata createdMetadata = versionedContentRepoService.autoCreateRepoObject(repoObject);
      created.add(createdMetadata.getVersion());
    }

    Object userMetadataForCollection = null; // TODO Implement

    // Create collection
    RepoCollection collection = RepoCollection.builder()
        .setKey(articleIdentity.toString())
        .setObjects(created)
            // TODO Set user metadata (needs lib support)
        .build();
    RepoCollectionMetadata collectionMetadata = versionedContentRepoService.autoCreateCollection(collection);

    return collectionMetadata;
  }

  private void writeRevision(RepoCollectionMetadata articleCollection) {
    RepoVersion collectionVersion = articleCollection.getVersion();
    // TODO Implement
  }

}
