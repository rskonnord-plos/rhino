package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.OptionalInt;

@Controller
public class IngestibleZipController extends RestController {

  private static final String ZIP_ROOT = "/zips";

  @Autowired
  private ArticleCrudService articleCrudService;

  /**
   * Create an article based on a POST containing an article .zip archive file.
   * <p/>
   * TODO: this method may never be used in production, since we've decided, at least for now, that we will use the
   * ingest and ingested directories that the current admin app uses instead of posting zips directly.
   *
   * @param response      response to the request
   * @param requestFile   body of the archive param, with the encoded article .zip file
   * @param forceReingest if present, re-ingestion of an existing article is allowed; otherwise, if the article already
   *                      exists, it is an error
   * @throws java.io.IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ZIP_ROOT, method = RequestMethod.POST)
  public void zipUpload(HttpServletRequest request, HttpServletResponse response,
                        @RequestParam("archive") MultipartFile requestFile,
                        @RequestParam(value = "force_reingest", required = false) String forceReingest,
                        @RequestParam(value = "revision", required = false) Integer revision)
      throws IOException {

    String archiveName = requestFile.getOriginalFilename();
    ArticleMetadata result;
    try (InputStream requestInputStream = requestFile.getInputStream();
         Archive archive = Archive.readZipFile(archiveName, requestInputStream)) {
      result = articleCrudService.writeArchive(archive,
          Optional.empty(),

          // If forceReingest is the empty string, the parameter was present.  Only
          // treat null as false.
          forceReingest == null ? DoiBasedCrudService.WriteMode.CREATE_ONLY : DoiBasedCrudService.WriteMode.WRITE_ANY,

          (revision != null) ? OptionalInt.of(revision) : OptionalInt.empty());
    }
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(result.getDoi(), revision);
    articleCrudService.readArticleMetadata(ingestionId).respond(request, response, entityGson);
  }

}
