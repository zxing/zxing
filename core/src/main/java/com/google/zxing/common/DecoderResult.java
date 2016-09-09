/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.common;

import java.util.List;

/**
 * <p>Encapsulates the result of decoding a matrix of bits. This typically
 * applies to 2D barcode formats. For now it contains the raw bytes obtained,
 * as well as a String interpretation of those bytes, if applicable.</p>
 *
 * @author Sean Owen
 */
public final class DecoderResult {

  private final byte[] rawBytes;
  private int numBits;
  private final String text;
  private final List<byte[]> byteSegments;
  private final String ecLevel;
  private Integer errorsCorrected;
  private Integer erasures;
  private Object other;
  private final int structuredAppendParity;
  private final int structuredAppendSequenceNumber;

  public DecoderResult(byte[] rawBytes,
                       String text,
                       List<byte[]> byteSegments,
                       String ecLevel) {
    this(rawBytes, text, byteSegments, ecLevel, -1, -1);
  }

  public DecoderResult(byte[] rawBytes,
                       String text,
                       List<byte[]> byteSegments,
                       String ecLevel,
                       int saSequence,
                       int saParity) {
    this.rawBytes = rawBytes;
    this.numBits = rawBytes == null ? 0 : 8 * rawBytes.length;
    this.text = text;
    this.byteSegments = byteSegments;
    this.ecLevel = ecLevel;
    this.structuredAppendParity = saParity;
    this.structuredAppendSequenceNumber = saSequence;
  }

  /**
   * @return raw bytes representing the result, or {@code null} if not applicable
   */
  public byte[] getRawBytes() {
    return rawBytes;
  }

  /**
   * @return how many bits of {@link #getRawBytes()} are valid; typically 8 times its length
   * @since 3.3.0
   */
  public int getNumBits() {
    return numBits;
  }

  /**
   * @param numBits overrides the number of bits that are valid in {@link #getRawBytes()}
   * @since 3.3.0
   */
  public void setNumBits(int numBits) {
    this.numBits = numBits;
  }

  /**
   * @return text representation of the result
   */
  public String getText() {
    return text;
  }

  /**
   * @return list of byte segments in the result, or {@code null} if not applicable
   */
  public List<byte[]> getByteSegments() {
    return byteSegments;
  }

  /**
   * @return name of error correction level used, or {@code null} if not applicable
   */
  public String getECLevel() {
    return ecLevel;
  }

  /**
   * @return number of errors corrected, or {@code null} if not applicable
   */
  public Integer getErrorsCorrected() {
    return errorsCorrected;
  }

  public void setErrorsCorrected(Integer errorsCorrected) {
    this.errorsCorrected = errorsCorrected;
  }

  /**
   * @return number of erasures corrected, or {@code null} if not applicable
   */
  public Integer getErasures() {
    return erasures;
  }

  public void setErasures(Integer erasures) {
    this.erasures = erasures;
  }

  /**
   * @return arbitrary additional metadata
   */
  public Object getOther() {
    return other;
  }

  public void setOther(Object other) {
    this.other = other;
  }
  
  public boolean hasStructuredAppend() {
    return structuredAppendParity >= 0 && structuredAppendSequenceNumber >= 0;
  }
  
  public int getStructuredAppendParity() {
    return structuredAppendParity;
  }
  
  public int getStructuredAppendSequenceNumber() {
    return structuredAppendSequenceNumber;
  }
  
}