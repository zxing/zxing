#ifndef __BIT_ARRAY_TEST_H__
#define __BIT_ARRAY_TEST_H__

/*
 *  BitArrayTest.h
 *  zxing
 *
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

#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>
#include <zxing/common/BitArray.h>

namespace zxing {
class BitArrayTest : public CPPUNIT_NS::TestFixture {
  CPPUNIT_TEST_SUITE(BitArrayTest);
  CPPUNIT_TEST(testGetSet);
  CPPUNIT_TEST(testSetBulk);
  CPPUNIT_TEST(testClear);
  CPPUNIT_TEST(testGetArray);
  CPPUNIT_TEST(testIsRange);
  CPPUNIT_TEST(testReverseHalves);
  CPPUNIT_TEST(testReverseEven);
  CPPUNIT_TEST(testReverseOdd);
  CPPUNIT_TEST(testReverseSweep);
  CPPUNIT_TEST(testReverseReverse);
  CPPUNIT_TEST_SUITE_END();

public:

protected:
  void testGetSet();
  void testSetBulk();
  void testClear();
  void testGetArray();
  void testIsRange();
  void testReverseHalves();
  void testReverseEven();
  void testReverseOdd();
  void testReverseSweep();
  void testReverseReverse();

private:
  static void fillRandom(BitArray& test, BitArray& reference);
};
}

#endif // __BIT_ARRAY_TEST_H__
