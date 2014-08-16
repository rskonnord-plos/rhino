package org.ambraproject.rhino.config.mocks;

/**
 * Created by jkrzemien on 8/15/14.
 */

public class HibernateSimulatorConfig {

  private long id;
  private String datastorePath;
  private String datastoreDomain;
  private String mockDataFolder;
  private String contentRepoAddress;
  private String contentRepoBucketName;
  private String ingestionSrcFolder;
  private String ingestionDestFolder;
  private boolean captureMockData;

  public void setId(long id) {
    this.id = id;
  }

  public long getId() {
    return id;
  }

  public String getDatastorePath() {
    return datastorePath;
  }

  public String getDatastoreDomain() {
    return datastoreDomain;
  }

  public String getMockDataFolder() {
    return mockDataFolder;
  }

  public String getContentRepoAddress() {
    return contentRepoAddress;
  }

  public String getContentRepoBucketName() {
    return contentRepoBucketName;
  }

  public String getIngestionSrcFolder() {
    return ingestionSrcFolder;
  }

  public String getIngestionDestFolder() {
    return ingestionDestFolder;
  }

  public void setDatastorePath(String datastorePath) {
    this.datastorePath = datastorePath;
  }

  public void setDatastoreDomain(String datastoreDomain) {
    this.datastoreDomain = datastoreDomain;
  }

  public void setMockDataFolder(String mockDataFolder) {
    this.mockDataFolder = mockDataFolder;
  }

  public void setContentRepoAddress(String contentRepoAddress) {
    this.contentRepoAddress = contentRepoAddress;
  }

  public void setContentRepoBucketName(String contentRepoBucketName) {
    this.contentRepoBucketName = contentRepoBucketName;
  }

  public void setIngestionSrcFolder(String ingestionSrcFolder) {
    this.ingestionSrcFolder = ingestionSrcFolder;
  }

  public void setIngestionDestFolder(String ingestionDestFolder) {
    this.ingestionDestFolder = ingestionDestFolder;
  }

  public void setCaptureMockData(boolean captureMockData) {
    this.captureMockData = captureMockData;
  }

  public boolean isCaptureMockData() {
    return captureMockData;
  }
}

