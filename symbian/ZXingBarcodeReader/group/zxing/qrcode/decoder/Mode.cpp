/*
 *  Mode.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 19/05/2008.
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

#include <zxing/qrcode/decoder/Mode.h>
#include <zxing/common/Counted.h>
#include <zxing/ReaderException.h>
#include <zxing/qrcode/Version.h>
#include <sstream>

namespace zxing {
namespace qrcode {
using namespace std;

Mode Mode::TERMINATOR(0, 0, 0);
Mode Mode::NUMERIC(10, 12, 14);
Mode Mode::ALPHANUMERIC(9, 11, 13);
Mode Mode::BYTE(8, 16, 16);
Mode Mode::KANJI(8, 10, 12);

Mode::Mode(int cbv0_9, int cbv10_26, int cbv27) :
    characterCountBitsForVersions0To9_(cbv0_9), characterCountBitsForVersions10To26_(cbv10_26),
    characterCountBitsForVersions27AndHigher_(cbv27) {
}

Mode& Mode::forBits(int bits) {
  switch (bits) {
  case 0x0:
    return TERMINATOR;
  case 0x1:
    return NUMERIC;
  case 0x2:
    return ALPHANUMERIC;
  case 0x4:
    return BYTE;
  case 0x8:
    return KANJI;
  default:
    ostringstream s;
    s << "Illegal mode bits: " << bits;
    throw ReaderException(s.str().c_str());
  }
}

int Mode::getCharacterCountBits(Version *version) {
  int number = version->getVersionNumber();
  if (number <= 9) {
    return characterCountBitsForVersions0To9_;
  } else if (number <= 26) {
    return characterCountBitsForVersions10To26_;
  } else {
    return characterCountBitsForVersions27AndHigher_;
  }
}

}
}
