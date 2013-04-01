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
  const int bits = BitArray::bitsPerWord + 1;
  BitArray array(bits);
  for(int i = 0; i < bits; i++) {
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
  const int bits = BitArray::bitsPerWord;
  BitArray array(bits);
  for(int i = 0; i < bits; i++) {
    array.set(i);
  }
  array.clear();
  for(int i = 0; i < bits; i++) {
    CPPUNIT_ASSERT_EQUAL(false, array.get(i));
  }
}

void BitArrayTest::testGetArray() {
  const int bits = BitArray::bitsPerWord;
  BitArray array(2 * bits);
  array.set(0);
  array.set(2 * bits - 1);
  vector<int> words(array.getBitArray());
  CPPUNIT_ASSERT_EQUAL(1, words[0]);
  CPPUNIT_ASSERT_EQUAL((1 << (bits - 1)), words[1]);
}

void BitArrayTest::testIsRange() {
  const int bits = BitArray::bitsPerWord;
  int bits2 = 2 * bits;
  BitArray array(bits2);
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(0, bits2, false));
  CPPUNIT_ASSERT_EQUAL(false, array.isRange(0, bits2, true));
  array.set(bits);
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(bits, bits + 1, true));
  array.set(bits - 1);
  CPPUNIT_ASSERT_EQUAL(true, array.isRange(bits - 1, bits + 1, true));
  array.set(bits + 2);
  CPPUNIT_ASSERT_EQUAL(false, array.isRange(bits - 1, bits + 3, true));
  for(int i = 0; i < bits - 1; i++) {
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
  for(int i = 0; i < test.getSize(); ++i) {
    if(random() & 0x1) {
      test.set(i);
      reference.set(i);
    }
  }
}

void BitArrayTest::testReverseHalves() {
  // one word test, split in half
  {
    const int bits = BitArray::bitsPerWord;
    BitArray test(bits);

    test.clear();
    for(int i = 0; i < bits / 2; ++i) {
      test.set(i);
    }

    test.reverse();
    for(int i = 0; i < bits / 2; ++i) {
      CPPUNIT_ASSERT_EQUAL(test.get(i), !test.get(bits - 1 - i));
    }
  }

  // two word test
  {
    const int bits2 = BitArray::bitsPerWord * 2;
    BitArray test2(bits2);

    test2.clear();
    for(int i = 0; i < bits2 / 2; ++i) {
      test2.set(i);
    }

    test2.reverse();
    for(int i = 0; i < bits2 / 2; ++i) {
      CPPUNIT_ASSERT_EQUAL(test2.get(i), !test2.get(bits2 - 1 - i));
    }
  }
}

void BitArrayTest::testReverseEven() {
  const int bits = BitArray::bitsPerWord * 8;
  BitArray test(bits);
  BitArray reference(bits);

  test.clear();
  reference.clear();

  fillRandom(test, reference);

  test.reverse();
  for(int i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
  }
}

void BitArrayTest::testReverseOdd() {
  const int bits = BitArray::bitsPerWord * 6 + 11;
  BitArray test(bits);
  BitArray reference(bits);

  test.clear();
  reference.clear();

  fillRandom(test, reference);

  test.reverse();
  for(int i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
  }
}

void BitArrayTest::testReverseSweep() {
  int bits;
  const int bitsHigh = BitArray::bitsPerWord * 10;

  for(bits = 1; bits < bitsHigh; ++bits) {
    BitArray test(bits);
    BitArray reference(bits);

    test.clear();
    reference.clear();

    fillRandom(test, reference);

    test.reverse();
    for(int i = 0; i < bits; ++i) {
      CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
    }
  }
}

void BitArrayTest::testReverseReverse() {
  const int bits = BitArray::bitsPerWord * 4 + 17;
  BitArray test(bits);
  BitArray reference(bits);

  test.clear();
  reference.clear();

  fillRandom(test, reference);

  // flip it once and test
  test.reverse();
  for(int i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(bits - 1 - i));
  }
  // flip it back and test
  test.reverse();
  for(int i = 0; i < bits; ++i) {
    CPPUNIT_ASSERT_EQUAL(test.get(i), reference.get(i));
  }
}
}

