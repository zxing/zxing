// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  Copyright 2010 ZXing authors All rights reserved.
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

#include "EAN8Reader.h"
#include <zxing/ReaderException.h>

using std::vector;
using zxing::oned::EAN8Reader;

// VC++
using zxing::Ref;
using zxing::BitArray;

EAN8Reader::EAN8Reader() : decodeMiddleCounters(4, 0) {}

int EAN8Reader::decodeMiddle(Ref<BitArray> row,
                             Range const& startRange,
                             std::string& result){
  vector<int>& counters (decodeMiddleCounters);
  counters[0] = 0;
  counters[1] = 0;
  counters[2] = 0;
  counters[3] = 0;

  int end = row->getSize();
  int rowOffset = startRange[1];

  for (int x = 0; x < 4 && rowOffset < end; x++) {
    int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
    result.append(1, (char) ('0' + bestMatch));
    for (int i = 0, end = counters.size(); i < end; i++) {
      rowOffset += counters[i];
    }
  }

  Range middleRange =
    findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
  rowOffset = middleRange[1];
  for (int x = 0; x < 4 && rowOffset < end; x++) {
    int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
    result.append(1, (char) ('0' + bestMatch));
    for (int i = 0, end = counters.size(); i < end; i++) {
      rowOffset += counters[i];
    }
  }
  return rowOffset;
}

zxing::BarcodeFormat EAN8Reader::getBarcodeFormat(){
  return BarcodeFormat::EAN_8;
}
