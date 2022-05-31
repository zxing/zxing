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

package com.google.zxing.negative;

import com.google.zxing.common.AbstractNegativeBlackBoxTestCase;

/**
 * This test ensures that partial barcodes do not decode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class PartialBlackBoxTestCase extends AbstractNegativeBlackBoxTestCase {

  public PartialBlackBoxTestCase() {
    super("src/test/resources/blackbox/partial");
    addTest(1, 0.0f);
    addTest(1, 90.0f);
    addTest(1, 180.0f);
    addTest(1, 270.0f);
  }

}
