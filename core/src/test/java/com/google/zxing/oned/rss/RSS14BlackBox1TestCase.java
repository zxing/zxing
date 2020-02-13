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

package com.google.zxing.oned.rss;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.CoverageTool2000;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;
import org.junit.After;
import org.junit.BeforeClass;
import org.w3c.dom.ls.LSOutput;

/**
 * @author Sean Owen
 */
public final class RSS14BlackBox1TestCase extends AbstractBlackBoxTestCase {

  @BeforeClass
  public static void setUp(){
    if (CoverageTool2000.setUpIsDone) {
      return;
    }
    CoverageTool2000.initCoverageMatrix(1,13);
    CoverageTool2000.initCoverageMatrix(0,24);
    CoverageTool2000.setUpIsDone = true;
  }
  public RSS14BlackBox1TestCase() {
    super("src/test/resources/blackbox/rss14-1", new MultiFormatReader(), BarcodeFormat.RSS_14);
    addTest(6, 6, 0.0f);
    addTest(6, 6, 180.0f);
  }
  @After
  public void print2(){
    System.out.println(CoverageTool2000.checkCoverage(1));
  }
}


