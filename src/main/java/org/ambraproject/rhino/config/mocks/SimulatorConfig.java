package org.ambraproject.rhino.config.mocks;

/**
 * Created by jkrzemien on 8/15/14.
 */
public interface SimulatorConfig {

  String getDatastorePath();

  String getDatastoreDomain();

  String getMockDataFolder();

  String getContentRepoAddress();

  String getContentRepoBucketName();

  String getIngestionSrcFolder();

  String getIngestionDestFolder();

  boolean isCaptureMockData();

}
