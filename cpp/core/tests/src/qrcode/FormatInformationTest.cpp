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

static const int MASKED_TEST_FORMAT_INFO = 0x2BED;
static const int UNMASKED_TEST_FORMAT_INFO = MASKED_TEST_FORMAT_INFO ^ 0x5412;

static void assertEquals(Ref<FormatInformation> a,
                         Ref<FormatInformation> b) {
  if (a == NULL || b == NULL)
  {
	CPPUNIT_ASSERT_EQUAL(a, b);
  }
  else
  {
	FormatInformation &aa = *a;
	FormatInformation &bb = *b;
	CPPUNIT_ASSERT_EQUAL(aa, bb);
  }
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
    FormatInformation::decodeFormatInformation(MASKED_TEST_FORMAT_INFO, MASKED_TEST_FORMAT_INFO);
  CPPUNIT_ASSERT_EQUAL((unsigned char) 0x07, expected->getDataMask());
  CPPUNIT_ASSERT_EQUAL(&ErrorCorrectionLevel::Q,
                       &expected->getErrorCorrectionLevel());
  // where the code forgot the mask!
  assertEquals(expected,
               FormatInformation::decodeFormatInformation(UNMASKED_TEST_FORMAT_INFO, MASKED_TEST_FORMAT_INFO));

  //TODO separate tests as in Java?

  // 1,2,3,4 bits difference
  assertEquals(expected, FormatInformation::decodeFormatInformation(
	MASKED_TEST_FORMAT_INFO ^ 0x01, MASKED_TEST_FORMAT_INFO ^ 0x01));
  assertEquals(expected, FormatInformation::decodeFormatInformation(
	MASKED_TEST_FORMAT_INFO ^ 0x03, MASKED_TEST_FORMAT_INFO ^ 0x03));
  assertEquals(expected, FormatInformation::decodeFormatInformation(
	MASKED_TEST_FORMAT_INFO ^ 0x07, MASKED_TEST_FORMAT_INFO ^ 0x07));

  Ref<FormatInformation> expectedNull(NULL);
  assertEquals(expectedNull, FormatInformation::decodeFormatInformation(
	MASKED_TEST_FORMAT_INFO ^ 0x0F, MASKED_TEST_FORMAT_INFO ^ 0x0F));

  // WithMisread
  assertEquals(expected, FormatInformation::decodeFormatInformation(
	MASKED_TEST_FORMAT_INFO ^ 0x03, MASKED_TEST_FORMAT_INFO ^ 0x0F));
}
}
}
