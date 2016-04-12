package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService.ArticleMetadataSource;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class VersionedIngestionService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  VersionedIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
  }

  static class IngestionResult {
    private final Article article;
    private final RepoCollectionList collection;

    public IngestionResult(Article article, RepoCollectionList collection) {
      this.article = Preconditions.checkNotNull(article);
      this.collection = Preconditions.checkNotNull(collection);
    }

    public Article getArticle() {
      return article;
    }

    public RepoCollectionList getCollection() {
      return collection;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IngestionResult that = (IngestionResult) o;
      return article.equals(that.article) && collection.equals(that.collection);
    }

    @Override
    public int hashCode() {
      return 31 * article.hashCode() + collection.hashCode();
    }
  }

  /**
   * Identifies the model that we are using for representing articles as CRepo collections. To be stored in the
   * collection's {@code userMetadata} field under the "schema" key.
   * <p>
   * Currently, this is not consumed anywhere. The "schema" field is future-proofing against changes to the model that
   * would require backfilling or special handling. The "format" value can be used to disambiguate different models or
   * different inputs. ("ambra-nlm" means that the article is an XML file under the NLM DTD, packaged as a zip file with
   * manifest as input for Ambra.) The "version" value can be incremented to reflect changes in ingestion logic that
   * break backwards compatibility.
   */
  private static final ImmutableMap<String, Object> SCHEMA_REPR = ImmutableMap.<String, Object>builder()
      .put("format", "ambra-nlm")
      .put("version", 1)
      .build();

  Article ingest(Archive archive) throws IOException, XmlContentException {
    String manifestEntry = null;
    for (String entryName : archive.getEntryNames()) {
      if (entryName.equalsIgnoreCase("manifest.xml")) {
        manifestEntry = entryName;
      }
    }
    if (manifestEntry == null) {
      throw new RestClientException("Archive has no manifest file", HttpStatus.BAD_REQUEST);
    }

    ManifestXml manifestXml;
    try (InputStream manifestStream = new BufferedInputStream(archive.openFile(manifestEntry))) {
      manifestXml = new ManifestXml(AmbraService.parseXml(manifestStream));
    }
    ImmutableList<ManifestXml.Asset> assets = manifestXml.parse();

    ManifestXml.Asset manuscriptAsset = findManuscriptAsset(assets);
    ManifestXml.Representation manuscriptRepr = findManuscriptRepr(manuscriptAsset);

    String manuscriptEntry = manuscriptRepr.getEntry();
    if (!archive.getEntryNames().contains(manifestEntry)) {
      throw new RestClientException("Manifest refers to missing file as main-entry: " + manuscriptEntry, HttpStatus.BAD_REQUEST);
    }

    ArticleXml parsedArticle;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
    }
    ArticleIdentity articleIdentity = parsedArticle.readDoi();
    if (!manuscriptAsset.getUri().equals(articleIdentity.getKey())) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAsset.getUri(), articleIdentity.getKey());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    final Article articleMetadata = parsedArticle.build(new Article());

    ArticlePackage articlePackage = new ArticlePackageBuilder(archive, parsedArticle, manifestXml, manifestEntry, manuscriptEntry).build();
    articlePackage.persist(parentService.hibernateTemplate, parentService.contentRepoService);

    return articleMetadata;
  }

  private ManifestXml.Asset findManuscriptAsset(List<ManifestXml.Asset> assets) {
    for (ManifestXml.Asset asset : assets) {
      if (asset.getMainEntry().isPresent()) {
        return asset;
      }
    }
    throw new RestClientException("main-entry not found", HttpStatus.BAD_REQUEST);
  }

  private ManifestXml.Representation findManuscriptRepr(ManifestXml.Asset manuscriptAsset) {
    Optional<String> mainEntry = manuscriptAsset.getMainEntry();
    Preconditions.checkArgument(mainEntry.isPresent(), "manuscriptAsset must have main-entry");
    for (ManifestXml.Representation representation : manuscriptAsset.getRepresentations()) {
      if (representation.getEntry().equals(mainEntry.get())) {
        return representation;
      }
    }
    throw new RestClientException("main-entry not matched to asset", HttpStatus.BAD_REQUEST);
  }


  private static final String MANUSCRIPTS_KEY = "manuscripts";
  private static final ImmutableBiMap<ArticleMetadataSource, String> SOURCE_KEYS = ImmutableBiMap.<ArticleMetadataSource, String>builder()
      .put(ArticleMetadataSource.FULL_MANUSCRIPT, "full")
      .put(ArticleMetadataSource.FRONT_MATTER, "front")
      .put(ArticleMetadataSource.FRONT_AND_BACK_MATTER, "frontAndBack")
      .build();

  static {
    if (!SOURCE_KEYS.keySet().equals(EnumSet.allOf(ArticleMetadataSource.class))) {
      throw new AssertionError("ArticleMetadataSource values don't match key map");
    }
  }

  /**
   * Build a representation of an article's metadata from a persisted collection.
   * <p>
   * The legacy Hibernate model object {@link Article} is used as a data-holder for convenience and compatibility. This
   * method constructs it anew, not by accessing Hibnerate, and populates only a subset of its normal fields.
   *
   * @param id            the ID of the article to serve
   * @param versionNumber the number of the ingested version to read, or absent for the latest version
   * @param source        whether to parse the extracted front matter or the full, original manuscript
   * @return an object containing metadata that could be extracted from the manuscript, with other fields unfilled
   * @deprecated method signature accommodates testing and will be changed
   */
  @Deprecated
  Article getArticleMetadata(ArticleIdentity id, Optional<Integer> versionNumber, ArticleMetadataSource source) {
    /*
     * *** Implementation notes ***
     *
     * The method signature accommodates the methods `ArticleCrudController.previewMetadataFromVersionedModel` and
     * `ArticleCrudService.readVersionedMetadata`, which are temporary hacks to expose read-service functionality for
     * testing. This method's signature will probably change when those methods are removed, though we expect to keep
     * most of the business logic.
     *
     * The `source` argument ought to be replaced with something that doesn't expose so much detail. It gives the option
     * to parse the full manuscript (including the body) for validation purposes, which should not be possible in the
     * production implementation. We do currently plan to choose between the 'front' and 'frontAndBack' documents based
     * on whether we need to serve cited articles, but the signature should talk about whether to include citations, not
     * which file to read. In a future API version, we may wish to simplify further by splitting citations into a
     * separate service.
     *
     * Also, the means of specifying a collection version (the `id` and `versionNumber` parameters) might be replaced
     * with a better way of specifying an article version.
     *
     * TODO: Improve as described above and delete this comment block
     */

    String identifier = id.getIdentifier();
    RepoCollectionMetadata collection;
    if (versionNumber.isPresent()) {
      collection = parentService.contentRepoService.getCollection(new RepoVersionNumber(identifier, versionNumber.get()));
    } else {
      collection = parentService.contentRepoService.getLatestCollection(identifier);
    }

    Map<String, Object> userMetadata = (Map<String, Object>) collection.getJsonUserMetadata().get();
    Map<String, Object> manuscriptsMap = (Map<String, Object>) userMetadata.get(MANUSCRIPTS_KEY);
    Map<String, String> manuscriptId = (Map<String, String>) manuscriptsMap.get(SOURCE_KEYS.get(source));
    RepoVersion manuscript = RepoVersionRepr.read(manuscriptId);

    Document document;
    try (InputStream manuscriptStream = parentService.contentRepoService.getRepoObject(manuscript)) {
      DocumentBuilder documentBuilder = AmbraService.newDocumentBuilder();
      log.debug("In getArticleMetadata source={} documentBuilder.parse() called", source);
      document = documentBuilder.parse(manuscriptStream);
      log.debug("finish");
    } catch (IOException | SAXException e) {
      throw new RuntimeException(e);
    }

    Article article;
    try {
      article = new ArticleXml(document).build(new Article());
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
    article.setLastModified(collection.getTimestamp());
    return article;
  }

}
