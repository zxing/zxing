/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.pdf417.detector;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

import org.junit.Assert;
import org.junit.Test;

public final class DetectorTest extends Assert {

  private static final int[] BIT_SET_INDEX = { 1, 2, 3, 5 };
  private static final int[] BIT_MATRIX_POINTS = { 1, 2, 2, 0, 3, 1 };

  @Test
  public void testMirror() {
    testMirror(7);
    testMirror(8);
  }

  private static void testMirror(int size) {
    BitArray result = new BitArray(size);
    Detector.mirror(getInput(size), result);
    assertEquals(getExpected(size).toString(), result.toString());
  }

  @Test
  public void testRotate180() {
    testRotate180(7, 4);
    testRotate180(7, 5);
    testRotate180(8, 4);
    testRotate180(8, 5);
  }

  private static void testRotate180(int width, int height) {
    BitMatrix input = getInput(width, height);
    Detector.rotate180(input);
    BitMatrix expected = getExpected(width, height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        assertEquals("(" + x + ',' + y + ')', expected.get(x, y), input.get(x, y));
      }
    }
  }

  private static BitMatrix getExpected(int width, int height) {
    BitMatrix result = new BitMatrix(width, height);
    for (int i = 0; i < BIT_MATRIX_POINTS.length; i += 2) {
      result.set(width - 1 - BIT_MATRIX_POINTS[i], height - 1 - BIT_MATRIX_POINTS[i + 1]);
    }
    return result;
  }

  private static BitMatrix getInput(int width, int height) {
    BitMatrix result = new BitMatrix(width, height);
    for (int i = 0; i < BIT_MATRIX_POINTS.length; i += 2) {
      result.set(BIT_MATRIX_POINTS[i], BIT_MATRIX_POINTS[i + 1]);
    }
    return result;
  }

  private static BitArray getExpected(int size) {
    BitArray expected = new BitArray(size);
    for (int index : BIT_SET_INDEX) {
      expected.set(size - 1 - index);
    }
    return expected;
  }

  private static BitArray getInput(int size) {
    BitArray input = new BitArray(size);
    for (int index : BIT_SET_INDEX) {
      input.set(index);
    }
    return input;
  }
}
