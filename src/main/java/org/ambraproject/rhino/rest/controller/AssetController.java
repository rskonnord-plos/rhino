/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.IdentityService;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.ambraproject.rhino.service.impl.AmbraService.reportNotFound;

@Controller
public class AssetController extends RestController {

  private static final String ASSET_META_ROOT = "/assets";

  @Autowired
  private AssetCrudService assetCrudService;
  @Autowired
  private IdentityService identityService;
  @Autowired
  private ContentRepoService contentRepoService;

  private AssetIdentity parse(String id, Integer versionNumber, Integer revisionNumber) {
    if (revisionNumber == null) {
      return new AssetIdentity(id, Optional.fromNullable(versionNumber), Optional.<String>absent());
    } else {
      if (versionNumber != null) {
        throw new RestClientException("Cannot specify version and revision", HttpStatus.NOT_FOUND);
      }

      // TODO: Look up by revision and return with correct version

      return null;

    }
  }

  private AssetIdentity parse(String id, Integer versionNumber, Integer revisionNumber, String fileType) {
    if (revisionNumber == null) {
      return new AssetIdentity(id, Optional.fromNullable(versionNumber), Optional.<String>absent());
    } else {
      if (versionNumber != null) {
        throw new RestClientException("Cannot specify version and revision", HttpStatus.NOT_FOUND);
      }

      // Look up by revision and return with correct version
      DoiBasedIdentity assetId = DoiBasedIdentity.create(id);
      ArticleIdentity parentArticle = assetCrudService.getParentArticle(assetId);
      if (parentArticle == null) {
        throw new RestClientException("Asset ID not mapped to article", HttpStatus.NOT_FOUND);
      }

      AssetIdentity assetIdentity = identityService.parseAssetId(parentArticle, assetId, fileType, revisionNumber);

      // obs : when stopping using versionNumber to call the crepo, we can remove the call to obtain the metadata

      RepoObjectMetadata objMeta;
      try {
        objMeta = contentRepoService.getRepoObjectMetadata(
            RepoVersion.create(assetIdentity.getIdentifier(), assetIdentity.getUuid().get()));
      } catch (ContentRepoException e) {
        if (e.getErrorType() == ErrorType.ErrorFetchingObjectMeta) {
          throw reportNotFound(assetIdentity);
        } else {
          throw e;
        }
      }

      RepoVersionNumber objVersionNumb = objMeta.getVersionNumber();
      return new AssetIdentity(id,
          Optional.fromNullable(objVersionNumb.getNumber()),
          Optional.fromNullable(objMeta.getVersion().getUuid().toString()));


    }
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_META_ROOT, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = ID_PARAM, required = true) String id,
                   @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                   @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    assetCrudService.readMetadata(parse(id, versionNumber, revisionNumber)).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_META_ROOT, params = {"figure"}, method = RequestMethod.GET)
  public void readAsFigure(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = ID_PARAM, required = true) String id,
                           @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                           @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber,
                           @RequestParam(value = FILE_TYPE_PARAM, required = false) String fileType)
      throws IOException {
    assetCrudService.readFigureMetadata(parse(id, versionNumber, revisionNumber, fileType)).respond(request, response, entityGson);
  }

}
