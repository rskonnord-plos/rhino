/*
 * Copyright (c) 2006-2013 by Public Library of Science
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

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;

public interface PingbackReadService {

  public static enum OrderBy {DATE, COUNT;}

  public abstract void listByArticle(ResponseReceiver receiver, MetadataFormat format, OrderBy orderBy) throws IOException;

  public abstract void read(ResponseReceiver receiver, ArticleIdentity article, MetadataFormat format) throws IOException;

}