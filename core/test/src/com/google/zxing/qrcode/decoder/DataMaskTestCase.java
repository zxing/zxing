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

package com.google.zxing.qrcode.decoder;

import com.google.zxing.common.BitMatrix;
import junit.framework.TestCase;

/**
 * @author Sean Owen
 */
public final class DataMaskTestCase extends TestCase {

  public void testMask0() {
    testMaskAcrossDimensions(0, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return (i + j) % 2 == 0;
      }
    });
  }

  public void testMask1() {
    testMaskAcrossDimensions(1, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return i % 2 == 0;
      }
    });
  }

  public void testMask2() {
    testMaskAcrossDimensions(2, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return j % 3 == 0;
      }
    });
  }

  public void testMask3() {
    testMaskAcrossDimensions(3, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return (i + j) % 3 == 0;
      }
    });
  }

  public void testMask4() {
    testMaskAcrossDimensions(4, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return (i / 2 + j / 3) % 2 == 0;
      }
    });
  }

  public void testMask5() {
    testMaskAcrossDimensions(5, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return (i * j) % 2 + (i * j) % 3 == 0;
      }
    });
  }

  public void testMask6() {
    testMaskAcrossDimensions(6, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return ((i * j) % 2 + (i * j) % 3) % 2 == 0;
      }
    });
  }

  public void testMask7() {
    testMaskAcrossDimensions(7, new MaskCondition() {
      public boolean isMasked(int i, int j) {
        return ((i + j) % 2 + (i * j) % 3) % 2 == 0;
      }
    });
  }

  private void testMaskAcrossDimensions(int reference,
                                        MaskCondition condition) {
    DataMask mask = DataMask.forReference(reference);
    for (int version = 1; version <= 40; version++) {
      int dimension = 17 + 4 * version;
      testMask(mask, dimension, condition);
    }
  }

  private void testMask(DataMask mask, int dimension, MaskCondition condition) {
    BitMatrix bits = new BitMatrix(dimension);
    mask.unmaskBitMatrix(bits, dimension);
    for (int i = 0; i < dimension; i++) {
      for (int j = 0; j < dimension; j++) {
        assertEquals(
            "(" + i + ',' + j + ')',
            condition.isMasked(i, j),
            bits.get(j, i));
      }
    }
  }

  private interface MaskCondition {
    boolean isMasked(int i, int j);
  }

}
