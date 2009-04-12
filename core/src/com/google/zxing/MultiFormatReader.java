/*
 * Copyright 2007 ZXing authors
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
 * MultiFormatReader is a convenience class and the main entry point into the library for most uses.
 * By default it attempts to decode all barcode formats that the library supports. Optionally, you
 * can provide a hints object to request different behavior, for example only decoding QR codes.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatReader implements Reader {

  private Hashtable hints;
  private Vector readers;

  /**
   * This version of decode honors the intent of Reader.decode(MonochromeBitmapSource) in that it
   * passes null as a hint to the decoders. However, that makes it inefficient to call repeatedly.
   * Use setHints() followed by decodeWithState() for continuous scan applications.
   *
   * @param image The pixel data to decode
   * @return The contents of the image
   * @throws ReaderException Any errors which occurred
   */
  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    setHints(null);
    return decodeInternal(image);
  }

  /**
   * Decode an image using the hints provided. Does not honor existing state.
   *
   * @param image The pixel data to decode
   * @param hints The hints to use, clearing the previous state.
   * @return The contents of the image
   * @throws ReaderException Any errors which occurred
   */
  public Result decode(MonochromeBitmapSource image, Hashtable hints) throws ReaderException {
    setHints(hints);
    return decodeInternal(image);
  }

  /**
   * Decode an image using the state set up by calling setHints() previously. Continuous scan
   * clients will get a <b>large</b> speed increase by using this instead of decode().
   *
   * @param image The pixel data to decode
   * @return The contents of the image
   * @throws ReaderException Any errors which occurred
   */
  public Result decodeWithState(MonochromeBitmapSource image) throws ReaderException {
    // Make sure to set up the default state so we don't crash
    if (readers == null) {
      setHints(null);
    }
    return decodeInternal(image);
  }

  /**
   * This method adds state to the MultiFormatReader. By setting the hints once, subsequent calls
   * to decodeWithState(image) can reuse the same set of readers without reallocating memory. This
   * is important for performance in continuous scan clients.
   *
   * @param hints The set of hints to use for subsequent calls to decode(image)
   */
  public void setHints(Hashtable hints) {
    this.hints = hints;

    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
    Vector possibleFormats = hints == null ? null : (Vector) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    readers = new Vector();
    if (possibleFormats != null) {
      boolean addOneDReader =
          possibleFormats.contains(BarcodeFormat.UPC_A) ||
              possibleFormats.contains(BarcodeFormat.UPC_E) ||
              possibleFormats.contains(BarcodeFormat.EAN_13) ||
              possibleFormats.contains(BarcodeFormat.EAN_8) ||
              possibleFormats.contains(BarcodeFormat.CODE_39) ||
              possibleFormats.contains(BarcodeFormat.CODE_128) ||
              possibleFormats.contains(BarcodeFormat.ITF);
      // Put 1D readers upfront in "normal" mode
      if (addOneDReader && !tryHarder) {
        readers.addElement(new MultiFormatOneDReader(hints));
      }
      if (possibleFormats.contains(BarcodeFormat.QR_CODE)) {
        readers.addElement(new QRCodeReader());
      }
      if (possibleFormats.contains(BarcodeFormat.DATAMATRIX)) {
        readers.addElement(new DataMatrixReader());
      }
      // At end in "try harder" mode
      if (addOneDReader && tryHarder) {
        readers.addElement(new MultiFormatOneDReader(hints));
      }
    }
    if (readers.isEmpty()) {
      if (!tryHarder) {
        readers.addElement(new MultiFormatOneDReader(hints));
      }
      readers.addElement(new QRCodeReader());
      // TODO re-enable once Data Matrix is ready
      // readers.addElement(new DataMatrixReader());
      if (tryHarder) {
        readers.addElement(new MultiFormatOneDReader(hints));
      }
    }
  }

  private Result decodeInternal(MonochromeBitmapSource image) throws ReaderException {
    int size = readers.size();
    for (int i = 0; i < size; i++) {
      Reader reader = (Reader) readers.elementAt(i);
      try {
        return reader.decode(image, hints);
      } catch (ReaderException re) {
        // continue
      }
    }

    throw ReaderException.getInstance();
  }

}
