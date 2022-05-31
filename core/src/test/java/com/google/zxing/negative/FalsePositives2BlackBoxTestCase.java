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
 * Additional random images with high contrast patterns which should not find any barcodes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class FalsePositives2BlackBoxTestCase extends AbstractNegativeBlackBoxTestCase {

  public FalsePositives2BlackBoxTestCase() {
    super("src/test/resources/blackbox/falsepositives-2");
    addTest(4, 0.0f);
    addTest(4, 90.0f);
    addTest(4, 180.0f);
    addTest(4, 270.0f);
  }

}
