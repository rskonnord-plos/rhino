#!/usr/bin/env python2

"""
Base class for Rhino related service tests.
"""

__author__ = 'jkrzemien@plos.org'

import unittest
import random
import json
import re
from os import walk
from os.path import dirname, abspath
from inspect import getfile

from requests import get, post, patch, put, delete
from mogilelocal import Client

from ..Decorators.Api import timeit
from ..Config import TIMEOUT, PRINT_DEBUG
from ..Response.JSONResponse import JSONResponse
from ..Response.XMLResponse import XMLResponse
from Simulator import Simulator


IMAGE_FILE_PATTERN = re.compile('\w+\.\d+\.(e|g)\d{3}.(png|tif)$')
DOI_P1 = re.compile("(info:doi)/([0-9\\.]+)/journal\\.([a-z]+)\\.([0-9]+)([\\._a-z0-9]*)")
DOI_P2 = re.compile("(info%3Adoi)%2F([0-9\\.]+)%2Fjournal\\.([a-z]+)\\.([0-9]+)([\\._a-z0-9]*)")
DOI_P3 = re.compile("(info:doi)/([0-9\\.]+)/(image\\.[a-z]+\\.v[0-9]+\\.i[0-9]+)([\\._a-z0-9]*)")
DOI_P4 = re.compile("(info:doi)/([0-9\\.]+)/journal\\.(image\\.[a-z]+\\.v[0-9]+\\.i[0-9]+)([\\._a-z0-9]*)")
# I make some assumptions here on length of the annotation URIs.
DOI_P5 = re.compile("(info:doi)/([0-9\\.]+/annotation)/([a-z0-9\\-]{36})$")
DOI_P6 = re.compile("(info:doi)/([0-9\\.]+/annotation)/([a-z0-9\\-]{36})([\\.\\-a-z0-9]*)")


