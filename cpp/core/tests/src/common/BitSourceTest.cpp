/*
 *  BitSourceTest.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 09/05/2008.
 *  Copyright 2008 Google UK. All rights reserved.
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

#include "BitSourceTest.h"

namespace zxing {

CPPUNIT_TEST_SUITE_REGISTRATION(BitSourceTest);

typedef char byte;

void BitSourceTest::testSource() {
  byte rawBytes[] = {(byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5};
  ArrayRef<byte> bytes(rawBytes, 5);
  BitSource source(bytes);
  CPPUNIT_ASSERT_EQUAL(40, source.available());
  CPPUNIT_ASSERT_EQUAL(0, source.readBits(1));
  CPPUNIT_ASSERT_EQUAL(39, source.available());
  CPPUNIT_ASSERT_EQUAL(0, source.readBits(6));
  CPPUNIT_ASSERT_EQUAL(33, source.available());
  CPPUNIT_ASSERT_EQUAL(1, source.readBits(1));
  CPPUNIT_ASSERT_EQUAL(32, source.available());
  CPPUNIT_ASSERT_EQUAL(2, source.readBits(8));
  CPPUNIT_ASSERT_EQUAL(24, source.available());
  CPPUNIT_ASSERT_EQUAL(12, source.readBits(10));
  CPPUNIT_ASSERT_EQUAL(14, source.available());
  CPPUNIT_ASSERT_EQUAL(16, source.readBits(8));
  CPPUNIT_ASSERT_EQUAL(6, source.available());
  CPPUNIT_ASSERT_EQUAL(5, source.readBits(6));
  CPPUNIT_ASSERT_EQUAL(0, source.available());
}
}
