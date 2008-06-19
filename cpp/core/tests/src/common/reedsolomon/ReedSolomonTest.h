#ifndef __REED_SOLOMON_TEST_H__
#define __REED_SOLOMON_TEST_H__

/*
 *  ReedSolomonTest.h
 *  zxing
 *
 *  Created by Christian Brunschen on 06/05/2008.
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

#include <vector>
#include <memory>
#include <cppunit/TestFixture.h>
#include <cppunit/extensions/HelperMacros.h>
#include "../../../../src/common/Counted.h"
#include "../../../../src/common/Array.h"

using namespace std;
using namespace common;

namespace reedsolomon {
  class ReedSolomonDecoder;
  
  class ReedSolomonTest : public CppUnit::TestFixture {
    CPPUNIT_TEST_SUITE (ReedSolomonTest);
    CPPUNIT_TEST (testNoError);
    CPPUNIT_TEST (testOneError);
    CPPUNIT_TEST (testMaxErrors);
    CPPUNIT_TEST (testTooManyErrors);
    CPPUNIT_TEST_SUITE_END ();
    
  public:
    void setUp();
    void tearDown();
    
  protected:
    void testNoError();
    void testOneError();
    void testMaxErrors();
    void testTooManyErrors();
    
  private:
    ArrayRef<int> qrCodeTest_;
    ArrayRef<int> qrCodeTestWithEc_;
    int qrCodeCorrectable_;
    ReedSolomonDecoder *qrRSDecoder_;
    void checkQRRSDecode(ArrayRef<int> &received);
    static void corrupt(ArrayRef<int> &received, int howMany);
  };
}

#endif // __REED_SOLOMON_TEST_H__
