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
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

import java.util.Hashtable;

/**
 * <p>Implements decoding of the UPC-A format.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class UPCAReader implements UPCEANReader {

  private final UPCEANReader ean13Reader = new EAN13Reader();

  public Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange) throws ReaderException {
    return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardRange));
  }

  public Result decodeRow(int rowNumber, BitArray row, Hashtable hints) throws ReaderException {
    return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, hints));
  }

  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    return maybeReturnResult(ean13Reader.decode(image));
  }

  public Result decode(MonochromeBitmapSource image, Hashtable hints) throws ReaderException {
    return maybeReturnResult(ean13Reader.decode(image, hints));
  }

  private static Result maybeReturnResult(Result result) throws ReaderException {
    String text = result.getText();
    if (text.charAt(0) == '0') {
      return new Result(text.substring(1), null, result.getResultPoints(), BarcodeFormat.UPC_A);
    } else {
      throw new ReaderException("Found EAN-13 code but was not a UPC-A code");
    }
  }

}