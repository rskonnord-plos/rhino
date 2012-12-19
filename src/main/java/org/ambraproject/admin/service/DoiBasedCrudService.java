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

package org.ambraproject.admin.service;

import org.ambraproject.admin.identity.DoiBasedIdentity;
import org.ambraproject.filestore.FileStoreException;
import org.springframework.http.HttpStatus;

import java.io.InputStream;

/**
 * Super-interface to CRUD services within the article namespace.
 * <p/>
 * Methods are included here only if they share a common signature among services. See the extending interfaces for
 * documentation on the behavior of each method.
 */
public interface DoiBasedCrudService<I extends DoiBasedIdentity> {

  public abstract InputStream read(I id) throws FileStoreException;

  public abstract void delete(I id) throws FileStoreException;

  public static enum WriteMode {
    /**
     * Write data to a given ID whether or not something exists at that ID.
     */
    WRITE_ANY,

    /**
     * Write data to an unused ID. Throw an error if it would overwrite something.
     */
    CREATE_ONLY,

    /**
     * Overwrite existing data at a used ID. Throw an error if there is nothing at the ID.
     */
    UPDATE_ONLY;
  }

  /**
   * An indication of whether a request that uploaded data (typically, PUT) created new data or updated existing data.
   */
  public static enum WriteResult {
    /**
     * A new entity was created.
     */
    CREATED(HttpStatus.CREATED),

    /**
     * An old entity was changed.
     */
    UPDATED(HttpStatus.OK);

    private final HttpStatus status;

    private WriteResult(HttpStatus status) {
      this.status = status;
    }

    /**
     * An HTTP status code that describes the operation.
     *
     * @return the status
     */
    public HttpStatus getStatus() {
      return status;
    }
  }


}
