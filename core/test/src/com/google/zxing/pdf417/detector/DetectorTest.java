package com.google.zxing.pdf417.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;

import org.junit.Assert;
import org.junit.Test;

public class DetectorTest {

  private final int[] bitSetIndex = { 1, 2, 3, 5 };

  private final int[] bitMatrixPoints = { 1, 2, 2, 0, 3, 1 };

  @Test
  public void testMirror() {
    testMirror(7);
    testMirror(8);
  }

  public void testMirror(int size) {
    BitArray result = new BitArray(size);
    DetectorNew.mirror(getInput(size), result);
    Assert.assertArrayEquals(getExpected(size).getBitArray(), result.getBitArray());
  }

  @Test
  public void testRotate180() throws NotFoundException {
    testRotate180(7, 4);
    testRotate180(7, 5);
    testRotate180(8, 4);
    testRotate180(8, 5);
  }

  public void testRotate180(int width, int height) throws NotFoundException {
    BitMatrix input = getInput(width, height);
    DetectorNew.rotate180(input);
    BitMatrix expected = getExpected(width, height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        Assert.assertEquals("(" + x + "," + y + ")", expected.get(x, y), input.get(x, y));
      }
    }
  }

  private BitMatrix getExpected(int width, int height) {
    BitMatrix result = new BitMatrix(width, height);
    for (int i = 0; i < bitMatrixPoints.length; i += 2) {
      result.set(width - 1 - bitMatrixPoints[i], height - 1 - bitMatrixPoints[i + 1]);
    }
    return result;
  }

  private BitMatrix getInput(int width, int height) {
    BitMatrix result = new BitMatrix(width, height);
    for (int i = 0; i < bitMatrixPoints.length; i += 2) {
      result.set(bitMatrixPoints[i], bitMatrixPoints[i + 1]);
    }
    return result;
  }

  private BitArray getExpected(final int size) {
    BitArray expected = new BitArray(size);
    for (int index : bitSetIndex) {
      expected.set(size - index -1);
    }
    return expected;
  }

  private BitArray getInput(final int size) {
    BitArray input = new BitArray(size);
    for (int index : bitSetIndex) {
      input.set(index);
    }
    return input;
  }
}
