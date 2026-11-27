/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.pdf417.decoder.ec;

import com.google.zxing.ChecksumException;

import org.junit.Test;

import java.util.Random;

/**
 * @author Sean Owen
 */
public final class ErrorCorrectionTestCase extends AbstractErrorCorrectionTestCase {

  private static final int[] PDF417_TEST = {
      48, 901, 56, 141, 627, 856, 330, 69, 244, 900, 852, 169, 843, 895, 852, 895, 913, 154, 845, 778, 387, 89, 869,
      901, 219, 474, 543, 650, 169, 201, 9, 160, 35, 70, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900,
      900, 900};
  private static final int[] PDF417_TEST_WITH_EC = {
      48, 901, 56, 141, 627, 856, 330, 69, 244, 900, 852, 169, 843, 895, 852, 895, 913, 154, 845, 778, 387, 89, 869,
      901, 219, 474, 543, 650, 169, 201, 9, 160, 35, 70, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900, 900,
      900, 900, 769, 843, 591, 910, 605, 206, 706, 917, 371, 469, 79, 718, 47, 777, 249, 262, 193, 620, 597, 477, 450,
      806, 908, 309, 153, 871, 686, 838, 185, 674, 68, 679, 691, 794, 497, 479, 234, 250, 496, 43, 347, 582, 882, 536,
      322, 317, 273, 194, 917, 237, 420, 859, 340, 115, 222, 808, 866, 836, 417, 121, 833, 459, 64, 159};
  private static final int ECC_BYTES = PDF417_TEST_WITH_EC.length - PDF417_TEST.length;
  private static final int ERROR_LIMIT = ECC_BYTES;
  private static final int MAX_ERRORS = ERROR_LIMIT / 2;
  private static final int MAX_ERASURES = ERROR_LIMIT;

  private final ErrorCorrection ec = new ErrorCorrection();

  @Test
  public void testNoError() throws ChecksumException {
    int[] received = PDF417_TEST_WITH_EC.clone();
    // no errors
    checkDecode(received);
  }

  @Test
  public void testOneError() throws ChecksumException {
    Random random = getRandom();
    for (int i = 0; i < PDF417_TEST_WITH_EC.length; i++) {
      int[] received = PDF417_TEST_WITH_EC.clone();
      received[i] = random.nextInt(256);
      checkDecode(received);
    }
  }

  @Test
  public void testMaxErrors() throws ChecksumException {
    Random random = getRandom();
    for (int testIterations = 0; testIterations < 100; testIterations++) { // # iterations is kind of arbitrary
      int[] received = PDF417_TEST_WITH_EC.clone();
      corrupt(received, MAX_ERRORS, random);
      checkDecode(received);
    }
  }

  @Test
  public void testTooManyErrors() {
    int[] received = PDF417_TEST_WITH_EC.clone();
    Random random = getRandom();
    corrupt(received, MAX_ERRORS + 1, random);
    try {
      checkDecode(received);
      fail("Should not have decoded");
    } catch (ChecksumException ce) {
      // good
    }
  }

  @Test
  public void testOversizedReceived() {
    // A received array of length >= field.getSize() would alias modulo (field.getSize() - 1)
    // or index past the exponent table; it must be rejected instead of throwing AIOOBE.
    for (int length : new int[] { ModulusGF.PDF417_GF.getSize(), ModulusGF.PDF417_GF.getSize() + 1 }) {
      int[] received = new int[length];
      System.arraycopy(PDF417_TEST_WITH_EC, 0, received, 0, PDF417_TEST_WITH_EC.length);
      received[10] = (received[10] + 1) % ModulusGF.PDF417_GF.getSize(); // force a non-zero syndrome
      try {
        ec.decode(received, ECC_BYTES, new int[] {0});
        fail("Should not have decoded for length " + length);
      } catch (ChecksumException ce) {
        // good
      }
    }
  }

