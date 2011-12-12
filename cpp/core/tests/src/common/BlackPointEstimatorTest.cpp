/*
 *  BlackPointEstimatorTest.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 12/05/2008.
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

#include "BlackPointEstimatorTest.h"
#include <zxing/common/IllegalArgumentException.h>
#include <vector>

namespace zxing {
using namespace std;
CPPUNIT_TEST_SUITE_REGISTRATION(BlackPointEstimatorTest);

void BlackPointEstimatorTest::testBasic() {
  int histogramRaw[] = { 0, 0, 11, 43, 37, 18, 3, 1, 0, 0, 13, 36, 24, 0, 11, 2 };
  vector<int> histogram(histogramRaw, histogramRaw+16);
  size_t point = GlobalHistogramBinarizer::estimate(histogram);
  CPPUNIT_ASSERT_EQUAL((size_t)64, point);
}

void BlackPointEstimatorTest::testTooLittleRange() {
  try {
    int histogramRaw[] = { 0, 0, 0, 0, 0, 0, 1, 43, 48, 18, 3, 1, 0, 0, 0, 0 };
    vector<int> histogram(histogramRaw, histogramRaw+16);
    GlobalHistogramBinarizer::estimate(histogram);
    CPPUNIT_FAIL("Should have thrown an exception");

  } catch (IllegalArgumentException ie) {
    // good
  }
}
}
