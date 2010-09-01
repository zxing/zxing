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
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

import java.util.Hashtable;
import java.util.Vector;

/**
 * <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
 * read all such formats, it is most efficient to use this implementation rather than invoke
 * individual readers.</p>
 *
 * @author Sean Owen
 */
public final class MultiFormatUPCEANReader extends OneDReader {

  private final Vector readers;

  public MultiFormatUPCEANReader(Hashtable hints) {
    Vector possibleFormats = hints == null ? null :
        (Vector) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    readers = new Vector();
    if (possibleFormats != null) {
      if (possibleFormats.contains(BarcodeFormat.EAN_13)) {
        readers.addElement(new EAN13Reader());
      } else if (possibleFormats.contains(BarcodeFormat.UPC_A)) {
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
      // UPC-A is covered by EAN-13
      readers.addElement(new EAN8Reader());
      readers.addElement(new UPCEReader());
    }
  }

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws NotFoundException {
    // Compute this location once and reuse it on multiple implementations
    int[] startGuardPattern = UPCEANReader.findStartGuardPattern(row);
    int size = readers.size();
    for (int i = 0; i < size; i++) {
      UPCEANReader reader = (UPCEANReader) readers.elementAt(i);
      Result result;
      try {
        result = reader.decodeRow(rowNumber, row, startGuardPattern, hints);
      } catch (ReaderException re) {
        continue;
      }
      // Special case: a 12-digit code encoded in UPC-A is identical to a "0"
      // followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
      // UPC-A as a 12-digit string and EAN-13 as a 13-digit string starting with "0".
      // Individually these are correct and their readers will both read such a code
      // and correctly call it EAN-13, or UPC-A, respectively.
      //
      // In this case, if we've been looking for both types, we'd like to call it
      // a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
      // UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
      // result if appropriate.
      //
      // But, don't return UPC-A if UPC-A was not a requested format!
      boolean ean13MayBeUPCA =
          BarcodeFormat.EAN_13.equals(result.getBarcodeFormat()) &&
          result.getText().charAt(0) == '0';
      Vector possibleFormats = hints == null ? null : (Vector) hints.get(DecodeHintType.POSSIBLE_FORMATS);
      boolean canReturnUPCA = possibleFormats == null || possibleFormats.contains(BarcodeFormat.UPC_A);

      if (ean13MayBeUPCA && canReturnUPCA) {
        return new Result(result.getText().substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
      }
      return result;
    }

    throw NotFoundException.getNotFoundInstance();
  }

  public void reset() {
    int size = readers.size();
    for (int i = 0; i < size; i++) {
      Reader reader = (Reader) readers.elementAt(i);
      reader.reset();
    }
  }

}
