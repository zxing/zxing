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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
 * read all such formats, it is most efficient to use this implementation rather than invoke
 * individual readers.</p>
 *
 * @author Sean Owen
 */
public final class MultiFormatUPCEANReader extends OneDReader {

  private final UPCEANReader[] readers;

  public MultiFormatUPCEANReader(Map<DecodeHintType,?> hints) {
    @SuppressWarnings("unchecked")
    Collection<BarcodeFormat> possibleFormats = hints == null ? null :
        (Collection<BarcodeFormat>) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    Collection<UPCEANReader> readers = new ArrayList<>();
    if (possibleFormats != null) {
      if (possibleFormats.contains(BarcodeFormat.EAN_13)) {
        readers.add(new EAN13Reader());
      } else if (possibleFormats.contains(BarcodeFormat.UPC_A)) {
        readers.add(new UPCAReader());
      }
      if (possibleFormats.contains(BarcodeFormat.EAN_8)) {
        readers.add(new EAN8Reader());
      }
      if (possibleFormats.contains(BarcodeFormat.UPC_E)) {
        readers.add(new UPCEReader());
      }
    }
    if (readers.isEmpty()) {
      readers.add(new EAN13Reader());
      // UPC-A is covered by EAN-13
      readers.add(new EAN8Reader());
      readers.add(new UPCEReader());
    }
    this.readers = readers.toArray(new UPCEANReader[readers.size()]);
  }

  @Override
  public Result decodeRow(int rowNumber,
                          BitArray row,
                          Map<DecodeHintType,?> hints) throws NotFoundException {
    // Compute this location once and reuse it on multiple implementations
    int[] startGuardPattern = UPCEANReader.findStartGuardPattern(row);
    for (UPCEANReader reader : readers) {
      try {
        Result result = reader.decodeRow(rowNumber, row, startGuardPattern, hints);
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
            result.getBarcodeFormat() == BarcodeFormat.EAN_13 &&
                result.getText().charAt(0) == '0';
        @SuppressWarnings("unchecked")
        Collection<BarcodeFormat> possibleFormats =
            hints == null ? null : (Collection<BarcodeFormat>) hints.get(DecodeHintType.POSSIBLE_FORMATS);
        boolean canReturnUPCA = possibleFormats == null || possibleFormats.contains(BarcodeFormat.UPC_A);
  
        if (ean13MayBeUPCA && canReturnUPCA) {
          // Transfer the metdata across
          Result resultUPCA = new Result(result.getText().substring(1),
                                         result.getRawBytes(),
                                         result.getResultPoints(),
                                         BarcodeFormat.UPC_A);
          resultUPCA.putAllMetadata(result.getResultMetadata());
          return resultUPCA;
        }
        return result;
      } catch (ReaderException ignored) {
        // continue
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  @Override
  public void reset() {
    for (Reader reader : readers) {
      reader.reset();
    }
  }

}
