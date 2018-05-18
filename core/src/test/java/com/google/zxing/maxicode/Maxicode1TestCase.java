/*
 * Copyright 2016 ZXing authors
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
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

/**
 * Tests {@link MaxiCodeReader} against a fixed set of test images.
 */
public final class Maxicode1TestCase extends AbstractBlackBoxTestCase {

  public Maxicode1TestCase() {
    super("src/test/resources/blackbox/maxicode-1", new MultiFormatReader(), BarcodeFormat.MAXICODE);
    addTest(5, 5, 0.0f);
    //addTest(5, 5, 90.0f);
    //addTest(5, 5, 180.0f);
    //addTest(5, 5, 270.0f);
  }

}