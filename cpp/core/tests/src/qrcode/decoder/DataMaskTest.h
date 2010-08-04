#ifndef __DATA_MASK_TEST_H__
#define __DATA_MASK_TEST_H__

/*
 *  DataMaskTest.h
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
#include <zxing/qrcode/decoder/DataMask.h>
#include <zxing/common/BitMatrix.h>

namespace zxing {
namespace qrcode {

class MaskCondition {
public:
  MaskCondition() { }
  virtual bool isMasked(int i, int j) = 0;
  virtual ~MaskCondition() { }
};


class DataMaskTest : public CPPUNIT_NS::TestFixture {
  CPPUNIT_TEST_SUITE(DataMaskTest);
  CPPUNIT_TEST(testMask0);
  CPPUNIT_TEST(testMask1);
  CPPUNIT_TEST(testMask2);
  CPPUNIT_TEST(testMask3);
  CPPUNIT_TEST(testMask4);
  CPPUNIT_TEST(testMask5);
  CPPUNIT_TEST(testMask6);
  CPPUNIT_TEST(testMask7);
  CPPUNIT_TEST_SUITE_END();

public:

protected:
  void testMask0();
  void testMask1();
  void testMask2();
  void testMask3();
  void testMask4();
  void testMask5();
  void testMask6();
  void testMask7();

private:
  void testMaskAcrossDimensions(int reference,
                                MaskCondition &condition) {
    DataMask& mask = DataMask::forReference(reference);
    for (int version = 1; version <= 40; version++) {
      int dimension = 17 + 4 * version;
      testMask(mask, dimension, condition);
    }
  }

  void testMask(DataMask& mask, int dimension, MaskCondition &condition) {
    BitMatrix bits(dimension);
    mask.unmaskBitMatrix(bits, dimension);
    for (int i = 0; i < dimension; i++) {
      for (int j = 0; j < dimension; j++) {
        //TODO: check why the coordinates are swapped
        CPPUNIT_ASSERT_EQUAL(
          //"(" + i + ',' + j + ')',
          condition.isMasked(i, j),
          bits.get(j, i));
      }
    }
  }
};

}
}

#endif // __DATA_MASK_TEST_H__
