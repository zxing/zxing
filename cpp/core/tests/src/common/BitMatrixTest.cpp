/*
 *  BitMatrixTest.cpp
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

#include "BitMatrixTest.h"
#include <limits>
#include <stdlib.h>

namespace zxing {
using namespace std;

CPPUNIT_TEST_SUITE_REGISTRATION(BitMatrixTest);

BitMatrixTest::BitMatrixTest() {
  srand(getpid());
}

void BitMatrixTest::testGetSet() {
  const int bits = BitMatrix::bitsPerWord;
  BitMatrix matrix(bits + 1);
  CPPUNIT_ASSERT_EQUAL(bits + 1, matrix.getHeight());
  for (int i = 0; i < bits + 1; i++) {
    for (int j = 0; j < bits + 1; j++) {
      if (i * j % 3 == 0) {
        matrix.set(i, j);
      }
    }
  }
  for (int i = 0; i < bits + 1; i++) {
    for (int j = 0; j < bits + 1; j++) {
      CPPUNIT_ASSERT_EQUAL(i * j % 3 == 0, matrix.get(i, j));
    }
  }
}

void BitMatrixTest::testSetRegion() {
  BitMatrix matrix(5);
  matrix.setRegion(1, 1, 3, 3);
  for (int i = 0; i < 5; i++) {
    for (int j = 0; j < 5; j++) {
      CPPUNIT_ASSERT_EQUAL(i >= 1 && i <= 3 && j >= 1 && j <= 3,
                           matrix.get(i, j));
    }
  }
}

void BitMatrixTest::testGetRow0() {
  const int width = 2;
  const int height = 2;
  runBitMatrixGetRowTest(width, height);
}

void BitMatrixTest::testGetRow1() {
  const int width = 98;
  const int height = 76;
  runBitMatrixGetRowTest(width, height);
}

void BitMatrixTest::testGetRow2() {
  const int width = 320;
  const int height = 320;
  runBitMatrixGetRowTest(width, height);
}

void BitMatrixTest::testGetRow3() {
  const int width = 17;
  const int height = 23;
  runBitMatrixGetRowTest(width, height);
}

void BitMatrixTest::runBitMatrixGetRowTest(int width, int height) {
  BitMatrix mat(width, height);
  for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
      bool v = ((rand() & 0x01) != 0);
      if (v) {
        mat.set(x, y);
      }
    }
  }
  Ref<BitArray> row(new BitArray(width));
  for (int y = 0; y < height; y++) {
    row = mat.getRow(y, row);
    for (int x = 0; x < width; x++) {
      CPPUNIT_ASSERT_EQUAL(row->get(x), mat.get(x,y));
    }
  }
}
}