class BaseServiceTest(unittest.TestCase):

  # This defines any *BaseServiceTest* derived class as able to be run by Nose in a parallel way.
  # Requires Nose's *MultiProcess* plugin to be *enabled*
  _multiprocess_can_split_ = True

  __response = None

  # Autowired by @timeit decorator
  _testStartTime = None

  # Autowired by @timeit decorator
  _apiTime = None

  # Created upon request of get_mogile_client()
  _mogile = None

  _simulator = Simulator()

  def setUp(self):
    pass

  def tearDown(self):
    self.__response = None
    self._testStartTime = None
    self._apiTime = None

  def _debug(self):
    if PRINT_DEBUG:
      print 'API Response = %s' % self.__response.text

  @timeit
  def doGet(self, url, params=None, headers=None, allow_redirects=True):
    self.__response = get(url, params=params, verify=False, timeout=TIMEOUT, allow_redirects=allow_redirects,
                          headers=headers)
    self._debug()

  @timeit
  def doPost(self, url, data=None, files=None, headers=None, allow_redirects=True):
    self.__response = post(url, data=data, files=files, verify=False, timeout=TIMEOUT, allow_redirects=allow_redirects,
                          headers=headers)
    self._debug()

  @timeit
  def doPatch(self, url, data=None, headers=None, allow_redirects=True):
    self.__response = patch(url, data=json.dumps(data), verify=False, timeout=TIMEOUT, allow_redirects=allow_redirects,
                          headers=headers)
    self._debug()

  @timeit
  def doDelete(self, url, data=None, headers=None, allow_redirects=True):
    self.__response = delete(url, data=data, verify=False, timeout=TIMEOUT, allow_redirects=allow_redirects,
                          headers=headers)
    self._debug()

  @timeit
  def doPut(self, url, data=None, headers=None, allow_redirects=True):
    self.__response = put(url, data=data, verify=False, timeout=TIMEOUT, allow_redirects=allow_redirects,
                          headers=headers)
    self._debug()

  @timeit
  def doUpdate(self, url, data=None, headers=None, allow_redirects=True):
    self.doPut(url, data=data, allow_redirects=allow_redirects, headers=headers)

  def simulator(self):
    return self._simulator

  def get_http_response(self):
    return self.__response

  def parse_response_as_xml(self):
    self.parsed = XMLResponse(self.get_http_response().text)

  def parse_response_as_json(self):
    self.parsed = JSONResponse(self.get_http_response().text)

  def verify_http_code_is(self, httpCode):
    print 'Validating HTTP Response code to be %s...' % httpCode,
    self.assertEquals(self.__response.status_code, httpCode)
    print 'OK'

  def find_file(self, filename):
    path = dirname(abspath(getfile(BaseServiceTest))) + '/../../'
    for root, dirs, files in walk(path):
      for file in files:
        if file == filename:
          return root + '/' + file

  def _get_image_filename(self, filename):
    return re.search(IMAGE_FILE_PATTERN, filename).group(0)

  def get_mogile_client(self):
    if not self._mogile:
      self._mogile = Client('../target/classes/datastores/test', '')
    return self._mogile

  def __makeFSID(self, prefix, suffix, ext, type):
    fsid = '/'.join((prefix, suffix, suffix))

    if ext is not '':
      fsid += ext

    fsid += '.'
    fsid += type

    return fsid.lower()

  def doi_to_fsid(self, doi, type):
    """
    Given a PLoS DOI and file type return a Mogile's FSID string. Five DOI formats can be used for conversions.

    1. info:doi/10.1371/journal.pone.0000001
    2. info%3Adoi%2F10.1371%2Fjournal.pone.0000001
    3. info%3Adoi%2F10.1371%2Fjournal.pone.0000001.e0002  - equation example
    4. info%3Adoi%2F10.1371%2Fjournal.image.pone.v03.i08  - image article example
    5. info%3Adoi%2F10.1371%2Fannotation%2F0bac4872-2fa2-416e-ac45-4b0ac79f8ddd - annotation (correction) type article
    6. info%3Adoi%2F10.1371%2Fannotation%2F0bac4872-2fa2-416e-ac45-4b0ac79f8ddd-s001 - annotation (correction) type article asset

    @param prefix - DOI
    @param type - file type ie pdf, xml etc.
    @return - a files store identifier string.
   """
    # DOI's are case insensitive.
    doi = doi.lower()
    extension = ''

    m1 = DOI_P1.match(doi)

    if m1:
      prefix = m1.group(2)
      suffix = '.'.join((m1.group(3), m1.group(4)))

      if len(m1.groups()) == 5:
        extension = m1.group(5)

      return self.__makeFSID(prefix, suffix, extension, type)

    m3 = DOI_P3.matcher(doi)
    if m3:
      prefix = m3.group(2)
      suffix = m3.group(3)

      if len(m3.groups()) == 4:
        extension = m3.group(4)

      return self.__makeFSID(prefix, suffix, extension, type)

    m2 = DOI_P2.matcher(doi)
    if m2:
      prefix = m2.group(2)
      suffix = m2.group(3) + "." + m2.group(4)

      if len(m2.groups()) == 5:
        extension = m2.group(5)
      return self.__makeFSID(prefix, suffix, extension, type)

    m4 = DOI_P4.matcher(doi)
    if m4:
      prefix = m4.group(2)
      suffix = m4.group(3)

      if (m4.groupCount()) == 5:
        extension = m4.group(5)
      return self.__makeFSID(prefix, suffix, extension, type)

    m5 = DOI_P5.matcher(doi)
    if m5:
      prefix = m5.group(2)
      suffix = m5.group(3)

      if len(m5.groups()) == 5:
        extension = m5.group(5)

      return self.__makeFSID(prefix, suffix, extension, type)

    m6 = DOI_P6.matcher(doi)
    if m6:
      prefix = m6.group(2)
      suffix = m6.group(3)

    if len(m6.groups()) == 4:
      extension = m6.group(4)
    return self.__makeFSID(prefix, suffix, extension, type)

    return ""


  @staticmethod
  def _run_tests_randomly():
    import doctest
    doctest.testmod()
    unittest.TestLoader.sortTestMethodsUsing = lambda _, x, y: random.choice([-1, 1])
    unittest.main()
