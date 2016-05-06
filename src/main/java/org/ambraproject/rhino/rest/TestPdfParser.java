/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.rest;

import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;

/**
 * Controller class intended to simulate the response from Crossref upon article syndication; useful for manual testing.
 * This controller processes POSTS to /crossref with a parameter, fname, set to the file being syndicated.
 * <p/>
 * This is intended to be called from plos-queue, which is in turn called from rhino upon article publication.  The
 * property plos.crossref.url in /etc/topaz/camel.properties should point to this server appopriately.
 * <p/>
 * The posted files will be stored in the directory specified by ambra.services.syndications.testDir in ambra.xml for
 * manual examination.
 */
@Controller
public class TestPdfParser extends RestController {

  //private static final Logger log = LoggerFactory.getLogger(TestPdfParser.class);

  @ResponseBody
  @RequestMapping(value = "/pdf", method = RequestMethod.GET)
  public String extractPdfText(HttpServletResponse response)
      throws Exception {

    String filePath = "/home/jfesenko/Documents/rhino/em_sample.pdf";

    PDFTextStripper pdfStripper = new PDFTextStripper();
    PDDocument pdDoc = PDDocument.load(new File(filePath));
    return pdfStripper.getText(pdDoc);
  }
}
