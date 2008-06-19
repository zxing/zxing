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

package com.google.zxing.datamatrix.detector;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.DetectorResult;

/**
 * <p>Encapsulates logic that can detect a Data Matrix Code in an image, even if the Data Matrix Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author bbrown@google.com (Brian Brown)
 */
public final class Detector {

  private final MonochromeBitmapSource image;

  public Detector(MonochromeBitmapSource image) {
    this.image = image;
  }

  /**
   * <p>Detects a Data Matrix Code in an image, simply.</p>
   *
   * @return {@link DetectorResult} encapsulating results of detecting a QR Code
   * @throws ReaderException if no Data Matrix Code can be found
   */
  public DetectorResult detect() {
    // TODO
    return new DetectorResult(null, null);
  }


}
