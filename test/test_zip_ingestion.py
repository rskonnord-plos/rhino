#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'


"""
This test case validates Rhino's convenience zipUpload Tests for ZIP ingestion.

Notes:

* For Data-Driven Testing (DDT) you can use ddt, available via: pip install ddt

Decorate your test class with @ddt and @data for your test methods, you will also need to pass an
extra argument to the test method.

* Using Nose's parameterized feature is not recommended since Nose doesn't play nice
with subclasses.

* Still need to take a look @ https://code.google.com/p/mogilefs/wiki/Clients
for MogileFS's Python client implementations.
"""

from Base.Api.Rhino.Ingestion import ZIPIngestion
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
    # Parse response as a JSON object
    self.parse_response_as_json()
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

    """
    Database validations from here
    """
    articlesInDB = HSQL().query("select doi, format, state from PUBLIC.ARTICLE where ARCHIVENAME = 'pone.0097823.zip'")

    # We should have only one row in the database matching the SELECT criteria
    assert len(articlesInDB) is 1

    article = articlesInDB[0]

    # Verify uploaded DOI against the one stored in DB
    assert article[0] == self._zip.get_full_doi()

    # Verify uploaded FORMAT against the one stored in DB
    assert article[1] == 'text/xml', 'Article format did not match text/xml'

    # Verify STATE stored in DB is STATE_UNPUBLISHED
    assert article[2] == DB_ARTICLE_STATE_UNPUBLISHED, 'Article state did not match %s' % DB_ARTICLE_STATE_UNPUBLISHED


if __name__ == '__main__':
    ZIPIngestion._run_tests_randomly()





"""
Actual test against db should validate:

<hibernate-mapping package="org.ambraproject.models" default-lazy="false">

  <class name="Article" table="article">
    <id name="ID" column="articleID" type="long">
      <generator class="native"/>
    </id>

    <timestamp name="lastModified" column="lastModified"/>
    <property name="created" column="created" type="timestamp" not-null="true" update="false"/>

    <property name="doi" column="doi" type="string" not-null="true" unique="true"/>
    <property name="title" column="title" type="text"/>
    <property name="eIssn" column="eIssn" type="string"/>
    <property name="state" column="state" type="integer"/>
    <property name="archiveName" column="archiveName" type="string"/>
    <property name="description" column="description" type="text"/>
    <property name="rights" column="rights" type="text"/>
    <property name="language" column="language" type="string"/>
    <property name="format" column="format" type="string"/>
    <property name="pages" column="pages" type="string"/>
    <property name="eLocationId" column="eLocationId" type="string"/>
    <property name="url" column="url" type="string"/>
    <property name="strkImgURI" column="strkImgURI" type="string"/>

    <property name="date" column="date" type="timestamp"/>

    <property name="volume" column="volume" type="string"/>
    <property name="issue" column="issue" type="string"/>
    <property name="journal" column="journal" type="string"/>

    <property name="publisherLocation" column="publisherLocation" type="string"/>
    <property name="publisherName" column="publisherName" type="string"/>

    <set name="types" table="articleType" cascade="all-delete-orphan">
      <key column="articleID"/>
      <element column="type" type="string"/>
    </set>

    <list name="relatedArticles" cascade="all-delete-orphan">
      <key column="parentArticleID" not-null="true"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleRelationship"/>
    </list>

    <list name="assets" cascade="all-delete-orphan">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleAsset"/>
    </list>

    <!--Don't want to delete orphan on these-->
    <set name="categories" cascade="save-update" table="articleCategoryJoinTable">
      <key column="articleID"/>
      <many-to-many class="org.ambraproject.models.Category" column="categoryID"/>
    </set>

    <list name="citedArticles" cascade="all-delete-orphan" lazy="true">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="CitedArticle"/>
    </list>

    <list name="collaborativeAuthors" table="articleCollaborativeAuthors" cascade="all-delete-orphan">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <element column="name" type="string"/>
    </list>

    <list name="authors"
          cascade="all-delete-orphan"
          where="type = 'author'">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleAuthor"/>
    </list>

    <list name="editors"
          cascade="all-delete-orphan"
          where="type = 'editor'">
      <key column="articleID"/>
      <list-index column="sortOrder"/>
      <one-to-many class="ArticleEditor"/>
    </list>

    <set name="journals" table="articlePublishedJournals" cascade="none" lazy="true">
      <key column="articleID"/>
      <many-to-many class="Journal" column="journalID"/>
    </set>

  </class>

</hibernate-mapping>
"""
