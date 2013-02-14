/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.identity;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Tests for {@link AssetFileIdentity}.
 */
public class AssetFileIdentityTest {

  @Test
  public void testGetContextElement() {
    assertEquals(AssetFileIdentity.create(
        "info:doi/10.1371/journal.pone.0056866.t003", "TIF").getContextElement(), "table-wrap");
    assertEquals(AssetFileIdentity.create(
        "info:doi/10.1371/journal.pone.0056866.g010", "TIF").getContextElement(), "fig");

    assertNull(AssetFileIdentity.create(
        "info:doi/10.1371/journal.pone.0056866", "XML").getContextElement());
  }
}