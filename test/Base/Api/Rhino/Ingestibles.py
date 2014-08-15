#!/usr/bin/env python2

"""
Base class for Rhino's Ingestibles API service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ...Tests.BaseServiceTest import BaseServiceTest
from ...Config import API_BASE_URL


INGESTIBLES_API = API_BASE_URL + '/ingestibles'


class Ingestibles(BaseServiceTest):

  def ingest_archive(self, filename, force_reingest=''):

    daData = {'name': filename, 'force_reingest': force_reingest}

    self.doPost(INGESTIBLES_API, daData)

    return self


  def list_ingestibles(self):

    self.doGet(INGESTIBLES_API)

    self.parse_response_as_json()

    return self