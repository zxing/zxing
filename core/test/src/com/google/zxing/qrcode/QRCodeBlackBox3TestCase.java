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

package com.google.zxing.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class QRCodeBlackBox3TestCase extends AbstractBlackBoxTestCase {

  public QRCodeBlackBox3TestCase() {
    super("test/data/blackbox/qrcode-3", new MultiFormatReader(), BarcodeFormat.QR_CODE);
    addTest(38, 38, 0.0f);
    addTest(38, 38, 90.0f);
    addTest(36, 36, 180.0f);
    addTest(38, 38, 270.0f);
  }

}
