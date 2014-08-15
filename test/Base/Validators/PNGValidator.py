#!/usr/bin/env python2

__author__ = 'jkrzemien@plos.org'

'''
This class loads up an XML file in order to be used later on for validations against
Tests's responses.
'''

from TIFValidator import TIFValidator


class PNGValidator(TIFValidator):

  def __init__(self, name, data, xml):
    super(PNGValidator, self).__init__(name, data, xml)
    self.MIME = 'image/png'
    self.EXT = 'PNG'

  def metadata(self, section, doi, testStartTime, apiTime):
    print 'Validating %s metadata section in Response...' % self.EXT,

    assert section is not None, 'Graphics section in response is NULL!'
    assert section['doi'] == doi, 'DOI field in Graphics section did not match %s!' % doi

    matchedXmlFile = [f for f in self._xml.get_xml().find_all(section['contextElement']) if f.contents[0].attrs['xlink:href'] == doi]
    assert len(matchedXmlFile) == 1, 'Inline formula for %s is not present in XML file!' % doi

    fileName = self.DOI_PREFFIX + self._name

    assert section['original']['file'].lower() == fileName.lower(), 'File field in Graphics section did not match!'
    assert section['original']['metadata']['doi'] == doi, 'DOI field in Graphics section did not match!'
    assert section['original']['metadata']['contentType'] == self.MIME, 'ContentType field in Graphics section did not match!'
    assert section['original']['metadata']['extension'] == self.EXT, 'Extension field in Graphics section did not match!'
    assert section['original']['metadata']['created'] == section['original']['metadata']['lastModified'], 'Created field in Graphics section did not match!'
    assert section['original']['metadata']['size'] == self.get_size(), 'Size field in Graphics section did not match!'
    self._verify_created_date(section['original']['metadata'], testStartTime, apiTime)
    print 'OK'
