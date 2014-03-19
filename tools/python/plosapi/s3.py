#!/usr/bin/env python

"""
    PLOS S3 repo module.

    Some definitions:

        DOI: a unique identifier  

"""
from __future__ import print_function
from __future__ import with_statement
from cStringIO import StringIO
from boto.s3.connection import S3Connection
from boto.s3.connection import Location
from time import sleep

import os, sys, re, string, requests, hashlib, json, traceback, urllib

__author__    = 'Bill OConnor'
__copyright__ = 'Copyright 2013, PLOS'
__version__   = '0.1'

class S3:
    """
    """
    _FOREWARD_MDATA_MAP = { 
                         'asset-contenttype':'contentType',
                         'asset-contextelement' : 'contextElement',
                         'asset-created' : 'created',
                         'asset-doi': 'doi',
                         'asset-extension': 'extension',
                         'asset-lastmodified': 'lastModified',
                         'asset-title': 'title',
                         'asset-size': 'size'
                       }
    _REVERSE_MDATA_MAP = { 'contentType' : 'asset-contenttype',
                           'contextElement' : 'asset-contextelement',
                           'created' : 'asset-created',
                           'doi' : 'asset-doi',
                           'extension' : 'asset-extension',
                           'lastModified' : 'asset-lastmodified',
                           'title' : 'asset-title',
                           'size' : 'asset-size'
                         }

    _AWS_SECRET_ACCESS_KEY = os.environ.get('AWS_SECRET_ACCESS_KEY')
    _AWS_ACCESS_KEY_ID = os.environ.get('AWS_ACCESS_KEY_ID')

    _JRNL_IDS = ['pone', 'pmed', 'ppat', 'pbio', 'pgen', 'pcbi', 'pntd', 'pctr', 'pcol' ]
    
    # List of article meta-data fields accepted by this repo 
    _ARTICLE_FLD_LIST = [ 'doi', 'strkImgURI', 'title', 'state', 'eIssn',
                    'eLocationId', 'journal', 'language', 'rights', 'url' ]

    # List of asset meta-data fields accepted by this repo
    _ASSET_FLD_LIST  = [ 'doi', 'title', 'contentType', 'contextElement', 'extension',  
                         'lastModified', 'created', 'size']

    _ARCTILE_DOI_CACHE = dict()


    def __init__(self, bucketID='us-west-1.pub.plos.org', prefix='10.1371'):
        """
        The S3 class is tied to a particular bucket and
        DOI prefix when it is initialized. This simplies
        matters when working with complete DOI and AFID's.
        """
        self.bucketID = bucketID
        self.prefix = prefix
        
        if self._AWS_ACCESS_KEY_ID and self._AWS_SECRET_ACCESS_KEY:
            self.conn = S3Connection(aws_access_key_id=self._AWS_ACCESS_KEY_ID, 
                                     aws_secret_access_key=self._AWS_SECRET_ACCESS_KEY)
            self.bucket = self.conn.get_bucket(bucketID) 
        return

    def _stripPrefix(self, doi, strip):
        if not strip: return doi
        return doi.replace('{p}/'.format(p=self.prefix), '')

    def _assetMeta(self, mData):
        """
        Extract the fields of interest from the asset
        meta-data. 
        """
        newMData = dict()
        # Alas not all asset meta-data has all the same fields.
        for f in self._ASSET_FLD_LIST:
            if mData.has_key(f): newMData['asset-' + f] = mData[f] 
        return newMData

    def _articleMeta(self, mData):
        """
        Extract the fields of interest from the article
        meta-data.
        """
        newMData = dict()
        # Alas not all asset meta-data has all the same fields.
        for f in self._ASSET_FLD_LIST:
            if mData.has_key(f): newMData['article-' + f] = mData[f]
        return newMData

    def _forewardmap(self, name):
        """
        Map a s3 meta-data name to the rhino equivalent.
        """
        if self._FOREWARD_MDATA_MAP.has_key(name):
            return self._FOREWARD_MDATA_MAP[name]
        else:
            return None

    def _reversemap(self, name): 
        """
        Map a rhino meta-data name to the s3 equivalent.
        """
        if self._REVERSE_MDATA_MAP.has_key(name):
            return self._REVERSE_MDATA_MAP[name]
        else:
            return None

    def _ext2upper(self, fname):
        """
        _ext2upper returns the file name with the extension upper cased.

        The file extension is assumed to start at the last period
        in the file name and continue on to the end of the string.
        """
        newfname = fname.split('.')
        newfname[-1] = newfname[-1].upper()
        return '.'.join(newfname)

    def _ext2lower(self, fname):
        """
        _ext2lower returns the file name with the extension lowered cased.

        The file extension is assumed to start at the last period
        in the file name and continue on to the end of the string.
        """
        newfname = fname.split('.')
        newfname[-1] = newfname[-1].lower()
        return '.'.join(newfname)

    def _doi2s3keyPath(self, doiSuffix):
        """
        Given a PLOS specific DOI return a PLOS specific S3 key
        path.

        PLOS DOI's follow the following formats:
 
        ex. 10.1371/journal.PLOSID.XXXXXXXX      (article DOI)
            10.1371/journal.PLOSID.XXXXXXX.gXXXX (asset DOI)
            10.1371/image.PLOSID.vXX.iXX         (image article DOI)
        
        If the key is structured properly it is easy to
        access S3 as if there is a hierarchial directory
        structure. Basically we want to replace the periods
        with '/' except in the case or the DOI prefix 10.1371.
        The prefix should remain intact.

        """
        keySuffix = string.replace(doiSuffix, '.', '/')
        return u'{p}/{s}/'.format(p=self.prefix, s=keySuffix)

    def _afid2s3key(self, afidSuffix, prefix=None):
        """
        Given a PLOS specific asset file ID (AFID) return a 
        PLOS specific S3 key.

        The AFID is fundementally a DOI with an extension 
        indicating the type/represention of the data contained
        within the asset. A completed S3 key will extract the DOI 
        portion of the AFID, convert it to an S3 key path, and
        append the afidSuffix.

        ex. 10.1371/journal.PLOSID.XXXXXXXX.XML -> 
            (S3 Key) 10.1371/journal/PLOSID/XXXXXXXX/PLOSID.XXXXXXXX.xml
            
            10.1371/image.PLOSID.vxx.ixx.xml ->
            10.1371/image/PLOSID/vxx/ixx/image.PLOSID.vxx.ixx.xml
           
            10.1371/annotation/XXXXXXXXXXXXXXX.xml ->
            10.1371/annotation/XXXXXXXXXXXXXXX.xml  
        """
        if not prefix: prefix = self.prefix

        if afidSuffix.lower().startswith('journal'):
            doiSuffix = '/'.join(afidSuffix.lower().split('.')[:-1])
            newAFID = '.'.join(afidSuffix.lower().split('.')[1:])
            fullAFID = u'{p}/{s}/{a}'.format(p=prefix, s=doiSuffix, a=newAFID)
        elif afidSuffix.lower().startswith('image'):
            # Get everything except the extensions
            doiSuffix = '/'.join(afidSuffix.lower().split('.')[:-1])
            fullAFID = u'{p}/{s}/{a}'.format(p=prefix, s=doiSuffix, a=afidSuffix.lower())
        elif afidSuffix.lower().startswith('annotation'):
            fullAFID = u'{p}/{a}'.format(p=prefix, a=afidSuffix.lower()) 
        else:
            raise Exception('s3:invalid afid suffix ' + afidSuffix)
        return fullAFID

    def _s3keyPath2doi(self, s3keyPath):
        """
        Use an S3 based key to create the DOI associated with the key. 
        
        ex: 10.1371/journal/pone/1234567/journal.pone.1234567.xml ->
            10.1371/journal.pone.1234567
        """
        elemLst = s3keyPath.split('/')
        if elemLst[1] == 'annotation':
           fullDOI = u'{p}/{s}'.format(p=elemLst[0], s= '/'.join(elemLst[1:-1]))
        else:
           fullDOI = u'{p}/{s}'.format(p=elemLst[0], s= '.'.join(elemLst[1:-1]))
        return fullDOI

    def _s3key2afid(self, s3key):
        """
        """
        elemLst = s3key.split('/')
        elemLst[-1] = self._ext2upper(elemLst[-1])
        if elemLst[1].lower() == 'journal':
            fullAFID = u'{p}/journal.{s}'.format(p=elemLst[0], s= '.'.join(elemLst[-1:]))    
        elif elemLst[1].lower() == 'image':
            fullAFID = u'{p}/{s}'.format(p=elemLst[0], s= '.'.join(elemLst[-1:]))
        elif elemLst[1].lower() == 'annotation':
            fullAFID = s3key
        else:
            raise Exception('s3:invalid s3 key ' + s3key)
        return fullAFID
    
    def _afidsFromDoi(self, doiSuffix):
        """
        Given a DOI Suffix return a list of AFIDs 
        """
        assets = self.assets(doiSuffix)
        for (adoi, afids) in assets.iteritems():
            for fullAFID in afids:
                (p, afid) = fullAFID.split('/', 1)
                yield afid 

    def _getAssetMeta(self, afidSuffix):
        """
        """
        keyID = self._afid2s3key(afidSuffix)
        mdata = self.bucket.get_key(keyID).metadata
        result = dict()
        for k in mdata.iterkeys():
            lk = k.lower()
            mappedKey = self._forewardmap(lk)
            if mappedKey:
                result[self._forewardmap(lk)] = mdata[k]
        
        result['md5'] = mdata['asset-md5']
        result['description'] = 'S3 does not support descriptions'
        return result
         
    def _getBinary(self, fname, fullKey):
        """
        Most of the files other than the article xml are binary in nature.
        Fetch the data and write it to a temporary file. Return the MD5
        hash of the fle contents.
        """
        m5 = hashlib.md5()
        s1 = hashlib.sha1()

        k = self.bucket.get_key(fullKey)
        with open(fname, 'wb') as f:
            for chunk in k:
                m5.update(chunk)
                s1.update(chunk)
                f.write(chunk)
            f.close()
        return (fname, m5.hexdigest(), s1.hexdigest(), k.content_type, k.size) 
    
    def buckets(self):
        """
        Return a list of all the buckets associated with this account.
        """
        return self.conn.get_all_buckets()

    def keycheck(self, afidSuffix):
        """
        Ultimately data is associate with unique 
        asset file ids. keycheck returns the meta-data
        assocated with this afid.
        """
        keyID = self._afid2s3key(afidSuffix)
        # keys = self.bucket.list_versions(prefix=fullKey, delimiter='/')
        keys = [ self.bucket.get_key(keyID) ]
        for k in keys:
            mdata = k.metadata
            mdata['S3:name'] = k.name
            mdata['S3:cache_control'] = k.cache_control
            mdata['S3:content_type'] = k.content_type
            mdata['S3:content_encoding'] = k.content_encoding
            mdata['S3:content_disposition'] = k.content_disposition
            mdata['S3:content_language'] = k.content_language
            mdata['S3:etag'] = k.etag
            mdata['S3:last_modified'] = k.last_modified
            mdata['S3:owner'] = k.owner
            mdata['S3:storage_class'] = k.storage_class
            mdata['S3:md5'] = k.md5
            mdata['S3:size'] = k.size
            mdata['S3:version_id'] = k.version_id
            mdata['S3:encrypted'] = k.encrypted        
        return mdata

    def bucket_names(self):
        """
        Return a list of bucket names.
        """
        return [ b.name for b in self.conn.get_all_buckets()]

    def has_doi(self, doiSuffix):
        """
        In this case if the article has assets for
        this doiSuffix then the DOI exists. That is not 
        say that a complete copy of all the assets exist
        on S3.
        """
        s3keyPath = self._doi2s3keyPath(doiSuffix)
        # If there exist any keys with this DOI return True
        bklstRslt = self.bucket.list(delimiter='/', prefix=s3keyPath)
        return not len(bklstRslt) == 0

    def articles(self, useCache=False, stripPrefix=False):
        """
        Get a list of DOIs from the s3 bucket keys. This is some what
        DOI specific. For image articles we need to iterate over 3
        levels. For journals only 2.

        The useCache parameter turns caching of DOIs on or off.
        This is to be used in situations where getting multiple lists 
        of DOIs would slow down processing.  
        """
        if useCache and not len(self._ARTICLE_DOI_CACHE) == 0:
            for k in self._ARTICLE_DOI_CACHE.keys():
                yield self._stripPrefix(k, stripPrefix)
        else:
            # Get the image article DOIs
            bklstRslt = self.bucket.list(delimiter='/', prefix=self.prefix + '/image/')
            for p1 in bklstRslt:
                bklstRslt2 = self.bucket.list(delimiter='/', prefix=p1.name)
                for p2 in bklstRslt2:
                    bklstRslt3 = self.bucket.list(delimiter='/', prefix=p2.name)
                    for k in bklstRslt3:
                        fullDOI = self._s3keyPath2doi(k.name)
                        if useCache: self._ARTICLE_DOI_CACHE[fullDOI] = 1
                        yield self._stripPrefix(fullDOI, stripPrefix)
            # Get the journal DOIs
            # TODO: Need to rethink this - jrnl list should be had from reading
            #       the bucket. 
            prefixLst = [ '{p}/journal/{id}/'.format(p=self.prefix, id=jrnlid) for jrnlid in self._JRNL_IDS ] 
            for p in prefixLst:
                bklstRslt = self.bucket.list(delimiter='/', prefix=p) 
                for k in bklstRslt:
                    fullDOI = self._s3keyPath2doi(k.name)
                    if useCache: self._ARTICLE_DOI_CACHE[fullDOI] = 1
                    yield self._stripPrefix(fullDOI, stripPrefix)
            # Get the annotation DOIs
            prefix = '{p}/annotation/'.format(p=self.prefix) 
            bklstRslt = self.bucket.list(delimiter='/', prefix=p)
            for k in bklstRslt:
                fullDOI = self._s3keyPath2doi(k.name)
                if useCache: self._ARTICLE_DOI_CACHE[fullDOI] = 1
                yield self._stripPrefix(fullDOI, stripPrefix)
    
    def article(self, doiSuffix):
        """
        Since s3 in only storing article data at this point 
        the article meta-data is not available.
        """
        raise NotImplementedError('s3:article not supported')    

    def rmArticle(self, doiSuffix):
        """
        """
        for afid in self._afidsFromDoi(doiSuffix):
            print(self._afid2s3key(afid))

    def assets(self, doiSuffix):
        """
        Return a map with ADOI's as keys and a list of
        AFIDs for each ADOI.
        """
        artDOI = '{p}/{s}'.format(p=self.prefix, s=doiSuffix)
        assets = {}
        afids = [] 
        assetDOIs = []
        s3keyPath = self._doi2s3keyPath(doiSuffix)

        # Pass 1: breakout xml and pdf afids from asset DOIs
        bklstRslt = self.bucket.list(delimiter='/', prefix=s3keyPath)
        for k in bklstRslt:
            if k.name.endswith('/'):
                assetDOIs.append(k.name)
            else:
                afids.append(self._s3key2afid(k.name))
        assets[artDOI] = afids
        
        # Pass 2: process the asset DOIs
        for assetDOI in assetDOIs:
            afids = []
            bklstRslt = self.bucket.list(delimiter='/', prefix=assetDOI)
            for k in bklstRslt:
                afids.append(self._s3key2afid(k.name))
            assets[self._s3keyPath2doi(assetDOI)] = afids
        return assets

    def asset(self, adoiSuffix):
        """
        Given an ADOI dump the meta-data 
        """
        assets = self.assets(adoiSuffix)
        result = dict()
        fullDOI = '{p}/{s}'.format(p=self.prefix, s=adoiSuffix)
        afids = assets[fullDOI]
        for fullAFID in afids:
           afid = fullAFID.split('/')
           if afid[0] == self.prefix:
               result[fullAFID] = self._getAssetMeta(afid[1])
           else:
               raise Exception('s3:invalid s3 prefix ' + fullAFID)
        return result

    def assetall(self, adoiSuffix):
        """
        """
        assets = self.assets(adoiSuffix)
        result = dict()
        for (adoi, afids) in assets.iteritems():
            for fullAFID in afids:
                afid = fullAFID.split('/')
                result[fullAFID] = self._getAssetMeta(afid[1])
        return result

    def assetFileMD5(self, afidSuffix):
        """
        It just so happens the etag component of
        the key is also the md5 hash. 
        """
        keyID = self._afid2s3key(afidSuffix)
        k = self.bucket.get_key(keyID)
        return string.strip(k.etag, '"') 

    def getAfid(self, afidSuffix, fname=None):
        """
        Retreive the actual asset data. If the file name is not
        specified use the afid as the file name.
        """
        if fname == None:
            fname = urllib.quote(self._ext2upper(afidSuffix))
        keyID = self._afid2s3key(afidSuffix)
        return self._getBinary(fname, keyID)

    def putAfid(self, afidSuffix, fname, articleMeta, assetMeta, md5, prefix=None,
                  force=False, cb=None, reduced_redundancy=True, retry=5, wait=2):
        """
        """
        status = 'FAILED'
        # See if the key exists
        keyID = self._afid2s3key(afidSuffix, prefix)
        s3key = self.bucket.get_key(keyID)

        if not s3key == None:
            status = 'EXIST'
            # yes - see if they are the same
            if not force and (s3key.etag.replace('"','') == md5):
                status += ' IDENTICAL'   
                return (afidSuffix, fname, md5, status)
        else:
            s3key = self.bucket.new_key(keyID)

        s3key.set_metadata('Content-Disposition', 'attachment; filename="{f}"'.format(f=fname))

        # Use the default prefix if not specified.
        if not prefix: prefix = self.prefix
        
        # Merge the article and asset meta-data
        mData = self._articleMeta(articleMeta)
        mData.update(self._assetMeta(assetMeta))
        mData['asset-md5'] = md5

        # Remove duplicate info
        k = 'asset-title'
        k2 = 'article-title'
        if mData.has_key(k) and (mData[k] == mData[k2]): 
            mData[k] = ''

        s3key.set_metadata('Content-Type', mData['asset-contentType'].encode('utf-8'))
        s3key.set_metadata('Content-Language', 'en'.encode('utf-8'))
        # Copy the meta-data
        [ s3key.set_metadata(k, v) for k,v in mData.iteritems() ]
        cnt = 0
        # Sometimes S3 doesn't respond quickly 
        while(cnt < retry):
            try:
                s3key.set_contents_from_filename(fname, cb=cb, replace=True, reduced_redundancy=reduced_redundancy)
                status = 'COPIED'
                cnt = retry
            except Exception as e:
               if ++cnt >= retry:
                   s = 'S3:putAfid - failed transfer {f} after {rt} retries.\n {msg}'
                   raise Exception(s.format(f=fname, rt=retry, msg=e.message))
               sleep(wait)

        return (afidSuffix, fname, md5, status) 

    def articleFiles(self, doiSuffix):
        """
        Download files for all AFIDs associated with this
        DOI. 
        """
        dname = urllib.quote(doiSuffix)
        os.mkdir(dname)
        os.chdir('./{d}'.format(d=dname))
        result = { doiSuffix : [ (afid, self.getAfid(afid)) for afid in self._afidsFromDoi(doiSuffix) ] }
        os.chdir('../')
        return result 

