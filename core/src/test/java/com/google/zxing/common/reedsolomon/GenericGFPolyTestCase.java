/*
 * Copyright 2018 ZXing authors
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

package com.google.zxing.common.reedsolomon;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link GenericGFPoly}.
 */
public final class GenericGFPolyTestCase extends Assert {

  private static final GenericGF FIELD = GenericGF.QR_CODE_FIELD_256;

  @Test
  public void testPolynomialString() {
    assertEquals("0", FIELD.getZero().toString());
    assertEquals("-1", FIELD.buildMonomial(0, -1).toString());
    GenericGFPoly p = new GenericGFPoly(FIELD, new int[] {3, 0, -2, 1, 1});
    assertEquals("a^25x^4 - ax^2 + x + 1", p.toString());
    p = new GenericGFPoly(FIELD, new int[] {3});
    assertEquals("a^25", p.toString());
  }

  @Test
  public void testZero() {
    assertEquals(FIELD.getZero(),FIELD.buildMonomial(1, 0));
    assertEquals(FIELD.getZero(), FIELD.buildMonomial(1, 2).multiply(0));
  }

  @Test
  public void testEvaluate() {
    assertEquals(3, FIELD.buildMonomial(0, 3).evaluateAt(0));
  }

}
