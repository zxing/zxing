/*
 * Copyright 2008 Google Inc.
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
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

import java.util.Hashtable;
import java.util.Vector;

/**
 * <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
 * read all such formats, it is most efficent to use this implementation rather than invoke
 * individual readers.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class MultiFormatUPCEANReader extends AbstractOneDReader {

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws ReaderException {
    Hashtable possibleFormats = hints == null ? null : (Hashtable) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    Vector readers = new Vector();
    if (possibleFormats != null) {
      if (possibleFormats.contains(BarcodeFormat.EAN_13)) {
        readers.addElement(new EAN13Reader());
      }
      if (possibleFormats.contains(BarcodeFormat.UPC_A)) {
        readers.addElement(new UPCAReader());
      }
      if (possibleFormats.contains(BarcodeFormat.EAN_8)) {
        readers.addElement(new EAN8Reader());
      }
      if (possibleFormats.contains(BarcodeFormat.UPC_E)) {
        readers.addElement(new UPCEReader());
      }
    }
    if (readers.isEmpty()) {
      readers.addElement(new EAN13Reader());      
      readers.addElement(new UPCAReader());
      readers.addElement(new EAN8Reader());
      readers.addElement(new UPCEReader());
    }

    // Compute this location once and reuse it on multiple implementations
    int[] startGuardPattern = AbstractUPCEANReader.findStartGuardPattern(row);
    for (int i = 0; i < readers.size(); i++) {
      UPCEANReader reader = (UPCEANReader) readers.elementAt(i);
      Result result;
      try {
        result = reader.decodeRow(rowNumber, row, startGuardPattern);
      } catch (ReaderException re) {
        continue;
      }
      // Special case: a 12-digit code encoded in UPC-A is identical to a "0"
      // followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
      // UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
      // Individually these are correct.
      //
      // These cases can't be distinguished, so we defer to the UPC-A decoder and
      // treat this case as a UPC-A code. But if we let the UPC-A decoder look at
      // symbols first, it will recognize any EAN-13 code as a 12-digit string,
      // which is wrong. So EAN-13 has to try first.
      //
      // Here is, therefore, where we implement this logic:
      if (result.getBarcodeFormat().equals(BarcodeFormat.EAN_13) &&
          result.getText().charAt(0) == '0') {
        return new Result(result.getText().substring(1), result.getResultPoints(), BarcodeFormat.UPC_A);
      }
      return result;
    }

    throw new ReaderException("No barcode was detected in this image.");
  }

}