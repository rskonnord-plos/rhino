/*
 * Copyright (c) 2006-2014 by Public Library of Science
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

import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;

/**
 * Service that deals with users
 */
public interface UserCrudService {

  /**
   * List all the users
   *
   * @return list of users
   * @throws IOException
   */
  public abstract Transceiver listUsers() throws IOException;

  /**
   * UserProfile object for a given authId
   *
   * @param authId authId
   * @return UserProfile object
   * @throws IOException
   */
  public abstract Transceiver read(String authId) throws IOException;

}