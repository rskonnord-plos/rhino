#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class loads up the name and data for a TIF file from the ingested ZIP file, along with the XML file
in order to be used for comparison between data in ZIP and API's responses.
'''

from datetime import datetime

from AbstractValidator import AbstractValidator


class TIFValidator(AbstractValidator):

  def __init__(self, name, data, xml):
    super(TIFValidator, self).__init__(data)
    self._name = name
    self._xml = xml
    self.DOI_HEADER = 'info:doi/'
    self.DOI_PREFFIX = '10.1371/journal.'
    self.MIME = 'image/tiff'
    self.EXT = 'TIF'

  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating %s metadata section in Response...' % self.EXT,

    assert section is not None, 'Graphics section in response is NULL!'
    assert section['doi'] == doi, 'DOI field in Graphics section did not match %s!' % doi

    all_related_tags = self._xml.get_xml().find_all(section['contextElement'])
    matchedXmlFile = False
    for tag in all_related_tags:
      try:
        if tag.contents[0].attrs['xlink:href'] == doi:
          matchedXmlFile = True
          break
      except KeyError:
        if doi.endswith(tag.contents[0].get_text()):
          matchedXmlFile = True
          break

    assert matchedXmlFile is True, 'Inline formula for %s is not present in XML file!' % doi

    fileName = self.DOI_PREFFIX + self._name

    assert section['original']['file'].lower() == fileName.lower(), 'File field in Graphics section did not match!'
    assert section['original']['metadata']['doi'] == doi, 'DOI field in Graphics section did not match!'
    assert section['original']['metadata']['contentType'] == self.MIME, 'ContentType field in Graphics section did not match!'
    assert section['original']['metadata']['extension'] == self.EXT, 'Extension field in Graphics section did not match!'
    assert section['original']['metadata']['created'] == section['original']['metadata']['lastModified'], 'Created field in Graphics section did not match!'
    assert section['original']['metadata']['size'] == self.get_size(), 'Size field in Graphics section did not match!'
    self._verify_created_date(section['original']['metadata'], testStartTime, apiTime)
    print 'OK'


  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
    deltaTime = sectionDate - testStartTime
    assert deltaTime.total_seconds() > 0, 'Created field in metadata section should be greater than test start time!'
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and one-leo.plosjournals.org environment (?)
    #assert apiTime > deltaTime.total_seconds(), 'API invocation time should be greater than diff between Created field in metadata section & test start time!'