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
 * @author dswitkin@google.com (Daniel Switkin)
 * @author srowen@google.com (Sean Owen)
 */
public final class MultiFormatOneDReader extends AbstractOneDReader {

  private final OneDReader[] readers = new OneDReader[]{
      new MultiFormatUPCEANReader(), new Code39Reader(), new Code128Reader()
  };

  public Result decodeRow(int rowNumber, BitArray row) throws ReaderException {
    ReaderException saved = null;
    for (int i = 0; i < readers.length; i++) {
      try {
        return readers[i].decodeRow(rowNumber, row);
      } catch (ReaderException re) {
        saved = re;
      }
    }
    throw saved;
  }

}