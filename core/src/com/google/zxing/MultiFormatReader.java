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
import com.google.zxing.datamatrix.DataMatrixReader;

import java.util.Hashtable;
import java.util.Vector;

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

  public Result decode(MonochromeBitmapSource image, Hashtable hints) throws ReaderException {

    Vector possibleFormats = hints == null ? null : (Vector) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    Vector readers = new Vector();
    if (possibleFormats != null) {
      if (possibleFormats.contains(BarcodeFormat.UPC_A) ||
          possibleFormats.contains(BarcodeFormat.UPC_E) ||
          possibleFormats.contains(BarcodeFormat.EAN_13) ||
          possibleFormats.contains(BarcodeFormat.EAN_8) ||
          possibleFormats.contains(BarcodeFormat.CODE_39) ||
          possibleFormats.contains(BarcodeFormat.CODE_128)) {
        readers.addElement(new MultiFormatOneDReader());
      }
      if (possibleFormats.contains(BarcodeFormat.QR_CODE)) {
        readers.addElement(new QRCodeReader());
      }
      if (possibleFormats.contains(BarcodeFormat.DATAMATRIX)) {
        readers.addElement(new DataMatrixReader());
      }
    }
    if (readers.isEmpty()) {
      readers.addElement(new MultiFormatOneDReader());
      readers.addElement(new QRCodeReader());
      readers.addElement(new DataMatrixReader());
    }

    for (int i = 0; i < readers.size(); i++) {
      Reader reader = (Reader) readers.elementAt(i);
      try {
        return reader.decode(image, hints);
      } catch (ReaderException re) {
        // continue
      }
    }

    throw new ReaderException("No barcode was detected in this image.");
  }

}
