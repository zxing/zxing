/*
 * Copyright 2018 ZXing authors
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

package com.google.zxing.client.j2se;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link CommandLineRunner}.
 */
public final class CommandLineRunnerTestCase extends Assert {

  @Test
  public void testCommandLineRunner() throws Exception {
    String[] args = { "--pure_barcode", DecodeWorkerTestCase.IMAGE_DATA_URI };
    // Not a lot to do here but make sure it runs
    CommandLineRunner.main(args);
  }

}
