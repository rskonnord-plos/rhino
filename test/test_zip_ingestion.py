#!/usr/bin/env python2
from Base.Validators import XMLValidator

__author__ = 'jkrzemien@plos.org'

"""
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.

Notes:

1. Uses Python's mogilelocal to check the local filesystem where Rhino **simulator** is running and saving its data.

"""

from Base.Api.Rhino.ZIPIngestion import ZIPIngestion
from Base.Validators.XMLValidator import XMLValidator
from Base.Database.HSQL import HSQL

DB_ARTICLE_STATE_ACTIVE = 0
DB_ARTICLE_STATE_UNPUBLISHED = 1
DB_ARTICLE_STATE_DISABLED = 2


class ZipIngestionTest(ZIPIngestion):
  """
  Attempting to test as much values as possible without hard coding them
  Ideally, test should:
    * Tests data inserted in DB
    * Tests files properly stored on MogileFS
  """

  def test_zip_ingestion_happy_path(self):
    """
    POST zips: Forced ingestion of ZIP archive
    """
    # Invoke ZIP API
    self.zipUpload('pone.0097823.zip', 'forced')

    # Validate HTTP code in the response is 201 (CREATED)
    self.verify_http_code_is(201)

    # Validate that *state* node in the response is **ingested**
    self.verify_state_is('ingested')

    # Validate that *all* nodes in response dealing with DOI text match the provided one
    self.verify_doi_is_correct()

    # Validate that *articleXml* node in response contains all and correct information
    self.verify_article_xml_section()

    # Validate that *articlePdf* node in response contains all and correct information
    self.verify_article_pdf_section()

    # Validate that **each** *graphics* node in response are complete and have correct information
    self.verify_graphics_section()

    # Validate that **each** *figures* node in response are complete and have correct information
    self.verify_figures_section()

    # === Database validations section ===
    articlesInDB = HSQL().query("select doi, format, state from PUBLIC.ARTICLE where ARCHIVENAME = 'pone.0097823.zip'")

    # We should have only one row in the database matching the SELECT criteria
    assert len(articlesInDB) is 1

    article = articlesInDB[0]

    # Verify uploaded DOI against the one stored in DB
    assert article[0] == self._zip.get_full_doi(), 'DOIs do not match %s!' % self._zip.get_full_doi()

    # Verify uploaded FORMAT against the one stored in DB
    assert article[1] == 'text/xml', 'Article format did not match text/xml'

    # Verify STATE stored in DB is STATE_UNPUBLISHED
    assert article[2] == DB_ARTICLE_STATE_UNPUBLISHED, 'Article state did not match %s' % DB_ARTICLE_STATE_UNPUBLISHED

    # === MogileFS validations section ===

    fsid = self.doi_to_fsid(self._zip.get_full_doi(), 'xml')
    mogileStoredXML = self.get_mogile_client().get_file_data(fsid)

    assert mogileStoredXML is not None, 'XML file for provided DOI was not found in Mogile storage!'

    self.assertEqual(XMLValidator(mogileStoredXML).get_xml(), self.get_processed_zip_file().get_xml_validator().get_xml())


if __name__ == '__main__':
  ZIPIngestion._run_tests_randomly()
