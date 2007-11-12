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

import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.upc.UPCReader;

import java.util.Hashtable;

/**
 * <p>This implementation can detect barcodes in one of several formats within
 * an image, and then decode what it finds. This implementation supports all
 * barcode formats that this library supports.</p>
 *
 * <p>For now, only delegates to {@link QRCodeReader}.</p>
 *
 * @author srowen@google.com (Sean Owen), dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatReader implements Reader {

  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    return decode(image, null);
  }

  public Result decode(MonochromeBitmapSource image, Hashtable hints)
      throws ReaderException {
    Hashtable possibleFormats =
        hints == null ? null : (Hashtable) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    boolean tryUPC = false;
    boolean tryQR = false;
    
    if (possibleFormats == null) {
      tryUPC = true;
      tryQR = true;
    } else if (possibleFormats.contains(BarcodeFormat.UPC)) {
      tryUPC = true;
    } else if (possibleFormats.contains(BarcodeFormat.QR_CODE)) {
      tryQR = true;
    } else {
      throw new ReaderException();
    }
    
    // UPC is much faster to decode, so try it first.
    if (tryUPC) {
      try {
        return new UPCReader().decode(image, hints);
      } catch (ReaderException e) {
      }
    }
    
    // Then fall through to QR codes.
    if (tryQR) {
      try {
        return new QRCodeReader().decode(image, hints);
      } catch (ReaderException e) {
      }
    }
    
    throw new ReaderException();
  }

}
