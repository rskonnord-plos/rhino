/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

DROP TABLE `doiAssociation`;

CREATE TABLE `article` (
  `articleId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `doi` VARCHAR(150) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleId`),
  UNIQUE KEY `doi` (`doi`));

CREATE TABLE `journal` (
  `journalId` bigint(20) NOT NULL AUTO_INCREMENT,
  `currentIssueId` bigint(20) DEFAULT NULL,
  `journalKey` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `eIssn` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`journalId`),
  KEY `journal_ibfk_1_idx` (`currentIssueId`));

CREATE TABLE `articleIngestion` (
  `ingestionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `articleId` BIGINT(20) NOT NULL,
  `journalId` BIGINT(20) NOT NULL,
  `ingestionNumber` INT NOT NULL,
  `title` TEXT NOT NULL,
  `publicationDate` DATE NOT NULL,
  `revisionDate` DATE DEFAULT NULL,
  `publicationStage` VARCHAR(100) DEFAULT NULL,
  `articleType` VARCHAR(100) DEFAULT NULL,
  `strikingImageItemId` BIGINT(20) DEFAULT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ingestionId`),
  UNIQUE KEY `article_ingestnum` (`articleId`,`ingestionNumber`),
  CONSTRAINT `fk_articleIngestion_1`
    FOREIGN KEY (`articleId`)
    REFERENCES `article` (`articleId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleIngestion_2`
    FOREIGN KEY (`journalId`)
    REFERENCES `journal` (`journalId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `articleItem` (
  `itemId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `doi` VARCHAR(150) NOT NULL,
  `articleItemType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`itemId`),
  KEY `doi` (`doi`),
  CONSTRAINT `fk_articleItem_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
ALTER TABLE `articleIngestion`
  ADD CONSTRAINT `fk_articleIngestion_3`
    FOREIGN KEY (`strikingImageItemId`)
    REFERENCES `articleItem` (`itemId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

CREATE TABLE `articleFile` (
  `fileId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `itemId` BIGINT(20) NULL,
  `fileType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci',
  `bucketName` VARCHAR(255) NOT NULL,
  `crepoKey` VARCHAR(255) NOT NULL,
  `crepoUuid` VARCHAR(36) NOT NULL,
  `fileSize` BIGINT NOT NULL,
  `ingestedFileName` VARCHAR(255) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fileId`),
  UNIQUE KEY `ingest_item_filetype` (`ingestionId`,`itemId`,`fileType`),
  UNIQUE KEY `ingest_ingestedFileName` (`ingestionId`,`ingestedFileName`),
  CONSTRAINT `fk_articleFile_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleFile_2`
    FOREIGN KEY (`itemId`)
    REFERENCES `articleItem` (`itemId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  UNIQUE KEY `crepoUuid_UNIQUE` (`crepoUuid`));
  
CREATE TABLE `articleRevision` (
  `revisionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `revisionNumber` INT NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`revisionId`),
  CONSTRAINT `fk_articleRevision_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `syndication` (
  `syndicationId` bigint(20) NOT NULL AUTO_INCREMENT,
  `revisionId` BIGINT(20) NOT NULL,
  `targetQueue` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `status` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `submissionCount` int(11) DEFAULT NULL,
  `errorMessage` longtext CHARACTER SET utf8 COLLATE utf8_bin,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastSubmitTimestamp` timestamp NULL DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`syndicationId`),
  UNIQUE KEY `revisionId` (`revisionId`,`targetQueue`),
  CONSTRAINT `fk_syndication_1`
  FOREIGN KEY (`revisionId`)
  REFERENCES `articleRevision` (`revisionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `articleRelationship` (
  `articleRelationshipId` bigint(20) NOT NULL AUTO_INCREMENT,
  `sourceArticleId` bigint(20) NOT NULL,
  `targetArticleId` bigint(20) NOT NULL,
  `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleRelationshipId`),
  KEY `sourceArticleId` (`sourceArticleId`),
  KEY `targetArticleId` (`targetArticleId`),
  CONSTRAINT `fk_articleRelationship_1` FOREIGN KEY (`sourceArticleId`) REFERENCES `article` (`articleId`),
  CONSTRAINT `fk_articleRelationship_2` FOREIGN KEY (`targetArticleId`) REFERENCES `article` (`articleId`));

CREATE TABLE `articleCategoryAssignment` (
  `articleId` bigint(20) NOT NULL,
  `categoryId` bigint(20) NOT NULL,
  `weight` int(11) NOT NULL,
  PRIMARY KEY (`articleId`,`categoryId`),
  KEY `articleId` (`articleId`),
  KEY `categoryId` (`categoryId`),
  CONSTRAINT `fk_articleCategoryAssignment_1`
  FOREIGN KEY (`articleId`)
  REFERENCES `article` (`articleId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleCategoryAssignment_2`
  FOREIGN KEY (`categoryId`)
  REFERENCES `category` (`categoryId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `articleCategoryAssignmentFlag` (
  `flagId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `articleId` bigint(20) NOT NULL,
  `categoryId` bigint(20) NOT NULL,
  `userProfileId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`flagId`),
  KEY `articleId` (`articleId`),
  KEY `categoryId` (`categoryId`),
  CONSTRAINT `fk_articleCategoryAssignmentFlag_1`
  FOREIGN KEY (`articleId`,`categoryId`)
  REFERENCES `articleCategoryAssignment` (`articleId`,`categoryId`)
    ON DELETE CASCADE
    ON UPDATE NO ACTION);


CREATE TABLE `comment` (
  `commentId` bigint(20) NOT NULL AUTO_INCREMENT,
  `commentURI` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `articleId` bigint(20) NOT NULL,
  `parentId` bigint(20) DEFAULT NULL,
  `userProfileId` bigint(20) NOT NULL,
  `title` text CHARACTER SET utf8 COLLATE utf8_bin,
  `body` text CHARACTER SET utf8 COLLATE utf8_bin,
  `competingInterestBody` text CHARACTER SET utf8 COLLATE utf8_bin,
  `highlightedText` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isRemoved` bit(1) DEFAULT b'0',
  PRIMARY KEY (`commentId`),
  UNIQUE KEY `commentURI` (`commentURI`),
  CONSTRAINT `fk_comment_1` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`),
  CONSTRAINT `fk_comment_2` FOREIGN KEY (`parentId`)  REFERENCES `comment` (`commentId`),
  KEY `userProfileID` (`userProfileID`));

