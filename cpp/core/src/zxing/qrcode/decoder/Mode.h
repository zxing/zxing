#ifndef __MODE_H__
#define __MODE_H__

/*
 *  Mode.h
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

#include <zxing/common/Counted.h>
#include <zxing/qrcode/Version.h>

namespace zxing {
namespace qrcode {

class Mode {
private:
  int characterCountBitsForVersions0To9_;
  int characterCountBitsForVersions10To26_;
  int characterCountBitsForVersions27AndHigher_;

  Mode(int cbv0_9, int cbv10_26, int cbv27);

public:
  static Mode TERMINATOR;
  static Mode NUMERIC;
  static Mode ALPHANUMERIC;
  static Mode BYTE;
  static Mode KANJI;

  static Mode& forBits(int bits);
  int getCharacterCountBits(Version *version);
};
}
}

#endif // __MODE_H__
