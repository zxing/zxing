/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.pdf417;

/**
 * @author Guenther Grau
 */
public final class PDF417ResultMetadata {

  private int segmentIndex;
  private String fileId;
  private int[] optionalData;
  private boolean lastSegment;

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

  public boolean isLastSegment() {
    return lastSegment;
  }

  public void setLastSegment(boolean lastSegment) {
    this.lastSegment = lastSegment;
  }

}