CREATE TABLE `commentFlag` (
  `commentFlagId` bigint(20) NOT NULL AUTO_INCREMENT,
  `commentId` bigint(20) NOT NULL,
  `userProfileId` bigint(20) NOT NULL,
  `reason` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `comment` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`commentFlagId`),
  KEY `commentId` (`commentId`),
  KEY `userProfileId` (`userProfileId`),
  CONSTRAINT `fk_commentFlag_1` FOREIGN KEY (`commentId`) REFERENCES `comment` (`commentId`));

CREATE TABLE `volume` (
  `volumeId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `journalId` bigint(20) NOT NULL,
  `journalSortOrder` int(11) NOT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`volumeId`),
  UNIQUE KEY `doi` (`doi`),
  KEY `journalID` (`journalId`),
  CONSTRAINT `fk_volume_1` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalId`));

CREATE TABLE `issue` (
  `issueId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `volumeId` bigint(20) NOT NULL,
  `volumeSortOrder` int(11) NOT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `imageArticleId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issueId`),
  UNIQUE KEY `doi` (`doi`),
  KEY `volumeId` (`volumeId`),
  CONSTRAINT `fk_issue_1` FOREIGN KEY (`volumeId`) REFERENCES `volume` (`volumeId`),
  CONSTRAINT `fk_issue_2` FOREIGN KEY (`imageArticleId`) REFERENCES `article` (`articleId`));

ALTER TABLE `journal`
ADD CONSTRAINT `fk_journal_1` FOREIGN KEY (`currentIssueId`) REFERENCES `issue` (`issueId`);

CREATE TABLE `articleList` (
  `articleListId` bigint(20) NOT NULL AUTO_INCREMENT,
  `listKey` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `displayName` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL,
  `journalId` bigint(20) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `listType` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  PRIMARY KEY (`articleListId`),
  UNIQUE KEY `listIdentity` (`journalId`,`listType`,`listKey`),
  KEY `journalId` (`journalId`),
  CONSTRAINT `fk_articleList_1` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalID`));

CREATE TABLE `articleListJoinTable` (
  `articleListId` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleId` bigint(20) NOT NULL,
  PRIMARY KEY (`articleListId`,`sortOrder`),
  KEY `articleId` (`articleId`),
  CONSTRAINT `fk_articleListJoinTable_1` FOREIGN KEY (`articleListId`) REFERENCES `articleList` (`articleListId`),
  CONSTRAINT `fk_articleListJoinTable_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`));

CREATE TABLE `issueArticleList` (
  `issueId` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleId` bigint(20) NOT NULL,
  PRIMARY KEY (`issueId`,`articleId`),
  CONSTRAINT `fk_issueArticleList_1` FOREIGN KEY (`issueId`) REFERENCES `issue` (`issueId`),
  CONSTRAINT `fk_issueArticleList_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`));

ALTER TABLE category CHANGE categoryID categoryId bigint(20) NOT NULL AUTO_INCREMENT;
ALTER TABLE category CHANGE created created timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE category CHANGE lastModified lastModified timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON
UPDATE CURRENT_TIMESTAMP;