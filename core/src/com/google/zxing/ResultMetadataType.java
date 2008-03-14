/*
 * Copyright 2008 Google Inc.
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

package com.google.zxing;

/**
 * Represents some type of metadata about the result of the decoding that the decoder
 * wishes to communicate back to the caller.
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class ResultMetadataType {

  // No, we can't use an enum here. J2ME doesn't support it.

  /**
   * Unspecified, application-specific metadata. Maps to an unspecified {@link Object}.
   */
  public static final ResultMetadataType OTHER = new ResultMetadataType();

  /**
   * Denotes the likely approximate orientation of the barcode in the image. This value
   * is given as degrees rotated clockwise from the normal, upright orientation.
   * For example a 1D barcode which was found by reading top-to-bottom would be
   * said to have orientation "90". This key maps to an {@link Integer} whose
   * value is in the range [0,360).
   */
  public static final ResultMetadataType ORIENTATION = new ResultMetadataType();

  private ResultMetadataType() {
  }

}