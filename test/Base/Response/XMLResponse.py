#!/usr/bin/env python2

"""
  Base class for Rhino's XML based service tests.
  Currently, there is *no* API that returns an XML as the API *response*.
  There are APIs that return an actual XML file that was previously ingested.
  Example:
  http://one-fluffy.plosjournals.org/api/assetfiles/10.1371/journal.pone.0097823.xml
"""

__author__ = 'jkrzemien@plos.org'

from bs4 import BeautifulSoup

from AbstractResponse import AbstractResponse


class XMLResponse(AbstractResponse):

  _xml = None

  def __init__(self, response):
    try:
      self._xml = BeautifulSoup(response.encode("UTF-8"), 'xml')
    except Exception as e:
      print 'Error while trying to parse response as XML!'
      print 'Actual response was: "%s"' % response
      raise e

  def get_xml(self):
    return self._xml

  """
  The following *find* expressions were **made up**.
  There is **no** actual XML API response in Rhino yet, so I can't know the names of the XML fields.
  """

  def get_doi(self):
    return self.get_xml().find_all(attrs={"pub-id-type": "doi"})

  def get_article_xml_section(self):
    return self.get_xml().find('articleXML')

  def get_article_pdf_section(self):
    return self.get_xml().find('articlePDF')

  def get_graphics_section(self):
    return self.get_xml().find('graphics')

  def get_figures_section(self):
    return self.get_xml().find('figures')

  def get_syndications_section(self):
    return self.get_xml().find('syndications')

  def get_state(self):
    return self.get_xml().find('state')