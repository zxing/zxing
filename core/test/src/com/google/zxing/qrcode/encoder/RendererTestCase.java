/**
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

package com.google.zxing.qrcode.encoder;

import junit.framework.TestCase;

//#include "file/base/file.h"
//#include "testing/base/gunit.h"
//#include "testing/base/benchmark.h"
//#include "wireless/qrcode/qrcode.h"
//#include "wireless/qrcode/qrcode_encoder.h"
//#include "wireless/qrcode/qrcode_renderer.h"

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author mysen@google.com (Chris Mysen) - ported from C++
 */
public final class RendererTestCase extends TestCase {
  public void testRenderAsPNG() {
    QRCode qr_code = new QRCode();
    assertTrue(Encoder.Encode(new ByteArray("http://www.google.com/"),
                              QRCode.EC_LEVEL_M, qr_code));
    String result;
    assertTrue(Renderer.RenderAsPNG(qr_code, 3, result));
    assertFalse(result.length() == 0);
    // We don't test the result image in this test.  We do that in
    // RegressionTest().
  }

  public void testRenderAsPNGFromData() {
    QRCode qr_code = new QRCode();
    assertTrue(Encoder.Encode(new ByteArray("http://www.google.com/"),
                              QRCode.EC_LEVEL_M, qr_code));
    String result1;
    assertTrue(Renderer.RenderAsPNG(qr_code, 3, result1));

    String result2;
    assertTrue(Renderer.RenderAsPNGFromData("http://www.google.com/",
						    QRCode.EC_LEVEL_M, 3,
						    result2));
    assertEquals(result1, result2);
  }

  // ec_level comes from QRCode.EC_LEVEL_[LMQH]
  static boolean Compare(final String bytes, final int ec_level,
                         final int cell_size, final String golden_base_name) {
    String result;
    assertTrue(Renderer.RenderAsPNGFromData(bytes, ec_level,
                                            cell_size, result));
    String golden_file_name = "test/data/qrcode_encode/" +
			      golden_base_name;
    String golden;
    File.ReadFileToStringOrDie(golden_file_name, golden);
    return golden == result;
  }

  // Golden images are generated with "qrcode_sample.cc".  The images
  // are checked with both eye balls and cell phones.
  public void testRegressionTest() {
    assertTrue(Compare("http://www.google.com/", QRCode.EC_LEVEL_M, 3,
		       "renderer-test-01.png"));
    assertTrue(Compare("12345", QRCode.EC_LEVEL_L, 2,
			"renderer-test-02.png"));
    // Test in Katakana in Shift_JIS.
    byte[] dat = {(byte)0x83,0x65,(byte)0x83,0x58,(byte)0x83,0x67};
    assertTrue(Compare(new String(dat), QRCode.EC_LEVEL_H, 5,
			"renderer-test-03.png"));
  }
}
