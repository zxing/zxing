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

import com.google.zxing.oned.MultiFormatOneDReader;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.Hashtable;

/**
 * <p>This implementation can detect barcodes in one of several formats within
 * an image, and then decode what it finds. This implementation supports all
 * barcode formats that this library supports.</p>
 *
 * @author srowen@google.com (Sean Owen), dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatReader implements Reader {

  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    return decode(image, null);
  }

  public Result decode(MonochromeBitmapSource image, Hashtable hints)
      throws ReaderException {
    Hashtable possibleFormats = hints == null ? null : (Hashtable) hints.get(DecodeHintType.POSSIBLE_FORMATS);

    boolean tryOneD;
    boolean tryQR;
    if (possibleFormats == null) {
      tryOneD = true;
      tryQR = true;
    } else {
      tryOneD = possibleFormats.contains(BarcodeFormat.ONED);
      tryQR = possibleFormats.contains(BarcodeFormat.QR_CODE);
    }
    if (!(tryOneD || tryQR)) {
      throw new ReaderException("POSSIBLE_FORMATS specifies no supported types");
    }

    // UPC is much faster to decode, so try it first.
    if (tryOneD) {
      try {
        return new MultiFormatOneDReader().decode(image, hints);
      } catch (ReaderException re) {
      }
    }

    // Then fall through to QR codes.
    if (tryQR) {
      try {
        return new QRCodeReader().decode(image, hints);
      } catch (ReaderException re) {
      }
    }

    throw new ReaderException("No barcode was detected in this image.");
  }

}
