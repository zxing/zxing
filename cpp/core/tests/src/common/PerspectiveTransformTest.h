#ifndef __PERSPECTIVE_TRANSFORM_TEST_H__
#define __PERSPECTIVE_TRANSFORM_TEST_H__

/*
 *  PerspectiveTransformTest.h
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
#include <zxing/common/PerspectiveTransform.h>

namespace zxing {
class PerspectiveTransformTest : public CPPUNIT_NS::TestFixture {
  CPPUNIT_TEST_SUITE(PerspectiveTransformTest);
  CPPUNIT_TEST(testSquareToQuadrilateral);
  CPPUNIT_TEST(testQuadrilateralToQuadrilateral);
  CPPUNIT_TEST_SUITE_END();

public:

protected:
  void testSquareToQuadrilateral();
  void testQuadrilateralToQuadrilateral();

private:
  static void assertPointEquals(float expectedX, float expectedY,
                                float sourceX, float sourceY,
                                Ref<PerspectiveTransform> pt);
};
}

#endif // __PERSPECTIVE_TRANSFORM_TEST_H__
