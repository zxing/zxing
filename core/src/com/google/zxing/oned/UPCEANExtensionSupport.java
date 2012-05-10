/*
 * Copyright (C) 2010 ZXing authors
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

import com.google.zxing.NotFoundException;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.BitArray;

final class UPCEANExtensionSupport {

  private static final int[] EXTENSION_START_PATTERN = {1,1,2};

  private final UPCEANExtension2Support twoSupport = new UPCEANExtension2Support();
  private final UPCEANExtension5Support fiveSupport = new UPCEANExtension5Support();

  Result decodeRow(int rowNumber, BitArray row, int rowOffset) throws NotFoundException {
    int[] extensionStartRange = UPCEANReader.findGuardPattern(row, rowOffset, false, EXTENSION_START_PATTERN);
    try {
      return fiveSupport.decodeRow(rowNumber, row, extensionStartRange);
    } catch (ReaderException re) {
      return twoSupport.decodeRow(rowNumber, row, extensionStartRange);
    }
  }

}
