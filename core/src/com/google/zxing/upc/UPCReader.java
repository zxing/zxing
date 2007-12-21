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

package com.google.zxing.upc;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import java.util.Hashtable;

/**
 * A reader which decodes UPC-A barcodes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class UPCReader implements Reader {

  /**
   * Locates and decodes a UPC barcode in an image.
   *
   * @return a String representing the digits found
   * @throws ReaderException if a barcode cannot be found or decoded
   */
  public Result decode(MonochromeBitmapSource image) throws ReaderException {
    return decode(image, null);
  }

  public Result decode(MonochromeBitmapSource image, Hashtable hints)
      throws ReaderException {
    UPCDecoder decoder = new UPCDecoder(image);
    String result = decoder.decode();
    if (result == null || result.length() == 0) {
      throw new ReaderException("No UPC barcode found");
    }
    return new Result(result, decoder.getPoints());
  }

}