if __name__ == "__main__":
    """
    Main entry point for command line execution. 
    """
    import argparse
    import pprint

    # Main command dispatcher.
    dispatch = { 'buckets'      : lambda repo, params: repo.bucket_names(),
                 'keycheck'     : lambda repo, params: [ repo.keycheck(afid) for afid in params ],
                 'articlefiles' : lambda repo, params: [ repo.articleFiles(doi) for doi in params ],
                 'article'      : lambda repo, params: [ repo.article(doi) for doi in params ],
                 'articles'     : lambda repo, params: repo.articles(),
                 'rm-article'   : lambda repo, params: [ repo.rmArticle(doi) for doi in params ],
                 'assets'       : lambda repo, params: [ repo.assets(doi) for doi in params ],
                 'asset'        : lambda repo, params: [ repo.asset(doi) for doi in params ],
                 'assetall'     : lambda repo, params: [ repo.assetall(doi) for doi in params ],
                 'md5'          : lambda repo, params: [ repo.assetFileMD5(adoi) for adoi in params ],
               }

    pp = pprint.PrettyPrinter(indent=2)
    parser = argparse.ArgumentParser(description='S3 API client module.')
    parser.add_argument('--bucket', help='specify an S3 buckt to use.')
    parser.add_argument('--prefix', help='specify a DOI prefix.')
    parser.add_argument('command', help="articles, article, articlefiles, assets, asset, assetfile, assetAll, md5, buckets, keycheck")
    parser.add_argument('params', nargs='*', help="command parameters")
    args = parser.parse_args()

    try:
        for val in dispatch[args.command](S3(), args.params):
            pp.pprint(val)
    except Exception as e:
        sys.stderr.write('Exception: {msg}.\n'.format(msg=e.message))
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)

    sys.exit(0)
