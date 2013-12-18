#!/usr/bin/env python2
"""
    Description
    ===========

    Script to migrate current Mogile filestore content into
    a SHA1 named files so that we can support a versioning
    repo. 

"""
from __future__ import print_function
from __future__ import with_statement

import os, sys, traceback, hashlib, time
from plosapi import Rhino
from pymogile import Client, MogileFSError

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

def stripPrefix(s):
    return s.replace('10.1371/', '')

def migrateDoi(rh, ds, doiCnt, doi, assets, dataOnly):
    TRANS_FAILED = 'trans|{cnt}|{doi}|{adoi}|{afid}|{c}|{lm}|{t}|{hashID}.SHA1|FAILED_AFID'
    TRANS_OK     = 'trans|{cnt}|{doi}|{adoi}|{afid}|{c}|{lm}|{t}|{hashID}.SHA1|OK'

    _doi = stripPrefix(doi)
    # Get the assets associated with the DOI
    for adoi,afids in assets.iteritems():
         _adoi = stripPrefix(adoi)
         # Get the files associated with that asset ID
         for afid, meta in afids.iteritems():
             _afid = stripPrefix(afid)
             lm = meta['lastModified']
             cType = meta['contentType']
             created = meta['created']
             try:
                 # Temporarily store this to a file
                 # and get the SHA1 hash.
                 (fname, hashID) = rh.getAfid(_afid, useHash='SHA1')
                 if not dataOnly:
	             #Store the stuff to mogile here
                     print('Write to Mogile')
                 # Out put enough info to build a repo database.
	         print(TRANS_OK.format(cnt=doiCnt,doi=doi,adoi=adoi,afid=afid,c=created,lm=lm,t=cType,hashID=hashID))
	     except Exception as e:
                 print(TRANS_FAILED.format(cnt=doiCnt,doi=doi,adoi=adoi,afid=afid,c=created,lm=lm,t=cType,hashID='NONE'))
             finally:
                 if os.path.exists(_afid):
                     os.remove(_afid)

def migrate(rh, ds, dois, dataOnly, retry=3):
    print('DOIs to process: {n}'.format(n=str(len(dois))))
    doiCnt = 0
    # Always start with the DOI
    # Even if the migration fails the 
    # number of files redone in a DOI 
    # will be small.   
    for doi in dois:
        doiRtry = retry
        doiCnt += 1
        # Get the assets associated with the DOI
        while(True and doiRtry > 0):
           try:
               _doi = stripPrefix(doi)
               article = rh.article(_doi)
               assets = article['assets']
               lm = article['lastModified']
               print('info|{cnt}|{doi}|{lm}|INITIATED'.format(cnt=doiCnt, doi=doi, lm=lm))
               migrateDoi(rh, ds, doiCnt, doi, assets, dataOnly)
               doiRtry = -1
               print('info|{cnt}|{doi}|{lm}|OK'.format(cnt=doiCnt, doi=doi, lm=lm))
           except Exception as e:
               doiRtry -= 1
               time.sleep(60*9)
        if doiRtry == 0:
            print('info|{cnt}|{doi}|{lm}|FAILED'.format(cnt=doiCnt,doi=doi,lm='NONE'))
 
if __name__ == "__main__":
    import argparse
    import pprint

    parser = argparse.ArgumentParser(description='Filestore to repo migration tool')
    parser.add_argument('--server', default='http://api.plosjournals.org/', help='Tracker host name.')
    parser.add_argument('--tracker', help='Tracker host name.')
    parser.add_argument('--dstDomain', help='Mogile domain to migrate too.')
    parser.add_argument('--doi', default='None', help='First doi to use on a restarting.')
    parser.add_argument('--srcDomain', default='plos_production', help='Mogile domain to migrate from.')
    parser.add_argument('--file', default='None', help='Use a file to provide doi to migrate.')
    parser.add_argument('-listDOIs', action='store_true', help='List all the availible DOIs to stdout.')
    parser.add_argument('-dataOnly', action='store_true', help='Output repo data only.')
    args = parser.parse_args()

    rh = Rhino(rhinoServer=args.server)

    # Prepare a sorted list of doi's to insure
    # restarts with doi set will start at the correct
    # place.
    if not args.file == 'None':
        f = open(args.file, 'r')
        dois = sorted([ doi.strip() for doi in f ])
    else:
        dois = sorted([doi.strip() for doi in rh.articles()])

    # if list DOIs is set then 
    # list the DOIs and exit
    if args.listDOIs:
        for d in dois:
            print(d)
        os._exit(0)

    stopDoi = args.doi.strip()
    #Reduce the list size if a stop doi specified
    if not args.doi == 'None':
        indx = 0
        for d in dois:
            if d == stopDoi:
                print('found')
                break
            indx += 1
        dois = dois[indx:]
        if len(dois) == 0:
            print('Doi start defined but not found: ' + stopDoi)
            os._exit(0)

    # Create a Mogile Client
    # datastore = Client(domain=args.srcDomain, trackers=[args.tracker])
    datastore = None
    migrate(rh, datastore, dois, args.dataOnly)
