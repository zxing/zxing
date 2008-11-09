/*
 * Copyright 2008 ZXing authors
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

import junit.framework.TestCase;

import java.util.BitSet;
import java.util.Random;

/**
 * @author srowen@google.com (Sean Owen)
 */
public final class ReedSolomonDecoderDataMatrixTestCase extends TestCase {

  private static final int[] DM_CODE_TEST = { 142, 164, 186 };
  private static final int[] DM_CODE_TEST_WITH_EC = { 142, 164, 186, 114, 25, 5, 88, 102 };
  private static final int DM_CODE_CORRECTABLE = (DM_CODE_TEST_WITH_EC.length - DM_CODE_TEST.length) / 2;

  private final ReedSolomonDecoder dmRSDecoder = new ReedSolomonDecoder(GF256.DATA_MATRIX_FIELD);

  public void testNoError() throws ReedSolomonException {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
    // no errors
    checkQRRSDecode(received);
  }

  public void testOneError() throws ReedSolomonException {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    Random random = new Random(0xDEADBEEFL);
    for (int i = 0; i < received.length; i++) {
      System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
      received[i] = random.nextInt(256);
      checkQRRSDecode(received);
    }
  }

  public void testMaxErrors() throws ReedSolomonException {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    Random random = new Random(0xDEADBEEFL);
    for (int i = 0; i < DM_CODE_TEST.length; i++) { // # iterations is kind of arbitrary
      System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
      // TODO we aren't testing max errors really since there is a known bug here
      corrupt(received, DM_CODE_CORRECTABLE - 1, random);
      checkQRRSDecode(received);
    }
  }

  public void testTooManyErrors() {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
    Random random = new Random(0xDEADBEEFL);
    corrupt(received, DM_CODE_CORRECTABLE + 1, random);
    try {
      checkQRRSDecode(received);
      fail("Should not have decoded");
    } catch (ReedSolomonException rse) {
      // good
    }
  }

  private void checkQRRSDecode(int[] received) throws ReedSolomonException {
    dmRSDecoder.decode(received, 2 * DM_CODE_CORRECTABLE, true);
    for (int i = 0; i < DM_CODE_TEST.length; i++) {
      assertEquals(received[i], DM_CODE_TEST[i]);
    }
  }

  private static void corrupt(int[] received, int howMany, Random random) {
    BitSet corrupted = new BitSet(received.length);
    for (int j = 0; j < howMany; j++) {
      int location = random.nextInt(received.length);
      if (corrupted.get(location)) {
        j--;
      } else {
        corrupted.set(location);
        int newByte = random.nextInt(256);
        received[location] = newByte;
      }
    }
  }
}