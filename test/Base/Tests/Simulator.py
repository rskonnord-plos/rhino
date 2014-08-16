#!/usr/bin/env python2

"""
Base class for Rhino related service tests.
"""

__author__ = 'jkrzemien@plos.org'

from ..Database.HSQL import HSQL


class Simulator(object):

  def setCaptureMode(self):
    HSQL().modify("UPDATE SIMULATOR SET CAPTURE_MODE = '1' WHERE ID = 1;")

  def setMockMode(self):
    HSQL().modify("UPDATE SIMULATOR SET CAPTURE_MODE = '0' WHERE ID = 1;")

  def setDataStorePath(self, path):
    HSQL().modify("UPDATE SIMULATOR SET DATASTORE_PATH = '%s' WHERE ID = 1;" % path)

  def setDataStoreDomain(self, domain):
    HSQL().modify("UPDATE SIMULATOR SET DATASTORE_DOMAIN = '%s' WHERE ID = 1;" % domain)

  def setMockScenario(self, path):
    HSQL().modify("UPDATE SIMULATOR SET MOCKDATA_FOLDER = '%s' WHERE ID = 1;" % path)

  def setContentRepoAddress(self, address):
    HSQL().modify("UPDATE SIMULATOR SET CONTENT_REPO_ADDRESS = '%s' WHERE ID = 1;" % address)

  def setContentRepoBucketName(self, bucketName):
    HSQL().modify("UPDATE SIMULATOR SET CONTENT_REPO_BUCKET_NAME = '%s' WHERE ID = 1;" % bucketName)

  def setIngestionSourceFolder(self, path):
    HSQL().modify("UPDATE SIMULATOR SET INGESTION_SRC_FOLDER = '%s' WHERE ID = 1;" % path)

  def setIngestionDestinationFolder(self, path):
    HSQL().modify("UPDATE SIMULATOR SET INGESTION_DEST_FOLDER = '%s' WHERE ID = 1;" % path)
