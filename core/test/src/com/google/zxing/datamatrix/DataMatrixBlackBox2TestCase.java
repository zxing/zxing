/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.datamatrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DataMatrixBlackBox2TestCase extends AbstractBlackBoxTestCase {

  public DataMatrixBlackBox2TestCase() {
    super("test/data/blackbox/datamatrix-2", new MultiFormatReader(), BarcodeFormat.DATA_MATRIX);
    addTest(10, 10, 0.0f);
    addTest(13, 13, 90.0f);
    addTest(16, 16, 180.0f);
    addTest(13, 13, 270.0f);
  }

}
