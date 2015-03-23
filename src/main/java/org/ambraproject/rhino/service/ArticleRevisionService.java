package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.plos.crepo.clientlib.model.RepoCollection;
import org.plos.crepo.clientlib.model.RepoCollectionMetadata;
import org.plos.crepo.clientlib.model.RepoObject;
import org.plos.crepo.clientlib.model.RepoObjectMetadata;
import org.plos.crepo.clientlib.model.RepoVersion;
import org.plos.crepo.clientlib.service.ContentRepoService;
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArticleRevisionService extends AmbraService {

  @Autowired
  private ContentRepoService versionedContentRepoService;
  @Autowired
  private Gson crepoGson;

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

    Map<String, RepoObject> toUpload = new LinkedHashMap<>(); // keys are zip entry names

    RepoObject manifestObject = new RepoObject.RepoObjectBuilder("manuscript/" + articleIdentity)
        .fileContent(manuscriptFile)
        .contentType(MediaType.APPLICATION_XML)
        .downloadName(articleIdentity.forXmlAsset().getFileName())
        .build();
    toUpload.put(manuscriptRepr.getEntry(), manifestObject);

    for (ManifestXml.Asset asset : assets) {
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        String entry = representation.getEntry();
        File file = files.get(entry);
        if (file.equals(manuscriptFile)) continue;
        String key = representation.getName() + "/" + AssetIdentity.create(asset.getUri());
        RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
            .fileContent(file)
                // TODO Add more metadata. Extract from articleMetadata and manifestXml as necessary.
            .build();
        toUpload.put(entry, repoObject);
      }
    }

    // Post files
    Map<String, RepoVersion> created = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : toUpload.entrySet()) { // Excellent candidate for parallelization! I can haz JDK8 plz?
      RepoObject repoObject = entry.getValue();
      RepoObjectMetadata createdMetadata = versionedContentRepoService.autoCreateRepoObject(repoObject);
      created.put(entry.getKey(), createdMetadata.getVersion());
    }

    Map<String, Object> userMetadataForCollection = buildArticleAsUserMetadata(manifestXml, created);

    // Create collection
    RepoCollection collection = RepoCollection.builder()
        .setKey(articleIdentity.toString())
        .setObjects(created.values())
        .setUserMetadata(crepoGson.toJson(userMetadataForCollection))
        .build();
    RepoCollectionMetadata collectionMetadata = versionedContentRepoService.autoCreateCollection(collection);

    return collectionMetadata;
  }

  private Map<String, Object> buildArticleAsUserMetadata(ManifestXml manifestXml, Map<String, RepoVersion> objects) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("format", "nlm");

    String manuscriptKey = manifestXml.getArticleXml();
    map.put("manuscript", new RepoVersionRepr(objects.get(manuscriptKey)));

    List<ManifestXml.Asset> assetSpec = manifestXml.parse();
    List<Map<String,Object>> assetList = new ArrayList<>(assetSpec.size());
    for (ManifestXml.Asset asset : assetSpec) {
      Map<String,Object> assetMetadata=new LinkedHashMap<>();
      assetMetadata.put("doi",AssetIdentity.create(asset.getUri()).toString());

      Map<String,Object> assetObjects=new LinkedHashMap<>();
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        RepoVersion objectForRepr = objects.get(representation.getEntry());
        assetObjects.put(representation.getName(), new RepoVersionRepr(objectForRepr));
      }
      assetMetadata.put("objects", assetObjects);

      assetList.add(assetMetadata);
    }
    map.put("assets", assetList);

    return map;
  }

  private static class RepoVersionRepr {
    private final String key;
    private final String uuid;

    private RepoVersionRepr(RepoVersion repoVersion) {
      this.key = repoVersion.getKey();
      this.uuid = repoVersion.getUuid().toString();
    }
  }

  private void writeRevision(RepoCollectionMetadata articleCollection) {
    RepoVersion collectionVersion = articleCollection.getVersion();
    // TODO Implement
  }

}
