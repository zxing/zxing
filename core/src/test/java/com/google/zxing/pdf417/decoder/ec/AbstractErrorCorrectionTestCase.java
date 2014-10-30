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

import com.google.zxing.common.reedsolomon.ReedSolomonTestCase;
import org.junit.Assert;

import java.util.BitSet;
import java.util.Random;

/**
 * @author Sean Owen
 */
abstract class AbstractErrorCorrectionTestCase extends Assert {

  static void corrupt(int[] received, int howMany, Random random) {
    ReedSolomonTestCase.corrupt(received, howMany, random, 929);
  }

  static int[] erase(int[] received, int howMany, Random random) {
    BitSet erased = new BitSet(received.length);
    int[] erasures = new int[howMany];
    int erasureOffset = 0;
    for (int j = 0; j < howMany; j++) {
      int location = random.nextInt(received.length);
      if (erased.get(location)) {
        j--;
      } else {
        erased.set(location);
        received[location] = 0;
        erasures[erasureOffset++] = location;
      }
    }
    return erasures;
  }

  static Random getRandom() {
    return new Random(0xDEADBEEF);
  }

}