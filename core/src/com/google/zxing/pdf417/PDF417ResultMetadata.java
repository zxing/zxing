package com.google.zxing.pdf417;

public final class PDF417ResultMetadata {

  private int segmentIndex;
  private String fileId;
  private int[] optionalData;
  private boolean lastSegment;

  private int correctedErrorsCount;
  private int erasureCount;

  public int getSegmentIndex() {
    return segmentIndex;
  }

  public void setSegmentIndex(int segmentIndex) {
    this.segmentIndex = segmentIndex;
  }

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public int[] getOptionalData() {
    return optionalData;
  }

  public void setOptionalData(int[] optionalData) {
    this.optionalData = optionalData;
  }

  public int getCorrectedErrorsCount() {
    return correctedErrorsCount;
  }

  public void setCorrectedErrorsCount(int correctedErrorsCount) {
    this.correctedErrorsCount = correctedErrorsCount;
  }

  public boolean isLastSegment() {
    return lastSegment;
  }

  public void setLastSegment(boolean lastSegment) {
    this.lastSegment = lastSegment;
  }

  public int getErasureCount() {
    return erasureCount;
  }

  public void setErasureCount(int erasureCount) {
    this.erasureCount = erasureCount;
  }
}