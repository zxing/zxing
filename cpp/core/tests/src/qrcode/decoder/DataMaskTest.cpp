// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
/*
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

#include "DataMaskTest.h"

namespace zxing {
namespace qrcode {

CPPUNIT_TEST_SUITE_REGISTRATION(DataMaskTest);

class Mask0Condition : public MaskCondition {
public:
  Mask0Condition() { }
  bool isMasked(int i, int j) {
    return (i + j) % 2 == 0;
  }
};

class Mask1Condition : public MaskCondition {
public:
  Mask1Condition() { }
  bool isMasked(int i, int) {
    return i % 2 == 0;
  }
};

class Mask2Condition : public MaskCondition {
public:
  Mask2Condition() { }
  bool isMasked(int, int j) {
    return j % 3 == 0;
  }
};

class Mask3Condition : public MaskCondition {
public:
  Mask3Condition() { }
  bool isMasked(int i, int j) {
    return (i + j) % 3 == 0;
  }
};

class Mask4Condition : public MaskCondition {
public:
  Mask4Condition() { }
  bool isMasked(int i, int j) {
    return (i / 2 + j / 3) % 2 == 0;
  }
};

class Mask5Condition : public MaskCondition {
public:
  Mask5Condition() { }
  bool isMasked(int i, int j) {
    return (i * j) % 2 + (i * j) % 3 == 0;
  }
};

class Mask6Condition : public MaskCondition {
public:
  Mask6Condition() { }
  bool isMasked(int i, int j) {
    return ((i * j) % 2 + (i * j) % 3) % 2 == 0;
  }
};

class Mask7Condition : public MaskCondition {
public:
  Mask7Condition() { }
  bool isMasked(int i, int j) {
    return ((i + j) % 2 + (i * j) % 3) % 2 == 0;
  }
};


void DataMaskTest::testMask0() {
  Mask0Condition condition;
  testMaskAcrossDimensions(0, condition);
}

void DataMaskTest::testMask1() {
  Mask1Condition condition;
  testMaskAcrossDimensions(1, condition);
}

void DataMaskTest::testMask2() {
  Mask2Condition condition;
  testMaskAcrossDimensions(2, condition);
}

void DataMaskTest::testMask3() {
  Mask3Condition condition;
  testMaskAcrossDimensions(3, condition);
}

void DataMaskTest::testMask4() {
  Mask4Condition condition;
  testMaskAcrossDimensions(4, condition);
}

void DataMaskTest::testMask5() {
  Mask5Condition condition;
  testMaskAcrossDimensions(5, condition);
}

void DataMaskTest::testMask6() {
  Mask6Condition condition;
  testMaskAcrossDimensions(6, condition);
}

void DataMaskTest::testMask7() {
  Mask7Condition condition;
  testMaskAcrossDimensions(7, condition);
}

}
}
