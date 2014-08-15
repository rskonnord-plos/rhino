#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'


"""
"""

from Base.Api.Rhino.ZIPIngestion import ZIPIngestion
from Base.Api.Rhino.AssetFiles import AssetFiles


class IngestiblesTest(AssetFiles, ZIPIngestion):

  """
  Test suite for AssetFiles namespace in Rhino's API
  """

  def test_retrieve_ingested_xml_file_happy_path(self):
    """
    GET assetfiles: Retrieve XML article file from a ZIP ingestion

    Test preconditions:
      1. An article **must** exist in order to be updated.
    """

    # Prepare test preconditions
    print ('Creating test precondition: An article must exist in order to be updated.')
    self.zipUpload('pone.0097823.zip', 'forced')
    self.verify_http_code_is(201)

    # Start of actual test
    print ('Starting actual test...')

    # Prepare API arguments
    doi = '10.1371'
    journal = 'journal.pone.0097823'

    # Invoke API
    self.get_asset_for(doi, journal, 'xml')
    self.verify_http_code_is(200)

    # Validations section
    assert self.parsed is not None, 'XML was not parsed, it is NULL'
    doi_nodes = self.parsed.get_doi()
    assert doi_nodes is not None, 'XML did not contain any DOI nodes'
    for node in doi_nodes:
      expected = '%s/%s' % (doi, journal)
      assert node.getText().startswith(expected) is True, 'Expected DOI is %s, but %s found' % (expected, node.getText())


if __name__ == '__main__':
    AssetFiles._run_tests_randomly()




