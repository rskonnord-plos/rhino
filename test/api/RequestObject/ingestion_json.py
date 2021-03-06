#!/usr/bin/env python2

# Copyright (c) 2017 Public Library of Science
#
# Permission is hereby granted, free of charge, to any person obtaining a
# copy of this software and associated documentation files (the "Software"),
# to deal in the Software without restriction, including without limitation
# the rights to use, copy, modify, merge, publish, distribute, sublicense,
# and/or sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

"""
Base class for Rhino's Ingest API service tests.
"""
from test.api import resources

__author__ = 'jkrzemien@plos.org; gfilomeno@plos.org'

from ...Base.base_service_test import BaseServiceTest
from ...Base.api import needs
from ...Base.MySQL import MySQL
from ..resources import *

class Ingestion(BaseServiceTest):

  @needs('parsed', 'parse_response_as_json()')
  def verify_article(self):
    """
    Validate ingestion with Article table
    """
    article_id = self.get_article_id_sql_doi(resources.NOT_SCAPE_ARTICLE_DOI)
    article = self.get_article_sql_archiveName(article_id)
    # # Verify uploaded DOI against the one stored in DB
    # self.verify_ingestion_text_expected_only(article[0], 'doi')
    # # Verify uploaded FORMAT against the one stored in DB
    # self.verify_ingestion_text_expected_only(article[1], 'format')
    # # Verify STATE stored in DB is STATE_UNPUBLISHED
    # self.verify_ingestion_text_expected_only(self.get_article_status(article[2]), 'state')
    # # Verify PAGE COUNT stored in DB
    self.verify_ingestion_text_expected_only(article[0], 'title')
    # self.verify_ingestion_text_expected_only(article[4], 'pages')
    # self.verify_ingestion_text_expected_only(article[5], 'eIssn')
    # #self.verify_ingestion_text_expected_only(article[6], 'description')
    # self.verify_ingestion_text_expected_only(article[7], 'rights')
    # self.verify_ingestion_text_expected_only(article[8], 'language')
    # self.verify_ingestion_text_expected_only(article[9], 'format')
    # self.verify_ingestion_text_expected_only(article[10], 'eLocationId')
    # self.verify_ingestion_text_expected_only(article[11], 'strkImgURI')
    # self.verify_ingestion_text_expected_only(article[12], 'volume')
    # self.verify_ingestion_text_expected_only(article[13], 'issue')
    # self.verify_ingestion_text_expected_only(article[14], 'journal')
    # self.verify_ingestion_text_expected_only(article[15], 'publisherLocation')
    # self.verify_ingestion_text_expected_only(article[16], 'publisherName')
    # self.verify_ingestion_text_expected_only(article[17], 'url')

  def verify_syndications(self, zip_article):
    """
    Validate ingestion with Syndication table
    """
    print 'Verify Syndications'
    syndications = self.get_syndications_sql_archiveName(zip_article)
    syndications_json = self.parsed.get_attribute('syndications')
    self.verify_array(syndications, syndications_json)
    for syndication in syndications:
        target = str(syndication[1])
        self.verify_ingestion_text(syndications_json[target]['doi'], syndication[0], target + '.doi')
        self.verify_ingestion_text(syndications_json[target]['target'], syndication[1], target + '.target')
        self.verify_ingestion_text(syndications_json[target]['status'], syndication[2], target + '.status')
        self.verify_ingestion_attribute(syndications_json[target]['submissionCount'], syndication[3], target + '.submissionCount')

  @needs('parsed', 'parse_response_as_json()')
  def verify_journals(self):
    """
    Validate ingestion with articlePublishedJournals  table
    """
    print 'Verify Journals'
    article_id = self.get_article_id_sql_doi(resources.NOT_SCAPE_ARTICLE_DOI)
    journals = self.get_journals_sql_archiveName(article_id)
    journals_json = self.parsed.get_attribute('journal')
    self.verify_array(journals, journals_json)
    for journal in journals:
      journal_name = str(journal[0])
      self.verify_ingestion_text(journals_json['journalKey'], journal[0], journal_name + '.journalKey')
      self.verify_ingestion_text(journals_json['eIssn'], journal[1], journal_name + '.eIssn')
      self.verify_ingestion_text(journals_json['title'], journal[2], journal_name + '.title')

  def verify_citedArticles(self, zip_article):
    """
    Validate ingestion with CitedArticles  table
    """
    print 'Verify CitedArticles and CitedPersons'
    citedArticles = self.get_citedArticles_sql_archiveName(zip_article)
    citedArticles_json = self.parsed.get_attribute('citedArticles')
    self.verify_array(citedArticles, citedArticles_json)
    i = 0
    for citedArticle in citedArticles:
      self.verify_ingestion_text(citedArticles_json[i]['key'], citedArticle[1], 'key')
      if citedArticles_json[i].has_key('year'):
        self.verify_ingestion_attribute(citedArticles_json[i]['year'], citedArticle[2], 'year')
      if citedArticles_json[i].has_key('displayYear'):
        self.verify_ingestion_text(citedArticles_json[i]['displayYear'], citedArticle[3], 'displayYear')
      if citedArticles_json[i].has_key('volumeNumber'):
        self.verify_ingestion_attribute(citedArticles_json[i]['volumeNumber'], citedArticle[4], 'volumeNumber')
      if citedArticles_json[i].has_key('volume'):
        self.verify_ingestion_text(citedArticles_json[i]['volume'], citedArticle[5], 'volume')
      if citedArticles_json[i].has_key('title'):
        self.verify_ingestion_text(citedArticles_json[i]['title'], citedArticle[6], 'title')
      if citedArticles_json[i].has_key('pages'):
        self.verify_ingestion_text(citedArticles_json[i]['pages'], citedArticle[7], 'pages')
      if citedArticles_json[i].has_key('eLocationID'):
        self.verify_ingestion_text(citedArticles_json[i]['eLocationID'], citedArticle[8], 'eLocationID')
      if citedArticles_json[i].has_key('journal'):
        self.verify_ingestion_text(citedArticles_json[i]['journal'], citedArticle[9], 'journal')
      if citedArticles_json[i].has_key('citationType'):
        self.verify_ingestion_text(citedArticles_json[i]['citationType'], citedArticle[10], 'citationType')
      if citedArticles_json[i].has_key('authors'):
        self.verify_citedPersons(citedArticle[0], citedArticles_json[i]['authors'])
      i = i + 1

  def verify_citedPersons(self, citedArticleID, citedPersons_json):
    """
    Validate ingestion with CitedPersons table
    """
    citedPersons = self.get_citedPerson_sql_citedArticleID(citedArticleID)
    self.verify_array(citedPersons, citedPersons_json)
    i = 0
    for citedPerson in citedPersons:
      self.verify_ingestion_text(citedPersons_json[i]['fullName'], citedPerson[0], 'fullName')
      self.verify_ingestion_text(citedPersons_json[i]['givenNames'], citedPerson[1], 'givenNames')
      self.verify_ingestion_text(citedPersons_json[i]['surnames'], citedPerson[2], 'surnames')
      self.verify_ingestion_text(citedPersons_json[i]['suffix'], citedPerson[3], 'suffix')
      i = i + 1

  @needs('parsed', 'parse_response_as_json()')
  def verify_article_file(self, article_id, content_type, file_type):
    """
    Validate ingestion's files with Assert table
    """
    print 'Verify Article\'s files ' + file_type
    asset_file = self.get_asset_file(content_type, article_id)
    print (asset_file)
    asset_file_json = self.parsed.get_attribute(file_type)
    print (asset_file_json)
    if asset_file and asset_file_json:
      self.verify_ingestion_text(asset_file_json['file'], asset_file[0], 'file')
      self.verify_ingestion_text(asset_file_json['metadata']['doi'], asset_file[1], 'doi')
      self.verify_ingestion_text(asset_file_json['metadata']['extension'], asset_file[2], 'extension')
      self.verify_ingestion_text(asset_file_json['metadata']['contentType'], asset_file[3], 'contentType')
      self.verify_ingestion_attribute(asset_file_json['metadata']['size'], asset_file[4], 'size')

  def verify_article_figures(self, zip_article):
    """
    Validate ingestion's figures with Assert table
    """
    print 'Verify Article\'s figures'
    self.verify_article_assets(zip_article, 'fig', 'figures')

  def verify_article_graphics(self, article_id):
    """
    Validate ingestion's graphics with Assert table
    """
    print 'Verify Article\'s graphics'
    self.verify_article_assets(article_id,'inline-formula, disp-formula', 'graphics')

  def verify_article_assets(self, article_id, assets_type, assets_json_name):
    assets = self.get_asset_figures_graphics(assets_type, article_id)
    assets_json = self.parsed.get_attribute(assets_json_name)
    i = 0
    for asset in assets:
      self.verify_ingestion_text(assets_json[i]['doi'],asset[0], 'doi')
      self.verify_ingestion_text(assets_json[i]['title'],asset[1], 'title')
      self.verify_ingestion_text(assets_json[i]['description'],asset[2], 'description')
      self.verify_ingestion_text(assets_json[i]['contextElement'],asset[3], 'contextElement')
      # Get the metadata according with the image type
      if 'original' == asset[8]:
        metadata_json = assets_json[i]['original']
        i = i + 1
      else:
        metadata_json = assets_json[i]['thumbnails'][asset[8]]
      self.verify_ingestion_text(metadata_json['file'],asset[4], 'file')
      self.verify_ingestion_text(metadata_json['metadata']['extension'],asset[5], 'extension')
      self.verify_ingestion_text(metadata_json['metadata']['contentType'],asset[6], 'contentType')
      self.verify_ingestion_attribute(metadata_json['metadata']['size'],asset[7], 'size')

  @needs('parsed', 'parse_response_as_json()')
  def verify_ingestion_text_expected_only(self, expected_results, attribute):
    actual_results = self.parsed.get_attribute(attribute)
    assert actual_results.encode('utf-8') == expected_results, \
      ('%s is not correct! actual: %s expected: %s' % (attribute, actual_results, expected_results))

  @needs('parsed', 'parse_response_as_json()')
  def verify_ingestion_text(self, actual_results, expected_results, attribute):
    assert actual_results.encode('utf-8') == expected_results, \
      ('%s is not correct! actual: %s expected: %s' % (attribute, actual_results, expected_results))

  @needs('parsed', 'parse_response_as_json()')
  def verify_ingestion_attribute(self, actual_results, expected_results, attribute):
    assert actual_results == expected_results, \
      ('%s is not correct! actual: %s expected: %s' % (attribute, actual_results, expected_results))

  def verify_array(self, actual_array, expected_array):
    self.assertIsNotNone(actual_array)
    self.assertIsNotNone(expected_array)
    #self.assertEquals(len(actual_array), len(expected_array), 'The arrays have a different size')

  """
  Below SQL statements will get article id from ambra db given doi  example 10.1371/journal.pone.0155391
  """
  def get_article_id_sql_doi (self,not_scape_doi):
    current_articles_id = MySQL().query('SELECT articleId FROM article WHERE doi = %s', [not_scape_doi])
    return current_articles_id[0][0]

  """
  Below SQL statements will delete article from ambra db given doi  example 10.1371/journal.pone.0155391
  """
  def delete_article_sql_doi (self,not_scape_doi):
    current_articles_id = self.get_article_id_sql_doi (not_scape_doi)
    MySQL().modify('CALL migrate_article_rollback(%s)', [current_articles_id])
    return self


  """
  Below SQL statements will query ambra articleIngestion table given articleId
  """
  def get_article_sql_archiveName (self,article_id):
    articles = MySQL().query('SELECT title FROM articleIngestion WHERE articleId = %s', [article_id])

    return articles[0]

  """
  Below SQL statements will query ambra syndication table given archiveName
  """
  def get_syndications_sql_archiveName (self,archive_name):
    syndications = MySQL().query('SELECT s.doi, s.target, s.status, s.submissionCount, s.created, s.lastModified '
                                  'FROM syndication as s JOIN oldArticle as a ON s.doi = a.doi '
                                  'WHERE a.archiveName = %s '
                                  'ORDER BY syndicationID', [archive_name])
    return syndications

  """
  Below SQL statements will query ambra journal table given archiveName
  """
  def get_journals_sql_archiveName(self, article_id):
    journals = MySQL().query('SELECT j.journalKey, j.eIssn, j.title '
                              'FROM journal AS j JOIN articleIngestion aj ON  j.journalID = aj.journalID '
                              'JOIN articleIngestion as a ON aj.articleID = a.articleID '
                              'WHERE a.articleid = %s ORDER BY j.journalID', [article_id])
    return journals

  def get_citedArticles_sql_archiveName(self,  article_id):
    citedArticles = MySQL().query('SELECT ca.citedArticleID, ca.keyColumn, ca.year, ca.displayYear, ca.volumeNumber, '
                                    'ca.volume, ca.title, ca.pages, ca.eLocationId, ca.journal, ca.citationType '
                                    'FROM citedArticle ca JOIN article a ON ca.articleID = a.articleID '
                                    'WHERE a.articleid = %s ORDER BY ca.sortOrder', [article_id])
    return citedArticles

  def get_citedPerson_sql_citedArticleID(self, citedArticleID):
    citedPersons = MySQL().query('SELECT fullName, givenNames, surnames, suffix '
                                  'FROM citedPerson WHERE citedArticleID = %s '
                                  'ORDER BY sortOrder', [citedArticleID])
    return citedPersons

  def get_asset_file(self, content_type, article_id):
    asset_file = MySQL().query('SELECT CONCAT(SUBSTRING_INDEX(aa.doi, \'/\', -2),\'.\',aa.extension) as file , '
                               'aa.doi, aa.extension, aa.contentType, aa.size '
                               'FROM articleAsset aa JOIN article a ON aa.articleID = a.articleID '
                               'WHERE aa.contentType = %s  AND a.articleId = %s', [content_type, article_id])
    return asset_file[0]

  def get_asset_figures_graphics(self, context_element, article_id):
    assets = MySQL().query('SELECT aa.doi, aa.title, aa.description, aa.contextElement, '
                              'CONCAT(SUBSTRING_INDEX(aa.doi, \'/\', -2),\'.\', aa.extension) as file, aa.extension, '
                              'aa.contentType, aa.size, '
                              'CASE aa.extension '
                              'WHEN \'PNG_I\' THEN \'inline\' '
                              'WHEN \'PNG_S\' THEN \'small\' '
                              'WHEN \'PNG_M\' THEN \'medium\' '
                              'WHEN \'PNG_L\' THEN \'large\' '
                              'WHEN \'PNG\' THEN \'graphic\' '
                              'ELSE \'original\' END as figureType '
                            'FROM articleAsset aa JOIN article a ON aa.articleID = a.articleID '
                            'WHERE aa.contextElement IN ( %s )'
                            'AND a.articleId = %s '
                            'ORDER BY aa.doi',[context_element, article_id])
    return assets


  def get_article_status(self, status):
     return {
        1: resources.INGESTED
     }[status]
