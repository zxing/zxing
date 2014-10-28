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

import org.junit.Ignore;
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

  @Ignore("Erasures not implemented yet")
  @Test
  public void testMaxErasures() throws ChecksumException {
    Random random = getRandom();
    for (int test : PDF417_TEST) { // # iterations is kind of arbitrary
      int[] received = PDF417_TEST_WITH_EC.clone();
      int[] erasures = erase(received, MAX_ERASURES, random);
      checkDecode(received, erasures);
    }
  }

  @Ignore("Erasures not implemented yet")
  @Test
  public void testTooManyErasures() {
    Random random = getRandom();
    int[] received = PDF417_TEST_WITH_EC.clone();
    int[] erasures = erase(received, MAX_ERASURES + 1, random);
    try {
      checkDecode(received, erasures);
      fail("Should not have decoded");
    } catch (ChecksumException ce) {
      // good
    }
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
