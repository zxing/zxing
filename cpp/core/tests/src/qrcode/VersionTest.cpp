/*
 *  VersionTest.cpp
 *  zxing
 *
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

#include "VersionTest.h"
#include <zxing/ReaderException.h>
#include <zxing/qrcode/Version.h>

namespace zxing {
namespace qrcode {

static void assertNotNull(ECBlocks *blocks) {
  CPPUNIT_ASSERT_EQUAL(false, blocks == 0);
}

static void checkVersion(Version *version, int number, int dimension) {
  CPPUNIT_ASSERT_EQUAL(false, version == 0);
  CPPUNIT_ASSERT_EQUAL(number, version->getVersionNumber());
  if (number > 1) {
    CPPUNIT_ASSERT_EQUAL(true,
                         version->getAlignmentPatternCenters().size() > 0);
  }
  CPPUNIT_ASSERT_EQUAL(dimension, version->getDimensionForVersion());
  assertNotNull(&version->getECBlocksForLevel(ErrorCorrectionLevel::H));
  assertNotNull(&version->getECBlocksForLevel(ErrorCorrectionLevel::L));
  assertNotNull(&version->getECBlocksForLevel(ErrorCorrectionLevel::M));
  assertNotNull(&version->getECBlocksForLevel(ErrorCorrectionLevel::Q));
  CPPUNIT_ASSERT_EQUAL(false, version->buildFunctionPattern() == 0);
}


CPPUNIT_TEST_SUITE_REGISTRATION(VersionTest);

void VersionTest::testVersionForNumber() {
  for (int i = 1; i <= (int)Version::VERSIONS.size(); i++) {
    Version *v = Version::VERSIONS[i-1];
    CPPUNIT_ASSERT_EQUAL((int)i, v->getVersionNumber());
  }

  try {
    Version::getVersionForNumber(0);
    CPPUNIT_FAIL("Should have thrown an exception");
  } catch (zxing::ReaderException const& re) {
    // good
  }
  for (int i = 1; i <= 40; i++) {
    checkVersion(Version::getVersionForNumber(i), i, 4*i + 17);
  }
}

void VersionTest::testGetProvisionalVersionForDimension() {
  for (int i = 1; i <= 40; i++) {
    int dimension = 4 * i + 17;
    Version *v = Version::getProvisionalVersionForDimension(dimension);
    int vi = v->getVersionNumber();
    CPPUNIT_ASSERT_EQUAL(i, vi);
  }
}

void VersionTest::testDecodeVersionInformation() {
  // Spot check
  CPPUNIT_ASSERT_EQUAL(7, Version::decodeVersionInformation(0x07C94)->getVersionNumber());
  CPPUNIT_ASSERT_EQUAL(12, Version::decodeVersionInformation(0x0C762)->getVersionNumber());
  CPPUNIT_ASSERT_EQUAL(17, Version::decodeVersionInformation(0x1145D)->getVersionNumber());
  CPPUNIT_ASSERT_EQUAL(22, Version::decodeVersionInformation(0x168C9)->getVersionNumber());
  CPPUNIT_ASSERT_EQUAL(27, Version::decodeVersionInformation(0x1B08E)->getVersionNumber());
  CPPUNIT_ASSERT_EQUAL(32, Version::decodeVersionInformation(0x209D5)->getVersionNumber());
}

}
}

