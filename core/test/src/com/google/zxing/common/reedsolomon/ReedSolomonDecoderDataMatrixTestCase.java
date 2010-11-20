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

import org.junit.Test;

import java.util.Random;

/**
 * @author Sean Owen
 * @author sanfordsquires
 */
public final class ReedSolomonDecoderDataMatrixTestCase extends AbstractReedSolomonTestCase {

  private static final int[] DM_CODE_TEST = { 142, 164, 186 };
  private static final int[] DM_CODE_TEST_WITH_EC = { 142, 164, 186, 114, 25, 5, 88, 102 };
  private static final int DM_CODE_ECC_BYTES = DM_CODE_TEST_WITH_EC.length - DM_CODE_TEST.length;
  private static final int DM_CODE_CORRECTABLE = DM_CODE_ECC_BYTES / 2;

  private final ReedSolomonDecoder dmRSDecoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_256);

  @Test
  public void testNoError() throws ReedSolomonException {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
    // no errors
    checkQRRSDecode(received);
  }

  @Test
  public void testOneError() throws ReedSolomonException {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    Random random = getRandom();
    for (int i = 0; i < received.length; i++) {
      System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
      received[i] = random.nextInt(256);
      checkQRRSDecode(received);
    }
  }

  @Test
  public void testMaxErrors() throws ReedSolomonException {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    Random random = getRandom();
    for (int test : DM_CODE_TEST) { // # iterations is kind of arbitrary
      System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
      corrupt(received, DM_CODE_CORRECTABLE, random);
      checkQRRSDecode(received);
    }
  }

  @Test
  public void testTooManyErrors() {
    int[] received = new int[DM_CODE_TEST_WITH_EC.length];
    System.arraycopy(DM_CODE_TEST_WITH_EC, 0, received, 0, received.length);
    Random random = getRandom();
    corrupt(received, DM_CODE_CORRECTABLE + 1, random);
    try {
      checkQRRSDecode(received);
      fail("Should not have decoded");
    } catch (ReedSolomonException rse) {
      // good
    }
  }

  private void checkQRRSDecode(int[] received) throws ReedSolomonException {
    dmRSDecoder.decode(received, DM_CODE_ECC_BYTES);
    for (int i = 0; i < DM_CODE_TEST.length; i++) {
      assertEquals(received[i], DM_CODE_TEST[i]);
    }
  }

}