package com.google.zxing;
import com.google.zxing.aztec.detector.DetectorTest;
import com.google.zxing.aztec.encoder.EncoderTest;
import com.google.zxing.common.StringUtilsTestCase;
import com.google.zxing.multi.MultiTestCase;
import com.google.zxing.oned.CodaBarWriterTestCase;
import com.google.zxing.oned.Code39ExtendedBlackBox2TestCase;
import com.google.zxing.oned.Code39ExtendedModeTestCase;
import com.google.zxing.oned.rss.RSS14BlackBox1TestCase;
import com.google.zxing.oned.rss.RSS14BlackBox2TestCase;
import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({Code39ExtendedModeTestCase.class, Code39ExtendedBlackBox2TestCase.class, CodaBarWriterTestCase.class,
 EncoderTest.class, StringUtilsTestCase.class, DetectorTest.class, RSS14BlackBox1TestCase.class,
 RSS14BlackBox2TestCase.class, MultiTestCase.class})

public class TestSuite extends TestCase {
  /**
   * One time setup before the tests are run.
   */
  @BeforeClass
  public static void setup() {
    CoverageTool2000.initCoverageMatrix(0, 23);
    CoverageTool2000.initCoverageMatrix(1, 13);
    CoverageTool2000.initCoverageMatrix(2, 12);
    CoverageTool2000.initCoverageMatrix(3, 26);
    CoverageTool2000.initCoverageMatrix(4, 10);
    CoverageTool2000.initCoverageMatrix(5, 51);
    CoverageTool2000.initCoverageMatrix(6, 30);
    CoverageTool2000.initCoverageMatrix(7, 26);

  }
  /**
   * One time teardown after all the tests are run.
   */
  @AfterClass
  public static void teardown() {
    CoverageTool2000.checkAllCoverage(8);
  }
}
