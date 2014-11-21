/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 *    http://plos.org
 *    http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.config;

import java.net.URI;

/**
 * Configuration for the server.  This will slowly replace the configuration values in ambra.xml and contain values that
 * are required to start up the server and its behavior.
 */
public class YamlConfiguration implements RuntimeConfiguration {

  // TODO add a validate function that can check for
  // * required values and throw meaning errors when they are not present
  // * supply meaning default values

  private final UserFields uf;

  public YamlConfiguration(UserFields uf) {
    // if the yaml file doesn't contain anything, UserFields object will be null

    if (uf == null) {
      this.uf = new UserFields();
    } else {
      this.uf = uf;
    }
  }

  public static class UserFields {

    private boolean prettyPrintJson = true; // the default value should be true
    private ContentRepoEndpoints contentRepo;

    public void setPrettyPrintJson(boolean prettyPrintJson) {
      this.prettyPrintJson = prettyPrintJson;
    }

    public void setContentRepo(ContentRepoEndpoints contentRepo) {
      this.contentRepo = contentRepo;
    }

    public static class ContentRepoEndpoints {
      private ContentRepoEndpointImpl editorial; // upstairs
      private ContentRepoEndpointImpl corpus;  // downstairs

      public void setEditorial(ContentRepoEndpointImpl editorial) {
        this.editorial = editorial;
      }

      public void setCorpus(ContentRepoEndpointImpl corpus) {
        this.corpus = corpus;
      }
    }

    public static class ContentRepoEndpointImpl {
      private URI address;
      private String bucket;

      public void setAddress(URI address) {
        this.address = address;
      }

      public void setBucket(String bucket) {
        this.bucket = bucket;
      }

      /**
       * Avoid returning the ContentRepoEndpointImpl object, because it has public setters for compatibility with
       * org.yaml.snakeyaml.Yaml.loadAs that we don't want to be reachable from elsewhere.
       */
      private final ContentRepoEndpoint immutableView = new ContentRepoEndpoint() {
        @Override
        public URI getAddress() {
          return address;
        }

        @Override
        public String getBucket() {
          return bucket;
        }
      };
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean prettyPrintJson() {
    return uf.prettyPrintJson;
  }

  @Override
  public ContentRepoEndpoint getCorpusBucket() {
    return (uf.contentRepo == null) ? null
        : (uf.contentRepo.corpus == null) ? null
        : uf.contentRepo.corpus.immutableView;
  }

  @Override
  public ContentRepoEndpoint getEditorialBucket() {
    return (uf.contentRepo == null) ? null
        : (uf.contentRepo.editorial == null) ? null
        : uf.contentRepo.editorial.immutableView;
  }
}
