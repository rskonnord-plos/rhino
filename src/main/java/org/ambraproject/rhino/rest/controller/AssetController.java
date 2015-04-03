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

import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.AssetCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class AssetController extends RestController {

  private static final String ASSET_META_NAMESPACE = "/assets/";

  @Autowired
  private AssetCrudService assetCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_META_NAMESPACE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(ID_PARAM) String id)
      throws IOException {
    assetCrudService.readMetadata(AssetIdentity.create(id)).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_META_NAMESPACE, params = {"figure"}, method = RequestMethod.GET)
  public void readAsFigure(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(ID_PARAM) String id)
      throws IOException {
    assetCrudService.readFigureMetadata(AssetIdentity.create(id)).respond(request, response, entityGson);
  }

}
