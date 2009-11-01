/*
 *  CountedTest.cpp
 *  zxing
 *
 *  Created by Christian Brunschen on 08/05/2008.
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

#include "CountedTest.h"
#include <zxing/common/Counted.h>
#include <iostream>

using namespace std;
using namespace CPPUNIT_NS;

namespace zxing {

class Foo : public Counted {
public:
  void foo() {
    cout << "foo!\n";
  }
};

//CPPUNIT_TEST_SUITE_REGISTRATION(CountedTest);

void CountedTest::setUp() {}
void CountedTest::tearDown() {}

void CountedTest::test() {
  Foo foo;
  CPPUNIT_ASSERT_EQUAL(0, foo.count());
  foo.retain();
  CPPUNIT_ASSERT_EQUAL(1, foo.count());
  {
    Ref<Foo> fooRef(foo);
    CPPUNIT_ASSERT_EQUAL(2, foo.count());
  }
  CPPUNIT_ASSERT_EQUAL(1, foo.count());
}

}
