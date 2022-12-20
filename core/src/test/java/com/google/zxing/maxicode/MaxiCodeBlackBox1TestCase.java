/*
 * Copyright 2022 ZXing authors
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

package com.google.zxing.maxicode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

/**
 * Tests all characters in Set A.
 *
 * @author Daniel Gredler
 * @see <a href="https://github.com/zxing/zxing/issues/1543">Defect 1543</a>
 */
public final class MaxiCodeBlackBox1TestCase extends AbstractBlackBoxTestCase {

  public MaxiCodeBlackBox1TestCase() {
    super("src/test/resources/blackbox/maxicode-1", new MultiFormatReader(), BarcodeFormat.MAXICODE);
    addHint(DecodeHintType.PURE_BARCODE);
    addTest(1, 1, 0.0f);
  }

}
