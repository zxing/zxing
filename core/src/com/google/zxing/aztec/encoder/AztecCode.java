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

package com.google.zxing.aztec.encoder;

import com.google.zxing.common.BitMatrix;

/**
 * Aztec 2D code representation
 * 
 * @author Rustam Abdullaev
 */
public final class AztecCode {

  private boolean compact;
  private int size;
  private int layers;
  private int codeWords;
  private BitMatrix matrix;

  /**
   * Compact or full symbol indicator
   */
  public boolean isCompact() {
    return compact;
  }

  public void setCompact(boolean compact) {
    this.compact = compact;
  }

  /**
   * Size in pixels (width and height)
   */
  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }
  
  /**
   * Number of levels
   */
  public int getLayers() {
    return layers;
  }
  
  public void setLayers(int layers) {
    this.layers = layers;
  }

  /**
   * Number of data codewords
   */
  public int getCodeWords() {
    return codeWords;
  }

  public void setCodeWords(int codeWords) {
    this.codeWords = codeWords;
  }

  /**
   * The symbol image
   */
  public BitMatrix getMatrix() {
    return matrix;
  }

  public void setMatrix(BitMatrix matrix) {
    this.matrix = matrix;
  }

}
