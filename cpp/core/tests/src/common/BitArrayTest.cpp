/*
 *  BitArrayTest.cpp
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

#include "BitArrayTest.h"
#include <limits>
#include <cstdlib>

using namespace std;

namespace zxing {

CPPUNIT_TEST_SUITE_REGISTRATION(BitArrayTest);

void BitArrayTest::testGetSet() {
  size_t bits = numeric_limits<unsigned int>::digits + 1;
  BitArray array(bits);
  for(size_t i = 0; i < bits; i++) {
    CPPUNIT_ASSERT_EQUAL(false, array.get(i));
    array.set(i);
    CPPUNIT_ASSERT_EQUAL(true, array.get(i));
  }
}

void BitArrayTest::testSetBulk() {
  BitArray array(64);
  array.setBulk(32, 0xFFFF0000);
  for(int i = 0; i < 48; i++) {
    CPPUNIT_ASSERT_EQUAL(false, array.get(i));
  }
  for(int i = 48; i < 64; i++) {
    CPPUNIT_ASSERT_EQUAL(true, array.get(i));
  }
}

void BitArrayTest::testClear() {
  size_t bits = numeric_limits<unsigned int>::digits;
  BitArray array(bits);
  for(size_t i = 0; i < bits; i++) {
    array.set(i);
  }
  array.clear();
  for(size_t i = 0; i < bits; i++) {
    CPPUNIT_ASSERT_EQUAL(false, array.get(i));
  }
}

void BitArrayTest::testGetArray() {
  size_t bits = numeric_limits<unsigned int>::digits;
  BitArray array(2 * bits);
  array.set(0);
  array.set(2 * bits - 1);
  vector<unsigned> words(array.getBitArray());
  CPPUNIT_ASSERT_EQUAL(1u, words[0]);
  CPPUNIT_ASSERT_EQUAL((1u << (bits - 1)), words[1]);
}

void BitArrayTest::testIsRange() {
  size_t bits = numeric_limits<unsigned int>::digits;
  size_t bits2 = 2 * bits;
  BitArray array(bits2);
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(0, bits2, false));
  CPPUNIT_ASSERT_EQUAL(false, array.isRange(0, bits2, true));
  array.set(bits);
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(bits, bits + 1, true));
  array.set(bits - 1);
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(bits - 1, bits + 1, true));
  array.set(bits + 2);
  CPPUNIT_ASSERT_EQUAL(false, array.isRange(bits - 1, bits + 3, true));
  for(size_t i = 0; i < bits - 1; i++) {
    array.set(i);
  }
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(0, bits + 1, true));
  for(int i = 33; i < 64; i++) {
    array.set(i);
  }
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(0, 64, true));
  CPPUNIT_ASSERT_EQUAL(false, array.isRange(0, 64, false));
}

// fills the two arrays with identical random bits
void BitArrayTest::fillRandom(BitArray& test, BitArray& reference) {
  srandom(0xDEADBEEFL + test.getSize());
  for(size_t i = 0; i < test.getSize(); ++i) {
    if(random() & 0x1) {
      test.set(i);
      reference.set(i);
    }
  }
}

void BitArrayTest::testReverseHalves() {
  // one word test, split in half
  {
    size_t bits = numeric_limits<unsigned int>::digits;
    BitArray test(bits);

    test.clear();
    for(size_t i = 0; i < bits / 2; ++i) {
      test.set(i);
    }

    test.reverse();
    for(size_t i = 0; i < bits / 2; ++i) {
      CPPUNIT_ASSERT_EQUAL(test.get(i), !test.get(bits - 1 - i));
    }
  }

  // two word test
  {
    size_t bits2 = numeric_limits<unsigned int>::digits * 2;
    BitArray test2(bits2);

    test2.clear();
    for(size_t i = 0; i < bits2 / 2; ++i) {
      test2.set(i);
    }

    test2.reverse();
    for(size_t i = 0; i < bits2 / 2; ++i) {
      CPPUNIT_ASSERT_EQUAL(test2.get(i), !test2.get(bits2 - 1 - i));
    }
  }
}

void BitArrayTest::testReverseEven() {
  size_t bits = numeric_limits<unsigned int>::digits * 8;
  BitArray test(bits);
  BitArray reference(bits);

  test.clear();
  reference.clear();

  fillRandom(test, reference);

  test.reverse();
  for(size_t i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
  }
}

void BitArrayTest::testReverseOdd() {
  size_t bits = numeric_limits<unsigned int>::digits * 6 + 11;
  BitArray test(bits);
  BitArray reference(bits);

  test.clear();
  reference.clear();

  fillRandom(test, reference);

  test.reverse();
  for(size_t i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
  }
}

void BitArrayTest::testReverseSweep() {
  size_t bits;
  size_t bitsHigh = numeric_limits<unsigned int>::digits * 10;

  for(bits = 1; bits < bitsHigh; ++bits) {
    BitArray test(bits);
    BitArray reference(bits);

    test.clear();
    reference.clear();

    fillRandom(test, reference);

    test.reverse();
    for(size_t i = 0; i < bits; ++i) {
      CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
    }
  }
}

void BitArrayTest::testReverseReverse() {
  size_t bits = numeric_limits<unsigned int>::digits * 4 + 17;
  BitArray test(bits);
  BitArray reference(bits);

  test.clear();
  reference.clear();

  fillRandom(test, reference);

  // flip it once and test
  test.reverse();
  for(size_t i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
  }
  // flip it back and test
  test.reverse();
  for(size_t i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(i));
  }
}
}

