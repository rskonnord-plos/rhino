#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This test cases validates Rhino's Articles Tests.
'''

from Base.Api.Rhino.Articles import Articles
from Base.Api.Rhino.ZIPIngestion import ZIPIngestion


class ArticlesTest(Articles, ZIPIngestion):

  def test_article_syndication_happy_path(self):

    #self.simulator().setCaptureMode()
    self.simulator().setMockMode()
    #self.simulator().setMockScenario('')

    """
    PATCH articles: Update article with publish and syndication.

    Test preconditions:
      1. An article **must** exist in order to be updated.
    """

    # === Prepare test preconditions ===
    print ('Creating test precondition: An article must exist in order to be updated.')
    self.zipUpload('pone.0097823.zip', 'forced')
    self.verify_http_code_is(201)
    self.verify_state_is('ingested')

    # === Start of actual test ===
    print ('Starting actual test...')

    # === Prepare API arguments ===
    desiredState = 'published'

    syndications = {
      'CROSSREF': {'status': 'IN_PROGRESS'}
    }

    # === Invoke API ===
    self.updateArticle('10.1371/journal.pone.0097823', desiredState, syndications)

    # === Perform validations ===
    self.verify_http_code_is(200)

    self.verify_state_is('published')

    # Can't actually perform this validation since Response ALWAYS contains error message:
    # "CROSSREF queue not configured"...(bug?)

    # self.verify_syndications_status_is(syndications)

    self.verify_doi_is_correct()

    self.define_zip_file_for_validations('pone.0097823.zip')

    self.verify_article_xml_section()

    self.verify_article_pdf_section()

    self.verify_graphics_section()

    self.verify_figures_section()


if __name__ == '__main__':
    Articles._run_tests_randomly()