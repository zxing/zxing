/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing.client.result;

import junit.framework.TestCase;

/**
 * Tests {@link com.google.zxing.client.result.URIParsedResult}.
 *
 * @author Sean Owen
 */
public final class URIParsedResultTestCase extends TestCase {

  public void testIsPossiblyMalicious() {
    doTestIsPossiblyMalicious("http://google.com", false);
    doTestIsPossiblyMalicious("http://google.com@evil.com", true);
    doTestIsPossiblyMalicious("http://google.com:@evil.com", true);
    doTestIsPossiblyMalicious("google.com:@evil.com", true);
    doTestIsPossiblyMalicious("https://google.com:443", false);
    doTestIsPossiblyMalicious("http://google.com/foo@bar", false);    
  }

  private void doTestIsPossiblyMalicious(String uri, boolean expected) {
    URIParsedResult result = new URIParsedResult(uri, null);
    assertEquals(expected, result.isPossiblyMaliciousURI());
  }

}