  @Test
  public void testInvalidCodewordAndErasureValues() {
    int[] negative = PDF417_TEST_WITH_EC.clone();
    negative[0] = -1;
    try {
      ec.decode(negative, ECC_BYTES, null);
      fail("Should not have decoded negative codeword");
    } catch (ChecksumException expected) {
      // good
    }

    int[] outOfRange = PDF417_TEST_WITH_EC.clone();
    outOfRange[0] = ModulusGF.PDF417_GF.getSize();
    try {
      ec.decode(outOfRange, ECC_BYTES, null);
      fail("Should not have decoded out-of-range codeword");
    } catch (ChecksumException expected) {
      // good
    }

    int[] corrupted = PDF417_TEST_WITH_EC.clone();
    corrupted[0] = (corrupted[0] + 1) % ModulusGF.PDF417_GF.getSize();
    try {
      ec.decode(corrupted, ECC_BYTES, new int[] { corrupted.length });
      fail("Should not have decoded with out-of-bounds erasure");
    } catch (ChecksumException expected) {
      // good
    }
  }

  @Test
  public void testRejectZeroIterationNonCodeword() {
    // S(x) = 1 with R = 8 has deg(S) = 0 < R/2; must be rejected rather than returning 0.
    int[] syndromes = { 1, 0, 0, 0, 0, 0, 0, 0 };
    int[] received = buildVectorWithSyndromes(20, syndromes);
    int[] original = received.clone();
    try {
      ec.decode(received, syndromes.length, null);
      fail("Should not have accepted non-codeword");
    } catch (ChecksumException expected) {
      assertArrayEquals(original, received);
    }
  }

  @Test
  public void testAtomicFailureOnBadErrorLocation() {
    // 2 errors: one in-bounds (power 2 -> index 7) and one out-of-bounds (power 15 -> index -6).
    ModulusGF field = ModulusGF.PDF417_GF;
    int e1 = 5;
    int e2 = 7;
    int[] syndromes = new int[6];
    for (int k = 0; k < syndromes.length; k++) {
      int term1 = field.multiply(e1, field.exp((2 * (k + 1)) % (field.getSize() - 1)));
      int term2 = field.multiply(e2, field.exp((15 * (k + 1)) % (field.getSize() - 1)));
      syndromes[k] = field.add(term1, term2);
    }
    int[] received = buildVectorWithSyndromes(10, syndromes);
    int[] original = received.clone();
    try {
      ec.decode(received, syndromes.length, null);
      fail("Should not have decoded with out-of-bounds error location");
    } catch (ChecksumException expected) {
      assertArrayEquals("received array must not be mutated on failure", original, received);
    }
  }

  private static int[] buildVectorWithSyndromes(int length, int[] syndromes) {
    ModulusGF field = ModulusGF.PDF417_GF;
    int r = syndromes.length;
    int[] points = new int[r];
    for (int k = 0; k < r; k++) {
      points[k] = field.exp(k + 1);
    }
    ModulusPoly result = field.getZero();
    for (int j = 0; j < r; j++) {
      if (syndromes[j] == 0) {
        continue;
      }
      ModulusPoly basis = field.getOne();
      int denom = 1;
      for (int k = 0; k < r; k++) {
        if (k != j) {
          basis = basis.multiply(new ModulusPoly(field, new int[] { 1, field.subtract(0, points[k]) }));
          denom = field.multiply(denom, field.subtract(points[j], points[k]));
        }
      }
      int scale = field.multiply(syndromes[j], field.inverse(denom));
      result = result.add(basis.multiply(scale));
    }
    int[] vector = new int[length];
    int[] coeffs = result.getCoefficients();
    System.arraycopy(coeffs, 0, vector, length - coeffs.length, coeffs.length);
    return vector;
  }

  private void checkDecode(int[] received) throws ChecksumException {
    checkDecode(received, new int[0]);
  }

  private void checkDecode(int[] received, int[] erasures) throws ChecksumException {
    ec.decode(received, ECC_BYTES, erasures);
    for (int i = 0; i < PDF417_TEST.length; i++) {
      assertEquals(received[i], PDF417_TEST[i]);
    }
  }

}
