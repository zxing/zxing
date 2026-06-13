/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.oned.rss.expanded.decoders;

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;
import com.google.zxing.oned.rss.expanded.BinaryUtil;

import org.junit.Test;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public final class AI01AndOtherAIsTest extends AbstractDecoderTest {

  // first bit is the linkage flag, second bit selects this decoder
  @Test(expected = NotFoundException.class)
  public void testTruncatedSymbol() throws Exception {
    // 24 bits, too few to hold the compressed GTIN this decoder always reads
    BitArray binary = BinaryUtil.buildBitArrayFromStringWithoutSpaces(".X......................");
    AbstractExpandedDecoder.createDecoder(binary).parseInformation();
  }
}
