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

package com.google.zxing.aztec.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.aztec.detector.Detector.Point;
import com.google.zxing.aztec.encoder.AztecCode;
import com.google.zxing.aztec.encoder.Encoder;
import com.google.zxing.common.BitMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tests for the Detector
 *
 * @author Frank Yellin
 */
public final class DetectorTest extends Assert {

  private static final Charset LATIN_1 = Charset.forName("ISO-8859-1");

  @Test
  public void testErrorInParameterLocatorZeroZero() throws Exception {
    // Layers=1, CodeWords=1.  So the parameter info and its Reed-Solomon info
    // will be completely zero!
    testErrorInParameterLocator("X");
  }

  @Test
  public void testErrorInParameterLocatorCompact() throws Exception {
    testErrorInParameterLocator("This is an example Aztec symbol for Wikipedia.");
  }

  @Test
  public void testErrorInParameterLocatorNotCompact() throws Exception {
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYabcdefghijklmnopqrstuvwxyz";
    testErrorInParameterLocator(alphabet + alphabet + alphabet);
  }

  // Test that we can tolerate errors in the parameter locator bits
  private static void testErrorInParameterLocator(String data) throws Exception {
    AztecCode aztec = Encoder.encode(data.getBytes(LATIN_1), 25);
    int layers = aztec.getLayers();
    boolean compact = aztec.isCompact();
    List<Point> orientationPoints = getOrientationPoints(aztec);
    Random random = new Random(aztec.getMatrix().hashCode());   // random, but repeatable
    for (BitMatrix matrix : getRotations(aztec.getMatrix())) {
      // Each time through this loop, we reshuffle the corners, to get a different set of errors
      Collections.shuffle(orientationPoints, random);
      for (int errors = 1; errors <= 3; errors++) {
        // Add another error to one of the parameter locator bits
        matrix.flip(orientationPoints.get(errors).getX(), orientationPoints.get(errors).getY());
        try {
          // The detector can't yet deal with bitmaps in which each square is only 1x1 pixel.
          // We zoom it larger.
          AztecDetectorResult r = new Detector(makeLarger(matrix, 3)).detect();
          if (errors < 3) {
            assertNotNull(r);
            assertEquals(r.getNbLayers(), layers);
            assertEquals(r.isCompact(), compact);
          } else {
            fail("Should not succeed with more than two errors");
          }
        } catch (NotFoundException e) {
          assertEquals("Should only fail with three errors", 3, errors);
        }
      }
    }
  }

  // Zooms a bit matrix so that each bit is factor x factor
  private static BitMatrix makeLarger(BitMatrix input, int factor) {
    int width = input.getWidth();
    BitMatrix output = new BitMatrix(width * factor);
    for (int inputY = 0; inputY < width; inputY++) {
      for (int inputX = 0; inputX < width; inputX++) {
        if (input.get(inputX, inputY)) {
          output.setRegion(inputX * factor, inputY * factor, factor, factor);
        }
      }
    }
    return output;
  }

  // Returns a list of the four rotations of the BitMatrix.  The identity rotation is
  // explicitly a copy, so that it can be modified without affecting the original matrix.
  private static List<BitMatrix> getRotations(BitMatrix input) {
    int width = input.getWidth();
    BitMatrix matrix0 = new BitMatrix(width);
    BitMatrix matrix90 = new BitMatrix(width);
    BitMatrix matrix180 = new BitMatrix(width);
    BitMatrix matrix270 = new BitMatrix(width);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < width; y++) {
        if (input.get(x, y)) {
          matrix0.set(x, y);
          matrix90.set(y, width - x - 1);
          matrix180.set(width - x - 1, width - y - 1);
          matrix270.set(width - y - 1, x);
        }
      }
    }
    return Arrays.asList(matrix0, matrix90, matrix180, matrix270);
  }

  private static List<Point> getOrientationPoints(AztecCode code) {
    int center = code.getMatrix().getWidth() / 2;
    int offset = code.isCompact() ? 5 : 7;
    List<Point> result = new ArrayList<Point>();
    for (int xSign = -1; xSign <= 1; xSign += 2) {
      for (int ySign = -1; ySign <= 1; ySign += 2) {
        result.add(new Point(center + xSign * offset, center + ySign * offset));
        result.add(new Point(center + xSign * (offset - 1), center + ySign * offset));
        result.add(new Point(center + xSign * offset, center + ySign * (offset - 1)));
      }
    }
    return result;
  }

}
