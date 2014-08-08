CREATE TABLE annotation (
  annotationID bigint(19) NOT NULL auto_increment,
  annotationURI varchar(255) NOT NULL,
  articleID bigint(19),
  parentID bigint(19),
  userProfileID bigint(19) NOT NULL,
  annotationCitationID bigint(19),
  type varchar(16),
  title text(65535),
  body text(65535),
  competingInterestBody text(65535),
  highlightedText text(65535),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (annotationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE annotationCitation (
  annotationCitationID bigint(19) NOT NULL auto_increment,
  year varchar(255),
  volume varchar(255),
  issue varchar(255),
  journal varchar(255),
  title text(65535),
  publisherName text(65535),
  eLocationId varchar(255),
  note text(65535),
  url varchar(255),
  summary varchar(10000),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (annotationCitationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE annotationCitationAuthor (
  annotationCitationAuthorID bigint(19) NOT NULL auto_increment,
  annotationCitationID bigint(19),
  fullName varchar(200),
  givenNames varchar(150),
  surnames varchar(200),
  suffix varchar(100),
  sortOrder int(10),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (annotationCitationAuthorID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE annotationCitationCollabAuthor (
  annotationCitationID bigint(19) DEFAULT 0 NOT NULL,
  sortOrder int(10) DEFAULT 0 NOT NULL,
  name varchar(255),
  PRIMARY KEY (annotationCitationID,sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE annotationFlag (
  annotationFlagID bigint(19) NOT NULL auto_increment,
  annotationID bigint(19) NOT NULL,
  userProfileID bigint(19) NOT NULL,
  reason varchar(25) NOT NULL,
  comment text(65535),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (annotationFlagID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE article (
  articleID bigint(19) NOT NULL auto_increment,
  doi varchar(50),
  title varchar(500),
  eIssn varchar(15),
  state int(10) NOT NULL,
  archiveName varchar(50),
  description text(65535),
  rights text(65535),
  language varchar(5),
  format varchar(10),
  date datetime NOT NULL,
  volume varchar(5),
  issue varchar(5),
  journal varchar(50),
  publisherLocation varchar(25),
  publisherName varchar(100),
  pages varchar(15),
  eLocationID varchar(15),
  url varchar(100),
  strkImgURI varchar(50),
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (articleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleAsset (
  articleAssetID bigint(19) NOT NULL auto_increment,
  articleID bigint(19),
  doi varchar(75) NOT NULL,
  contextElement varchar(30),
  contentType varchar(100),
  extension varchar(10),
  title varchar(500),
  description text(65535),
  size bigint(19),
  sortOrder int(10),
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (articleAssetID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleCategoryFlagged (
  articleID bigint(19) NOT NULL,
  categoryID bigint(19) NOT NULL,
  userProfileID bigint(19),
  created datetime NOT NULL,
  lastModified datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleCategoryJoinTable (
  articleID bigint(19) NOT NULL,
  categoryID bigint(19) NOT NULL,
  PRIMARY KEY (articleID,categoryID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleCollaborativeAuthors (
  articleID bigint(19) NOT NULL,
  sortOrder int(10) NOT NULL,
  name varchar(255),
  PRIMARY KEY (articleID,sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleList (
  articleListID bigint(19) NOT NULL auto_increment,
  listCode varchar(255),
  displayName varchar(255),
  journalID bigint(19),
  journalSortOrder int(10),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (articleListID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleListJoinTable (
  articleListID bigint(19) NOT NULL,
  sortOrder int(10) NOT NULL,
  doi varchar(255),
  PRIMARY KEY (articleListID,sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articlePerson (
  articlePersonID bigint(19) NOT NULL auto_increment,
  articleID bigint(19),
  sortOrder int(10),
  type varchar(15) NOT NULL,
  fullName varchar(100) NOT NULL,
  givenNames varchar(100),
  surnames varchar(100) NOT NULL,
  suffix varchar(15),
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (articlePersonID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articlePublishedJournals (
  articleID bigint(19) NOT NULL,
  journalID bigint(19) NOT NULL,
  PRIMARY KEY (articleID,journalID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleRelationship (
  articleRelationshipID bigint(19) NOT NULL auto_increment,
  parentArticleID bigint(19) NOT NULL,
  otherArticleDoi varchar(100),
  otherArticleID bigint(19),
  type varchar(50) NOT NULL,
  sortOrder int(10) NOT NULL,
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (articleRelationshipID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE articleType (
  articleID bigint(19) NOT NULL,
  type varchar(100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE category (
  categoryID bigint(19) NOT NULL auto_increment,
  path varchar(255),
  created datetime,
  lastModified datetime,
  PRIMARY KEY (categoryID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE categoryFeaturedArticle (
  categoryFeaturedArticleID bigint(19) NOT NULL auto_increment,
  journalID bigint(19) NOT NULL,
  articleID bigint(19) NOT NULL,
  category varchar(100) NOT NULL,
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (categoryFeaturedArticleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE citedArticle (
  citedArticleID bigint(19) NOT NULL auto_increment,
  articleID bigint(19),
  keyColumn varchar(10),
  year int(10),
  displayYear varchar(50),
  month varchar(15),
  day varchar(20),
  volumeNumber int(10),
  volume varchar(100),
  issue varchar(60),
  title text(65535),
  publisherLocation varchar(250),
  publisherName text(65535),
  pages varchar(150),
  eLocationID varchar(100),
  journal varchar(250),
  note text(65535),
  url varchar(100),
  doi varchar(100),
  citationType varchar(60),
  summary varchar(100),
  sortOrder int(10),
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (citedArticleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE citedArticleCollaborativeAuthors (
  citedArticleID bigint(19) NOT NULL,
  sortOrder int(10) NOT NULL,
  name varchar(255),
  PRIMARY KEY (citedArticleID,sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE citedPerson (
  citedPersonID bigint(19) NOT NULL auto_increment,
  citedArticleID bigint(19),
  type varchar(15) NOT NULL,
  fullName varchar(200),
  givenNames varchar(150),
  surnames varchar(200),
  suffix varchar(100),
  sortOrder int(10),
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (citedPersonID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE issue (
  issueID bigint(19) NOT NULL auto_increment,
  issueUri varchar(255),
  volumeID bigint(19),
  volumeSortOrder int(10),
  displayName varchar(255) NOT NULL,
  respectOrder bit(1),
  imageUri varchar(255),
  title varchar(500),
  description text(65535),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (issueID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE issueArticleList (
  issueID bigint(19) DEFAULT 0 NOT NULL,
  sortOrder int(10) DEFAULT 0 NOT NULL,
  doi varchar(100),
  PRIMARY KEY (issueID,sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE journal (
  journalID bigint(19) NOT NULL auto_increment,
  currentIssueID bigint(19),
  journalKey varchar(255),
  eIssn varchar(255),
  imageUri varchar(255),
  title varchar(500),
  description text(65535),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (journalID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE pingback (
  pingbackID bigint(19) NOT NULL auto_increment,
  articleID bigint(19) NOT NULL,
  url varchar(255) NOT NULL,
  title varchar(255),
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (pingbackID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE savedSearch (
  savedSearchID bigint(19) NOT NULL auto_increment,
  userProfileID bigint(19) NOT NULL,
  savedSearchQueryID bigint(19) NOT NULL,
  searchName varchar(255) NOT NULL,
  searchType varchar(16) NOT NULL,
  lastWeeklySearchTime datetime NOT NULL,
  lastMonthlySearchTime datetime NOT NULL,
  monthly bit(1) DEFAULT b'0',
  weekly bit(1) DEFAULT b'0',
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (savedSearchID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE savedSearchQuery (
  savedSearchQueryID bigint(19) NOT NULL auto_increment,
  searchParams text(65535) NOT NULL,
  hash varchar(50) NOT NULL,
  created datetime NOT NULL,
  lastmodified datetime NOT NULL,
  PRIMARY KEY (savedSearchQueryID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE syndication (
  syndicationID bigint(19) NOT NULL auto_increment,
  doi varchar(255) NOT NULL,
  target varchar(50) NOT NULL,
  status varchar(50) NOT NULL,
  submissionCount int(10),
  errorMessage longtext,
  created datetime NOT NULL,
  lastSubmitTimestamp datetime,
  lastModified datetime,
  PRIMARY KEY (syndicationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE trackback (
  trackbackID bigint(19) NOT NULL auto_increment,
  articleID bigint(19) NOT NULL,
  url varchar(500) NOT NULL,
  title varchar(500),
  blogname varchar(500) NOT NULL,
  excerpt text(65535) NOT NULL,
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (trackbackID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userArticleView (
  userArticleViewID bigint(19) NOT NULL auto_increment,
  userProfileID bigint(19) NOT NULL,
  articleID bigint(19) NOT NULL,
  created datetime NOT NULL,
  type varchar(20) NOT NULL,
  PRIMARY KEY (userArticleViewID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userLogin (
  userLoginID bigint(19) NOT NULL auto_increment,
  userProfileID bigint(19) NOT NULL,
  sessionID varchar(255),
  IP varchar(100),
  userAgent varchar(255),
  created datetime NOT NULL,
  PRIMARY KEY (userLoginID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userProfile (
  userProfileID bigint(19) NOT NULL auto_increment,
  userProfileURI varchar(100) NOT NULL,
  authId varchar(255),
  realName varchar(500),
  givenNames varchar(255),
  surName varchar(65),
  title varchar(255),
  gender varchar(15),
  email varchar(255),
  homePage varchar(512),
  weblog varchar(512),
  publications varchar(255),
  displayName varchar(255),
  suffix varchar(255),
  positionType varchar(255),
  organizationName varchar(512),
  organizationType varchar(255),
  organizationVisibility tinyint(3) DEFAULT 0 NOT NULL,
  postalAddress text(65535),
  city varchar(255),
  country varchar(255),
  biography text(65535),
  interests text(65535),
  researchAreas text(65535),
  alertsJournals text(65535),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  password varchar(255) DEFAULT 'pass' NOT NULL,
  verificationToken varchar(255),
  verified bit(1) DEFAULT b'1' NOT NULL,
  active bit(1) DEFAULT b'1' NOT NULL,
  PRIMARY KEY (userProfileID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userProfileRoleJoinTable (
  userRoleID bigint(19) NOT NULL,
  userProfileID bigint(19) NOT NULL,
  PRIMARY KEY (userRoleID,userProfileID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userRole (
  userRoleID bigint(19) NOT NULL auto_increment,
  roleName varchar(15),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (userRoleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userRolePermission (
  userRoleID bigint(19) NOT NULL,
  permission varchar(255) NOT NULL,
  PRIMARY KEY (userRoleID,permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE userSearch (
  userSearchID bigint(19) NOT NULL auto_increment,
  userProfileID bigint(19) NOT NULL,
  searchTerms text(65535),
  searchString text(65535),
  created datetime NOT NULL,
  PRIMARY KEY (userSearchID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE version (
  versionID bigint(19) NOT NULL auto_increment,
  name varchar(25) NOT NULL,
  version int(10) NOT NULL,
  updateInProcess bit(1) NOT NULL,
  created datetime NOT NULL,
  lastModified datetime,
  PRIMARY KEY (versionID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE volume (
  volumeID bigint(19) NOT NULL auto_increment,
  volumeUri varchar(255),
  journalID bigint(19),
  journalSortOrder int(10),
  displayName varchar(255),
  imageUri varchar(255),
  title varchar(500),
  description text(65535),
  created datetime NOT NULL,
  lastModified datetime NOT NULL,
  PRIMARY KEY (volumeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE annotation
ADD FOREIGN KEY (parentID)
REFERENCES annotation (annotationID);

ALTER TABLE annotation
ADD FOREIGN KEY (annotationCitationID)
REFERENCES annotationCitation (annotationCitationID);

ALTER TABLE annotation
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);

ALTER TABLE annotation
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);



ALTER TABLE annotationCitationAuthor
ADD FOREIGN KEY (annotationCitationID)
REFERENCES annotationCitation (annotationCitationID);



ALTER TABLE annotationCitationCollabAuthor
ADD FOREIGN KEY (annotationCitationID)
REFERENCES annotationCitation (annotationCitationID);



ALTER TABLE annotationFlag
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);

ALTER TABLE annotationFlag
ADD FOREIGN KEY (annotationID)
REFERENCES annotation (annotationID);



ALTER TABLE articleAsset
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE articleCategoryFlagged
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);

ALTER TABLE articleCategoryFlagged
ADD FOREIGN KEY (categoryID)
REFERENCES category (categoryID);

ALTER TABLE articleCategoryFlagged
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE articleCategoryJoinTable
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);

ALTER TABLE articleCategoryJoinTable
ADD FOREIGN KEY (categoryID)
REFERENCES category (categoryID);



ALTER TABLE articleCollaborativeAuthors
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE articleList
ADD FOREIGN KEY (journalID)
REFERENCES journal (journalID);



ALTER TABLE articleListJoinTable
ADD FOREIGN KEY (articleListID)
REFERENCES articleList (articleListID);



ALTER TABLE articlePerson
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE articlePublishedJournals
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);

ALTER TABLE articlePublishedJournals
ADD FOREIGN KEY (journalID)
REFERENCES journal (journalID);



ALTER TABLE articleRelationship
ADD FOREIGN KEY (otherArticleID,parentArticleID)
REFERENCES article (articleID,articleID);



ALTER TABLE articleType
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE categoryFeaturedArticle
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);

ALTER TABLE categoryFeaturedArticle
ADD FOREIGN KEY (journalID)
REFERENCES journal (journalID);



ALTER TABLE citedArticle
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE citedArticleCollaborativeAuthors
ADD FOREIGN KEY (citedArticleID)
REFERENCES citedArticle (citedArticleID);



ALTER TABLE citedPerson
ADD FOREIGN KEY (citedArticleID)
REFERENCES citedArticle (citedArticleID);



ALTER TABLE issue
ADD FOREIGN KEY (volumeID)
REFERENCES volume (volumeID);



ALTER TABLE issueArticleList
ADD FOREIGN KEY (issueID)
REFERENCES issue (issueID);



ALTER TABLE journal
ADD FOREIGN KEY (currentIssueID)
REFERENCES issue (issueID);



ALTER TABLE savedSearch
ADD FOREIGN KEY (savedSearchQueryID)
REFERENCES savedSearchQuery (savedSearchQueryID);

ALTER TABLE savedSearch
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);



ALTER TABLE trackback
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);



ALTER TABLE userArticleView
ADD FOREIGN KEY (articleID)
REFERENCES article (articleID);

ALTER TABLE userArticleView
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);



ALTER TABLE userLogin
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);



ALTER TABLE userProfileRoleJoinTable
ADD FOREIGN KEY (userRoleID)
REFERENCES userRole (userRoleID);

ALTER TABLE userProfileRoleJoinTable
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);



ALTER TABLE userRolePermission
ADD FOREIGN KEY (userRoleID)
REFERENCES userRole (userRoleID);



ALTER TABLE userSearch
ADD FOREIGN KEY (userProfileID)
REFERENCES userProfile (userProfileID);



ALTER TABLE volume
ADD FOREIGN KEY (journalID)
REFERENCES journal (journalID);



CREATE UNIQUE INDEX annotationCitationID ON annotationCitationAuthor (annotationCitationID,sortOrder);

CREATE UNIQUE INDEX annotationCitationID ON annotationCitationAuthor (annotationCitationID,sortOrder);

CREATE UNIQUE INDEX annotationCitationID ON annotation (annotationCitationID);

CREATE INDEX annotationID ON annotationFlag (annotationID);

CREATE UNIQUE INDEX annotationURI ON annotation (annotationURI);

CREATE INDEX articleID ON articleCollaborativeAuthors (articleID);

CREATE UNIQUE INDEX articleID ON articleCategoryFlagged (articleID,categoryID,userProfileID);

CREATE INDEX articleID ON articleCategoryJoinTable (articleID);

CREATE INDEX articleID ON articleType (articleID);

CREATE INDEX articleID ON categoryFeaturedArticle (articleID);

CREATE INDEX articleID ON citedArticle (articleID);

CREATE INDEX articleID ON trackback (articleID);

CREATE UNIQUE INDEX articleID ON pingback (articleID,url);

CREATE INDEX articleID ON annotation (articleID);

CREATE UNIQUE INDEX articleID ON articleCategoryFlagged (articleID,categoryID,userProfileID);

CREATE INDEX articleID ON articlePerson (articleID);

CREATE INDEX articleID ON userArticleView (articleID);

CREATE UNIQUE INDEX articleID ON pingback (articleID,url);

CREATE INDEX articleID ON articleAsset (articleID);

CREATE UNIQUE INDEX articleID ON articleCategoryFlagged (articleID,categoryID,userProfileID);

CREATE UNIQUE INDEX authId ON userProfile (authId);

CREATE INDEX categoryID ON articleCategoryFlagged (categoryID);

CREATE INDEX categoryID ON articleCategoryJoinTable (categoryID);

CREATE INDEX citedArticleID ON citedArticleCollaborativeAuthors (citedArticleID);

CREATE INDEX citedArticleID ON citedPerson (citedArticleID);

CREATE INDEX currentIssueID ON journal (currentIssueID);

CREATE UNIQUE INDEX displayName ON userProfile (displayName);

CREATE UNIQUE INDEX doi ON syndication (doi,target);

CREATE INDEX doi ON articleAsset (doi);

CREATE UNIQUE INDEX doi ON syndication (doi,target);

CREATE INDEX doi ON article (doi);

CREATE UNIQUE INDEX doi_2 ON articleAsset (doi,extension);

CREATE UNIQUE INDEX doi_2 ON articleAsset (doi,extension);

CREATE UNIQUE INDEX email ON userProfile (email);

CREATE INDEX hash ON savedSearchQuery (hash);

CREATE UNIQUE INDEX hash_2 ON savedSearchQuery (hash);

CREATE UNIQUE INDEX issueUri ON issue (issueUri);

CREATE INDEX journalID ON articlePublishedJournals (journalID);

CREATE UNIQUE INDEX journalID ON categoryFeaturedArticle (journalID,category);

CREATE INDEX journalID ON articleList (journalID);

CREATE UNIQUE INDEX journalID ON categoryFeaturedArticle (journalID,category);

CREATE INDEX journalID ON volume (journalID);

CREATE UNIQUE INDEX listCode ON articleList (listCode);

CREATE INDEX otherArticleID ON articleRelationship (otherArticleID);

CREATE INDEX parentArticleID ON articleRelationship (parentArticleID);

CREATE INDEX parentID ON annotation (parentID);

CREATE UNIQUE INDEX path ON category (path);

CREATE UNIQUE INDEX roleName ON userRole (roleName);

CREATE INDEX savedSearchQueryID ON savedSearch (savedSearchQueryID);

CREATE UNIQUE INDEX userProfileID ON savedSearch (userProfileID,searchName);

CREATE INDEX userProfileID ON userArticleView (userProfileID);

CREATE INDEX userProfileID ON annotation (userProfileID);

CREATE INDEX userProfileID ON articleCategoryFlagged (userProfileID);

CREATE UNIQUE INDEX userProfileID ON savedSearch (userProfileID,searchName);

CREATE INDEX userProfileID ON userLogin (userProfileID);

CREATE INDEX userProfileID ON userProfileRoleJoinTable (userProfileID);

CREATE INDEX userProfileID ON userSearch (userProfileID);

CREATE INDEX userProfileID ON annotationFlag (userProfileID);

CREATE UNIQUE INDEX userProfileURI ON userProfile (userProfileURI);

CREATE INDEX volumeID ON issue (volumeID);

CREATE UNIQUE INDEX volumeUri ON volume (volumeUri);

