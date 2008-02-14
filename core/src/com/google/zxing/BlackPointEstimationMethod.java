/*
 * Copyright 2007 Google Inc.
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
 * <p>Enumerates different methods of sampling an imagine to estimate a black point.</p>
 *
 * @author srowen@google.com (Sean Owen), dswitkin@google.com (Daniel Switkin)
 */
public final class BlackPointEstimationMethod {

  /**
   * Method probably most suitable for use with 2D barcdoe format.
   */
  public static final BlackPointEstimationMethod TWO_D_SAMPLING = new BlackPointEstimationMethod();
  /**
   * Method probably most suitable for 1D barcode decoding, where one row at a time is sampled.
   */
  public static final BlackPointEstimationMethod ROW_SAMPLING = new BlackPointEstimationMethod();

  private BlackPointEstimationMethod() {
    // do nothing
  }

}