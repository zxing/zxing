// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
 *  LuminanceSource.cpp
 *  zxing
 *
 *  Copyright 2008 ZXing authors All rights reserved.
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

#include <zxing/LuminanceSource.h>
#include <zxing/common/IllegalArgumentException.h>  // for IllegalArgumentException

#include "zxing/common/Counted.h"                   // for Ref
#include "Utils/stringstreamlite.hpp"

#include <boost/assert.hpp>

namespace pping {

LuminanceSource::operator std::string() const MB_NOEXCEPT_EXCEPT_BADALLOC {
  unsigned char* row = 0;
  mb::stringstreamlite oss;
  for (int y = 0; y < getHeight(); y++) {
    row = getRow(y, row);
    for (int x = 0; x < getWidth(); x++) {
      int luminance = row[x] & 0xFF;
      char c;
      if (luminance < 0x40) {
        c = '#';
      } else if (luminance < 0x80) {
        c = '+';
      } else if (luminance < 0xC0) {
        c = '.';
      } else {
        c = ' ';
      }
      oss << c;
    }
    oss << '\n';
  }
  delete [] row;
  return oss.str();
}

}
