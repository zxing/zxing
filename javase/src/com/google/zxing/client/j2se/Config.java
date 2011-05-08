/*
 * Copyright 2011 ZXing authors
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

package com.google.zxing.client.j2se;

import com.google.zxing.DecodeHintType;

import java.util.Hashtable;

final class Config {

  private Hashtable<DecodeHintType, Object> hints;
  private boolean tryHarder;
  private boolean pureBarcode;
  private boolean productsOnly;
  private boolean dumpResults;
  private boolean dumpBlackPoint;
  private boolean multi;
  private boolean brief;
  private boolean recursive;
  private int[] crop;
  private int threads = 1;

  Hashtable<DecodeHintType, Object> getHints() {
    return hints;
  }

  void setHints(Hashtable<DecodeHintType, Object> hints) {
    this.hints = hints;
  }

  boolean isTryHarder() {
    return tryHarder;
  }

  void setTryHarder(boolean tryHarder) {
    this.tryHarder = tryHarder;
  }

  boolean isPureBarcode() {
    return pureBarcode;
  }

  void setPureBarcode(boolean pureBarcode) {
    this.pureBarcode = pureBarcode;
  }

  boolean isProductsOnly() {
    return productsOnly;
  }

  void setProductsOnly(boolean productsOnly) {
    this.productsOnly = productsOnly;
  }

  boolean isDumpResults() {
    return dumpResults;
  }

  void setDumpResults(boolean dumpResults) {
    this.dumpResults = dumpResults;
  }

  boolean isDumpBlackPoint() {
    return dumpBlackPoint;
  }

  void setDumpBlackPoint(boolean dumpBlackPoint) {
    this.dumpBlackPoint = dumpBlackPoint;
  }

  boolean isMulti() {
    return multi;
  }

  void setMulti(boolean multi) {
    this.multi = multi;
  }

  boolean isBrief() {
    return brief;
  }

  void setBrief(boolean brief) {
    this.brief = brief;
  }

  boolean isRecursive() {
    return recursive;
  }

  void setRecursive(boolean recursive) {
    this.recursive = recursive;
  }

  int[] getCrop() {
    return crop;
  }

  void setCrop(int[] crop) {
    this.crop = crop;
  }

  int getThreads() {
    return threads;
  }

  void setThreads(int threads) {
    this.threads = threads;
  }
}
