#!/usr/bin/env python2

"""
Class for accessing SQL databases and be able to run queries against it, retrieving results
and/or performing modifications.

Python's JayDeBeApi connectors can be installed via the following commands:

  sudo apt-get install python-jpype
  sudo easy_install JayDeBeApi

"""

__author__ = 'jkrzemien@plos.org'

#import pyodbc
import jaydebeapi
from os import walk
from os.path import dirname, abspath
from inspect import getfile
from contextlib import closing


class HSQL(object):

  def __init__(self):
    self.conn = jaydebeapi.connect('org.hsqldb.jdbc.JDBCDriver', ['jdbc:hsqldb:hsql://localhost/rhinodb;default_schema=true;get_column_name=false', 'sa', ''], self.find_file('hsqldb-2.3.2.jar'),)

  def find_file(self, filename):
    path = dirname(abspath(getfile(HSQL)))
    for root, dirs, files in walk(path):
      for file in files:
        if file == filename:
          return root + '/' + file

  def _getCursor(self):
    return self.conn.cursor()

  def query(self, query, queryArgsTuple=None):

    with closing(self._getCursor()) as cursor:
      cursor.execute(query, queryArgsTuple)
      results = cursor.fetchall()

    return results

  def modify(self, query, queryArgsTuple=None):

    with closing(self._getCursor()) as cursor:
      cursor.execute(query, queryArgsTuple)
      self.conn.commit()




