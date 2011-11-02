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

package com.google.zxing.pdf417;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

/**
 * This test consists of perfect, computer-generated images. We should have 100% passing.
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
public final class PDF417BlackBox1TestCase extends AbstractBlackBoxTestCase {

  public PDF417BlackBox1TestCase() {
    super("test/data/blackbox/pdf417", new MultiFormatReader(), BarcodeFormat.PDF_417);
    addTest(3, 3, 0.0f);
    addTest(3, 3, 180.0f);
  }

}