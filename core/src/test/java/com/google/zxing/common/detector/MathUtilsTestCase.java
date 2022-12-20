/*
 * Copyright 2014 ZXing authors
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

package com.google.zxing.common.detector;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link MathUtils}.
 */
public final class MathUtilsTestCase extends Assert {

  private static final float EPSILON = 1.0E-8f;

  @Test
  public void testRound() {
    assertEquals(-1, MathUtils.round(-1.0f));
    assertEquals(0, MathUtils.round(0.0f));
    assertEquals(1, MathUtils.round(1.0f));

    assertEquals(2, MathUtils.round(1.9f));
    assertEquals(2, MathUtils.round(2.1f));

    assertEquals(3, MathUtils.round(2.5f));

    assertEquals(-2, MathUtils.round(-1.9f));
    assertEquals(-2, MathUtils.round(-2.1f));

    assertEquals(-3, MathUtils.round(-2.5f)); // This differs from Math.round()

    assertEquals(Integer.MAX_VALUE, MathUtils.round(Integer.MAX_VALUE));
    assertEquals(Integer.MIN_VALUE, MathUtils.round(Integer.MIN_VALUE));

    assertEquals(Integer.MAX_VALUE, MathUtils.round(Float.POSITIVE_INFINITY));
    assertEquals(Integer.MIN_VALUE, MathUtils.round(Float.NEGATIVE_INFINITY));

    assertEquals(0, MathUtils.round(Float.NaN));
  }

  @Test
  public void testDistance() {
    assertEquals((float) Math.sqrt(8.0), MathUtils.distance(1.0f, 2.0f, 3.0f, 4.0f), EPSILON);
    assertEquals(0.0f, MathUtils.distance(1.0f, 2.0f, 1.0f, 2.0f), EPSILON);

    assertEquals((float) Math.sqrt(8.0), MathUtils.distance(1, 2, 3, 4), EPSILON);
    assertEquals(0.0f, MathUtils.distance(1, 2, 1, 2), EPSILON);
  }

  @Test
  public void testSum() {
    assertEquals(0, MathUtils.sum(new int[] {}));
    assertEquals(1, MathUtils.sum(new int[] {1}));
    assertEquals(4, MathUtils.sum(new int[] {1, 3}));
    assertEquals(0, MathUtils.sum(new int[] {-1, 1}));
  }

}
