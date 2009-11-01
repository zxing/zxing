/*
 *  ModeTest.cpp
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

#include "ModeTest.h"
#include <zxing/qrcode/Version.h>

namespace zxing {
namespace qrcode {

CPPUNIT_TEST_SUITE_REGISTRATION(ModeTest);

void ModeTest::testForBits() {
  CPPUNIT_ASSERT_EQUAL(&Mode::TERMINATOR, &(Mode::forBits(0x00)));
  CPPUNIT_ASSERT_EQUAL(&Mode::NUMERIC, &(Mode::forBits(0x01)));
  CPPUNIT_ASSERT_EQUAL(&Mode::ALPHANUMERIC, &(Mode::forBits(0x02)));
  CPPUNIT_ASSERT_EQUAL(&Mode::BYTE, &(Mode::forBits(0x04)));
  CPPUNIT_ASSERT_EQUAL(&Mode::KANJI, &(Mode::forBits(0x08)));
  try {
    Mode::forBits(0x10);
    CPPUNIT_FAIL("should have thrown an exception");
  } catch (zxing::ReaderException ex) {
    // expected
  }
}

void ModeTest::testCharacterCount() {
  CPPUNIT_ASSERT_EQUAL(10, Mode::NUMERIC.getCharacterCountBits(Version::getVersionForNumber(5)));
  CPPUNIT_ASSERT_EQUAL(12, Mode::NUMERIC.getCharacterCountBits(Version::getVersionForNumber(26)));
  CPPUNIT_ASSERT_EQUAL(14, Mode::NUMERIC.getCharacterCountBits(Version::getVersionForNumber(40)));
  CPPUNIT_ASSERT_EQUAL(9, Mode::ALPHANUMERIC.getCharacterCountBits(Version::getVersionForNumber(6)));
  CPPUNIT_ASSERT_EQUAL(8, Mode::BYTE.getCharacterCountBits(Version::getVersionForNumber(7)));
  CPPUNIT_ASSERT_EQUAL(8, Mode::KANJI.getCharacterCountBits(Version::getVersionForNumber(8)));
}
}
}
