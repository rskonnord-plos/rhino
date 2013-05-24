package org.ambraproject.rhino.rest.controller;

import com.google.common.io.Closeables;
import com.google.common.io.Closer;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class JournalCrudController extends RestController {

  private static final String JOURNAL_ROOT = "/journals";
  private static final String JOURNAL_TEMPLATE = JOURNAL_ROOT + "/{journalKey}";
  private static final String FRONT_END_PARAMETER = "fend";

  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private VolumeCrudService volumeCrudService;

  @RequestMapping(value = JOURNAL_ROOT, method = RequestMethod.GET)
  public void listJournals(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    journalCrudService.listJournals(receiver, mf);
  }

  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable String journalKey,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    journalCrudService.read(receiver, journalKey, mf);
  }

  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<String> createVolume(HttpServletRequest request, @PathVariable String journalKey)
      throws IOException {
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    if (StringUtils.isBlank(input.getVolumeUri())) {
      throw new RestClientException("volumeUri required", HttpStatus.BAD_REQUEST);
    }

    DoiBasedIdentity volumeId = volumeCrudService.create(journalKey, input);
    return reportCreated(volumeId.getIdentifier());
  }

  @RequestMapping(value = JOURNAL_TEMPLATE, params = {FRONT_END_PARAMETER}, method = RequestMethod.PUT)
  public void writeFrontEndBundle(HttpServletRequest request, @PathVariable String journalKey) throws IOException {
    InputStream stream = null;
    boolean threw = true;
    try {
      stream = request.getInputStream();
      journalCrudService.writeFrontEndBundle(journalKey, stream);
    } finally {
      Closeables.close(stream, threw);
    }
  }

  @RequestMapping(value = JOURNAL_TEMPLATE, params = {FRONT_END_PARAMETER}, method = RequestMethod.GET)
  public void readFrontEndBundle(HttpServletResponse response, @PathVariable String journalKey) throws IOException {
    Closer closer = Closer.create();
    try {
      ServletOutputStream responseStream = closer.register(response.getOutputStream());
      InputStream bundleStream = closer.register(journalCrudService.readFrontEndBundle(journalKey));
      IOUtils.copy(bundleStream, responseStream);
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
  }

}
