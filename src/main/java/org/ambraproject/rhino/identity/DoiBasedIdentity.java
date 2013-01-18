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

package org.ambraproject.rhino.identity;

import com.google.common.base.Preconditions;

/**
 * An entity identifier based on a Digital Object Identifier (DOI). Instances of this class cover two cases: <ol>
 * <li>The entity is an article, in which case the {@link #getIdentifier() identifier} is the article's actual DOI that
 * would be known to public DOI resolvers.</li> <li>The entity is not an article, but has a "DOI-like" identifier. In
 * this case, the {@link #getIdentifier() identifier} is not a true DOI that a resolver would recognize. (However, such
 * asset IDs use the parent article's DOI as a prefix, so in that sense they are "DOI-based".)</li> </ol>
 */
public abstract class DoiBasedIdentity {

  protected static final String DOI_SCHEME_VALUE = "info:doi/";
  protected static final String XML_EXTENSION = "xml";

  private final String identifier; // non-null, non-empty, doesn't contain ':'

  protected DoiBasedIdentity(String identifier) {
    super();
    this.identifier = Preconditions.checkNotNull(identifier);

    Preconditions.checkArgument(identifier.indexOf(':') < 0, "DOI must not have scheme prefix (\"info:doi/\")");
    Preconditions.checkArgument(!identifier.isEmpty(), "DOI is an empty string");
  }


  /**
   * Return the DOI or DOI-like identifier for the article or asset that this object identifies. The return value will
   * not be prefixed with {@code "info:doi/"} and, under Ambra's current data model, must not be stored in a DOI column
   * of any database table.
   * <p/>
   * The value {@code this.getIdentifier()} represents an actual DOI (that a public DOI resolver would know about) if
   * and only if {@code this.isAsset()} returns {@code false}.
   *
   * @return the DOI or DOI-like identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Return a database key value to represent the article or asset that this object identifies. Under Ambra's current
   * data model, this is the string returned by {@link #getIdentifier} prefixed with {@code "info:doi/"}.
   *
   * @return the key value
   */
  public String getKey() {
    return DOI_SCHEME_VALUE + identifier;
  }

  @Override
  public String toString() {
    return getIdentifier();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DoiBasedIdentity that = (DoiBasedIdentity) o;

    if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return identifier != null ? identifier.hashCode() : 0;
  }

}