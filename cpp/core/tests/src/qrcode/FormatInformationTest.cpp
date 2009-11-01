/*
 *  FormatInformationTest.cpp
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

#include "FormatInformationTest.h"

#include "ErrorCorrectionLevelTest.h"
#include <zxing/ReaderException.h>
#include <zxing/qrcode/FormatInformation.h>
#include <zxing/qrcode/ErrorCorrectionLevel.h>

namespace zxing {
namespace qrcode {

CPPUNIT_TEST_SUITE_REGISTRATION(FormatInformationTest);

static void assertEquals(Ref<FormatInformation> a,
                         Ref<FormatInformation> b) {
  FormatInformation &aa = *a;
  FormatInformation &bb = *b;
  CPPUNIT_ASSERT_EQUAL(aa, bb);
}

void FormatInformationTest::testBitsDiffering() {
  CPPUNIT_ASSERT_EQUAL(0, FormatInformation::numBitsDiffering(1, 1));
  CPPUNIT_ASSERT_EQUAL(1, FormatInformation::numBitsDiffering(0, 2));
  CPPUNIT_ASSERT_EQUAL(2, FormatInformation::numBitsDiffering(1, 2));
  CPPUNIT_ASSERT_EQUAL(32, FormatInformation::numBitsDiffering(0xffffffff,
                       0));
}

void FormatInformationTest::testDecode() {
  // Normal case
  Ref<FormatInformation> expected =
    FormatInformation::decodeFormatInformation(0x2BED ^ 0x5412);
  CPPUNIT_ASSERT_EQUAL((unsigned char) 0x07, expected->getDataMask());
  CPPUNIT_ASSERT_EQUAL(&ErrorCorrectionLevel::Q,
                       &expected->getErrorCorrectionLevel());
  // where the code forgot the mask!
  assertEquals(expected,
               FormatInformation::decodeFormatInformation(0x2BED));

  // 1,2,3,4 bits difference
  assertEquals(expected,
               FormatInformation::decodeFormatInformation(0x2BEF ^ 0x5412));
  assertEquals(expected,
               FormatInformation::decodeFormatInformation(0x2BEE ^ 0x5412));
  assertEquals(expected,
               FormatInformation::decodeFormatInformation(0x2BEA ^ 0x5412));
  CPPUNIT_ASSERT_EQUAL(true, FormatInformation::
                       decodeFormatInformation(0x2BE2 ^ 0x5412) == 0);
}
}
}
