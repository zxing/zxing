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

package com.google.zxing.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sean Owen
 */
public final class PerspectiveTransformTestCase extends Assert {

  private static final float EPSILON = 0.0001f;

  @Test
  public void testSquareToQuadrilateral() {
    PerspectiveTransform pt = PerspectiveTransform.squareToQuadrilateral(
        2.0f, 3.0f, 10.0f, 4.0f, 16.0f, 15.0f, 4.0f, 9.0f);
    assertPointEquals(2.0f, 3.0f, 0.0f, 0.0f, pt);
    assertPointEquals(10.0f, 4.0f, 1.0f, 0.0f, pt);
    assertPointEquals(4.0f, 9.0f, 0.0f, 1.0f, pt);
    assertPointEquals(16.0f, 15.0f, 1.0f, 1.0f, pt);
    assertPointEquals(6.535211f, 6.8873234f, 0.5f, 0.5f, pt);
    assertPointEquals(48.0f, 42.42857f, 1.5f, 1.5f, pt);
  }

  @Test
  public void testQuadrilateralToQuadrilateral() {
    PerspectiveTransform pt = PerspectiveTransform.quadrilateralToQuadrilateral(
        2.0f, 3.0f, 10.0f, 4.0f, 16.0f, 15.0f, 4.0f, 9.0f,
        103.0f, 110.0f, 300.0f, 120.0f, 290.0f, 270.0f, 150.0f, 280.0f);
    assertPointEquals(103.0f, 110.0f, 2.0f, 3.0f, pt);
    assertPointEquals(300.0f, 120.0f, 10.0f, 4.0f, pt);
    assertPointEquals(290.0f, 270.0f, 16.0f, 15.0f, pt);
    assertPointEquals(150.0f, 280.0f, 4.0f, 9.0f, pt);
    assertPointEquals(7.1516876f, -64.60185f, 0.5f, 0.5f, pt);
    assertPointEquals(328.09116f, 334.16385f, 50.0f, 50.0f, pt);
  }

  private static void assertPointEquals(float expectedX,
                                        float expectedY,
                                        float sourceX,
                                        float sourceY,
                                        PerspectiveTransform pt) {
    float[] points = {sourceX, sourceY};
    pt.transformPoints(points);
    assertEquals(expectedX, points[0], EPSILON);
    assertEquals(expectedY, points[1], EPSILON);
  }

}