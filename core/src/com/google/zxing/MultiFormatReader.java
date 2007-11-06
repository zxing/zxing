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
    // TODO for now we are only support QR Code so this behaves accordingly. This needs to
    // become more sophisticated
    if (possibleFormats == null || possibleFormats.contains(BarcodeFormat.QR_CODE)) {
      return new QRCodeReader().decode(image, hints);
    } else {
      throw new ReaderException();
    }
  }

}
