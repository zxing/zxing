/*
 * Copyright 2006 Jeremias Maerki.
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

package com.google.zxing.datamatrix.encoder;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the ECC200 error correction.
 */
public final class ErrorCorrectionTestCase extends Assert {

  @Test
  public void testRS() {
    //Sample from Annexe R in ISO/IEC 16022:2000(E)
    char[] cw = {142, 164, 186};
    SymbolInfo symbolInfo = SymbolInfo.lookup(3);
    CharSequence s = ErrorCorrection.encodeECC200(String.valueOf(cw), symbolInfo);
    assertEquals("142 164 186 114 25 5 88 102", HighLevelEncodeTestCase.visualize(s));

    //"A" encoded (ASCII encoding + 2 padding characters)
    cw = new char[]{66, 129, 70};
    s = ErrorCorrection.encodeECC200(String.valueOf(cw), symbolInfo);
    assertEquals("66 129 70 138 234 82 82 95", HighLevelEncodeTestCase.visualize(s));
  }

}
