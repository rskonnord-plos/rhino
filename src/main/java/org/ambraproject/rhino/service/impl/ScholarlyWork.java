package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

class ScholarlyWork {

  private final DoiBasedIdentity doi;
  private final ImmutableMap<String, RepoObject> objects;
  private final String type;

  ScholarlyWork(DoiBasedIdentity doi, Map<String, RepoObject> objects) {
    this.doi = Objects.requireNonNull(doi);
    this.objects = ImmutableMap.copyOf(objects);
    this.type = ""; // TODO!
  }

  public DoiBasedIdentity getDoi() {
    return doi;
  }

  public RepoCollectionList persistToCrepo(ContentRepoService contentRepoService) {
    Map<String, RepoObjectMetadata> createdObjects = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : objects.entrySet()) {
      RepoObjectMetadata createdObject = contentRepoService.autoCreateRepoObject(entry.getValue());
      createdObjects.put(entry.getKey(), createdObject);
    }

    Map<String, RepoVersion> versionMap = Maps.transformValues(createdObjects, RepoObjectMetadata::getVersion);
    String collectionMetadata = new Gson().toJson(versionMap); // TODO: Use Gson bean

    RepoCollection repoCollection = RepoCollection.builder()
        .setObjects(versionMap.values())
        .setUserMetadata(collectionMetadata)
        .setKey(getCrepoKey())
        .build();

    return contentRepoService.autoCreateCollection(repoCollection);
  }

  private String getCrepoKey() {
    return type + "/" + doi.getIdentifier();
  }

  public void relate(ScholarlyWork work) {
    // TODO: Implement
  }
}
