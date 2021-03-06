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

__author__ = 'jkrzemien@plos.org'

'''
This class loads up the name and data for a TIF file from the ingestion ZIP file, along with the XML file
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
    print 'Validating Graphics metadata section in Response...',

    assert section is not None, "Graphics section in response is NULL!"
    assert section['doi'] == doi, "DOI field in Graphics section did not match!"

    matchedXmlFile = self._xml.find(".//fig/label[contains(text(),'%s')]" % section['title'])
    assert matchedXmlFile is not None, "Title field in Graphics section did not match!"

    matchedXmlFile = self._xml.find(".//fig/caption/p[contains(text(),'%s')]" % section['description'])
    assert matchedXmlFile is not None, "Description field in Graphics section did not match!"

    xpath = ".//%s/*[@xlink:href='%s']" % (section['contextElement'], section['original']['metadata']['doi'])
    matchedXmlFile = self._xml.find(xpath)
    assert matchedXmlFile is not None, "%s field in Graphics section did not match!" % xpath

    fileName = self.DOI_PREFFIX + self._name

    assert section['original']['file'].lower() == fileName.lower(), "File field in Graphics section did not match!"
    assert section['original']['metadata']['doi'] == doi, "DOI field in Graphics section did not match!"
    assert section['original']['metadata']['contentType'] == self.MIME, "ContentType field in Graphics section did not match!"
    assert section['original']['metadata']['extension'] == self.EXT, "Extension field in Graphics section did not match!"
    assert section['original']['metadata']['created'] == section['original']['metadata']['lastModified'], "Created field in Graphics section did not match!"
    assert section['original']['metadata']['size'] == self.get_size(), "Size field in Graphics section did not match!"
    self._verify_created_date(section['original']['metadata'], testStartTime, apiTime)
    print 'OK'


  def _verify_created_date(self, section, testStartTime, apiTime):
    # Some dates (PDF section) seem to include millis too, double check for possible bug?
    sectionDate = datetime.strptime(section['created'], '%Y-%m-%dT%H:%M:%S.%fZ')
    deltaTime = sectionDate - testStartTime
    assert deltaTime.total_seconds() > 0, "Created field in metadata section should be greater than test start time!"
    # Next validation is not working properly because there seems to be a difference of
    # around 7 hours between my box and one-leo.plosjournals.org environment (?)
    #assert apiTime > deltaTime.total_seconds(), "API invocation time should be greater than diff between Created field in metadata section & test start time!"
