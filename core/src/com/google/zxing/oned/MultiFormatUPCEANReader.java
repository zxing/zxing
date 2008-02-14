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

import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

/**
 * <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
 * read all such formats, it is most efficent to use this implementation rather than invoke
 * individual readers.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class MultiFormatUPCEANReader extends AbstractOneDReader {

  /**
   * Reader implementations to which this implementation delegates, in the order
   * they will be attempted. Order is important.
   */
  private final UPCEANReader[] readers = new UPCEANReader[]{
      new EAN13Reader(), new UPCAReader(), new EAN8Reader(), new UPCEReader()
  };

  public Result decodeRow(int rowNumber, BitArray row) throws ReaderException {
    // Compute this location once and reuse it on multiple implementations
    int[] startGuardPattern = AbstractUPCEANReader.findStartGuardPattern(row);
    ReaderException saved = null;
    for (int i = 0; i < readers.length; i++) {
      try {
        return readers[i].decodeRow(rowNumber, row, startGuardPattern);
      } catch (ReaderException re) {
        saved = re;
      }
    }
    throw saved;
  }

}