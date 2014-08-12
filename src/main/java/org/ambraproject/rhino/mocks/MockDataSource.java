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

package org.ambraproject.rhino.mocks;

import org.apache.commons.dbcp.BasicDataSource;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Created by jkrzemien on 8/6/14.
 */

public enum MockDataSource {

  SINGLETON;

  private final Server hsqlServer;
  private final BasicDataSource ds;

  private MockDataSource() {

    HsqlProperties hsqlProperties = new HsqlProperties();
    hsqlProperties.setProperty("hsqldb.lock_file", Boolean.TRUE.toString());

    this.hsqlServer = new Server();
    // hsqlServer.setLogWriter(null);
    // hsqlServer.setSilent(true);
    // hsqlServer.setPort(9091);
    hsqlServer.setDatabaseName(0, "rhinodb");
    hsqlServer.setDatabasePath(0, getClass().getResource("/sql").getFile()); //"file:/tmp/rhinodb"

    try {
      hsqlServer.setProperties(hsqlProperties);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ServerAcl.AclFormatException e) {
      e.printStackTrace();
    }
    hsqlServer.start();

    this.ds = new BasicDataSource();
    ds.setDriverClassName("org.hsqldb.jdbcDriver");
    ds.setUrl("jdbc:hsqldb:hsql://localhost/rhinodb;default_schema=true;get_column_name=false");
    ds.setUsername("sa");
    ds.setPassword("");
  }

  public DataSource get() {
    return ds;
  }
}


