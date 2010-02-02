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
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

import java.util.Hashtable;

/**
 * <p>Implements decoding of the UPC-A format.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class UPCAReader extends UPCEANReader {

  private final UPCEANReader ean13Reader = new EAN13Reader();

  public Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange, Hashtable hints)
      throws NotFoundException, FormatException, ChecksumException {
    return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardRange, hints));
  }

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints)
      throws NotFoundException, FormatException, ChecksumException {
    return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, hints));
  }

  public Result decode(BinaryBitmap image) throws NotFoundException, FormatException {
    return maybeReturnResult(ean13Reader.decode(image));
  }

  public Result decode(BinaryBitmap image, Hashtable hints) throws NotFoundException, FormatException {
    return maybeReturnResult(ean13Reader.decode(image, hints));
  }

  BarcodeFormat getBarcodeFormat() {
    return BarcodeFormat.UPC_A;
  }

  protected int decodeMiddle(BitArray row, int[] startRange, StringBuffer resultString)
      throws NotFoundException {
    return ean13Reader.decodeMiddle(row, startRange, resultString);
  }

  private static Result maybeReturnResult(Result result) throws FormatException {
    String text = result.getText();
    if (text.charAt(0) == '0') {
      return new Result(text.substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
    } else {
      throw FormatException.getFormatInstance();
    }
  }

}
