#ifndef __BIT_MATRIX_TEST_H__
#define __BIT_MATRIX_TEST_H__

/*
 *  BitMatrixTest.h
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
#include <zxing/common/BitMatrix.h>

namespace zxing {
class BitMatrixTest : public CPPUNIT_NS::TestFixture {
  CPPUNIT_TEST_SUITE(BitMatrixTest);
  CPPUNIT_TEST(testGetSet);
  CPPUNIT_TEST(testSetRegion);
  CPPUNIT_TEST(testGetBits);
  CPPUNIT_TEST(testGetRow1);
  CPPUNIT_TEST(testGetRow2);
  CPPUNIT_TEST(testGetRow3);
  CPPUNIT_TEST_SUITE_END();

public:
  BitMatrixTest();

protected:
  void testGetSet();
  void testSetRegion();
  void testGetBits();
  void testGetRow1();
  void testGetRow2();
  void testGetRow3();

private:
  void runBitMatrixGetRowTest(int width, int height);
};
}


#endif // __BIT_MATRIX_TEST_H__
