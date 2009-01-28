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

package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

import java.util.Hashtable;
import java.util.Vector;

/**
 * @author kevin.osullivan@sita.aero
 */
public final class ITFBlackBox1TestCase extends AbstractBlackBoxTestCase {

  public ITFBlackBox1TestCase() {
    super("test/data/blackbox/itf-1", new MultiFormatReader(), BarcodeFormat.ITF);
    addTest(9, 12, 0.0f);
  }

  // TODO(dswitkin): This is only used for the mean time because ITF is not turned on by default.
  // The other formats are included here to make sure we don't recognize an ITF barcode as something
  // else. Unfortunately this list is fragile. The right thing to do is profile ITF for performance,
  // and if it doesn't impose significant overhead, turn it on by default. Then this method can be
  // removed completely.
  @Override
  protected Hashtable<DecodeHintType, Object> getHints() {
    Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
    Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>();
    vector.addElement(BarcodeFormat.UPC_A);
    vector.addElement(BarcodeFormat.UPC_E);
    vector.addElement(BarcodeFormat.EAN_13);
    vector.addElement(BarcodeFormat.EAN_8);
    vector.addElement(BarcodeFormat.CODE_39);
    vector.addElement(BarcodeFormat.CODE_128);
    vector.addElement(BarcodeFormat.ITF);
    vector.addElement(BarcodeFormat.QR_CODE);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
    return hints;
  }

}
