#ifndef __FORMAT_INFORMATION_H__
#define __FORMAT_INFORMATION_H__

/*
 *  FormatInformation.h
 *  zxing
 *
 *  Created by Christian Brunschen on 18/05/2008.
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

#include <zxing/qrcode/ErrorCorrectionLevel.h>
#include <zxing/common/Counted.h>
#include <iostream>

namespace zxing {
namespace qrcode {

class FormatInformation : public Counted {
private:
  static int FORMAT_INFO_MASK_QR;
  static int FORMAT_INFO_DECODE_LOOKUP[][2];
  static int N_FORMAT_INFO_DECODE_LOOKUPS;
  static int BITS_SET_IN_HALF_BYTE[];

  ErrorCorrectionLevel &errorCorrectionLevel_;
  unsigned char dataMask_;

  FormatInformation(int formatInfo);

public:
  static int numBitsDiffering(unsigned int a, unsigned int b);
  static Ref<FormatInformation> decodeFormatInformation(int rawFormatInfo);
  static Ref<FormatInformation> doDecodeFormatInformation(int rawFormatInfo);
  ErrorCorrectionLevel &getErrorCorrectionLevel();
  unsigned char getDataMask();
  friend bool operator==(const FormatInformation &a, const FormatInformation &b);
  friend std::ostream& operator<<(std::ostream& out, const FormatInformation& fi);
};
}
}

#endif // __FORMAT_INFORMATION_H__
