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

package com.google.zxing;

import com.google.zxing.common.AbstractBlackBoxTestCase;
import com.google.zxing.datamatrix.DataMatrixBlackBox1TestCase;
import com.google.zxing.datamatrix.DataMatrixBlackBox2TestCase;
import com.google.zxing.oned.Code128BlackBox1TestCase;
import com.google.zxing.oned.Code128BlackBox2TestCase;
import com.google.zxing.oned.Code128BlackBox3TestCase;
import com.google.zxing.oned.Code39BlackBox1TestCase;
import com.google.zxing.oned.Code39BlackBox3TestCase;
import com.google.zxing.oned.Code39ExtendedBlackBox2TestCase;
import com.google.zxing.oned.EAN13BlackBox1TestCase;
import com.google.zxing.oned.EAN13BlackBox2TestCase;
import com.google.zxing.oned.EAN13BlackBox3TestCase;
import com.google.zxing.oned.EAN13BlackBox4TestCase;
import com.google.zxing.oned.EAN8BlackBox1TestCase;
import com.google.zxing.oned.ITFBlackBox1TestCase;
import com.google.zxing.oned.ITFBlackBox2TestCase;
import com.google.zxing.oned.UPCABlackBox1TestCase;
import com.google.zxing.oned.UPCABlackBox2TestCase;
import com.google.zxing.oned.UPCABlackBox3ReflectiveTestCase;
import com.google.zxing.oned.UPCABlackBox4TestCase;
import com.google.zxing.oned.UPCABlackBox5TestCase;
import com.google.zxing.oned.UPCEBlackBox1TestCase;
import com.google.zxing.oned.UPCEBlackBox2TestCase;
import com.google.zxing.oned.UPCEBlackBox3ReflectiveTestCase;
import com.google.zxing.pdf417.PDF417BlackBox1TestCase;
import com.google.zxing.pdf417.PDF417BlackBox2TestCase;
import com.google.zxing.qrcode.QRCodeBlackBox1TestCase;
import com.google.zxing.qrcode.QRCodeBlackBox2TestCase;
import com.google.zxing.qrcode.QRCodeBlackBox3TestCase;
import com.google.zxing.qrcode.QRCodeBlackBox4TestCase;
import com.google.zxing.qrcode.QRCodeBlackBox5TestCase;

/**
 * This is a quick and dirty way to get totals across all the positive black box tests. It is
 * necessary because we spawn multiple processes when using the standard test-blackbox Ant target.
 * It would be a shame to change that because it does help with performance. Perhaps we can find a
 * way to unify these in the future.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class AllPositiveBlackBoxTester {

  // This list has to be manually kept up to date. I don't know any automatic way to include every
  // subclass of AbstractBlackBoxTestCase, and furthermore to exclude subclasses of
  // AbstractNegativeBlackBoxTestCase which derives from it.
  private static final AbstractBlackBoxTestCase[] TESTS = {
    new DataMatrixBlackBox1TestCase(),
    new DataMatrixBlackBox2TestCase(),
    new Code128BlackBox1TestCase(),
    new Code128BlackBox2TestCase(),
    new Code128BlackBox3TestCase(),
    new Code39BlackBox1TestCase(),
    new Code39ExtendedBlackBox2TestCase(),
    new Code39BlackBox3TestCase(),
    new EAN13BlackBox1TestCase(),
    new EAN13BlackBox2TestCase(),
    new EAN13BlackBox3TestCase(),
    new EAN13BlackBox4TestCase(),
    new EAN8BlackBox1TestCase(),
    new ITFBlackBox1TestCase(),
    new ITFBlackBox2TestCase(),
    new UPCABlackBox1TestCase(),
    new UPCABlackBox2TestCase(),
    new UPCABlackBox3ReflectiveTestCase(),
    new UPCABlackBox4TestCase(),
    new UPCABlackBox5TestCase(),
    new UPCEBlackBox1TestCase(),
    new UPCEBlackBox2TestCase(),
    new UPCEBlackBox3ReflectiveTestCase(),
    new PDF417BlackBox1TestCase(),
    new PDF417BlackBox2TestCase(),
    new QRCodeBlackBox1TestCase(),
    new QRCodeBlackBox2TestCase(),
    new QRCodeBlackBox3TestCase(),
    new QRCodeBlackBox4TestCase(),
    new QRCodeBlackBox5TestCase()
  };

//  private static final AbstractBlackBoxTestCase[] TESTS1 = {
//    new DataMatrixBlackBox1TestCase(),
//    new DataMatrixBlackBox2TestCase(),
//    new Code128BlackBox1TestCase(),
//    new Code128BlackBox2TestCase(),
//    new Code128BlackBox3TestCase(),
//    new Code39BlackBox1TestCase(),
//    new Code39ExtendedBlackBox2TestCase(),
//    new Code39BlackBox3TestCase(),
//    new EAN13BlackBox1TestCase(),
//    new EAN13BlackBox2TestCase(),
//    new EAN13BlackBox3TestCase(),
//    new EAN13BlackBox4TestCase(),
//    new EAN8BlackBox1TestCase(),
//    new ITFBlackBox1TestCase(),
//    new ITFBlackBox2TestCase(),
//  };
//
//  private static final AbstractBlackBoxTestCase[] TESTS2 = {
//    new UPCABlackBox1TestCase(),
//    new UPCABlackBox2TestCase(),
//    new UPCABlackBox3ReflectiveTestCase(),
//    new UPCABlackBox4TestCase(),
//    new UPCABlackBox5TestCase(),
//    new UPCEBlackBox1TestCase(),
//    new UPCEBlackBox2TestCase(),
//    new UPCEBlackBox3ReflectiveTestCase(),
//    new QRCodeBlackBox1TestCase(),
//    new QRCodeBlackBox2TestCase(),
//    new QRCodeBlackBox3TestCase(),
//    new QRCodeBlackBox4TestCase(),
//    new QRCodeBlackBox5TestCase()
//  };
//
//  private static class WorkerThread extends Thread {
//
//    private AbstractBlackBoxTestCase[] testCases;
//    private AbstractBlackBoxTestCase.SummaryResults results;
//
//    public WorkerThread(AbstractBlackBoxTestCase[] tests) {
//      testCases = tests;
//    }
//
//    @Override
//    public void run() {
//      try {
//        results = new AbstractBlackBoxTestCase.SummaryResults();
//        for (int x = 0; x < testCases.length; x++) {
//          results.add(testCases[x].testBlackBoxCountingResults());
//        }
//      } catch (IOException e) {
//
//      }
//    }
//
//    public AbstractBlackBoxTestCase.SummaryResults getResults() {
//      return results;
//    }
//  }

  private AllPositiveBlackBoxTester() {

  }

  public static void main(String[] args) throws Exception {
    long now = System.currentTimeMillis();
    AbstractBlackBoxTestCase.SummaryResults results = new AbstractBlackBoxTestCase.SummaryResults();

    for (AbstractBlackBoxTestCase test : TESTS) {
      results.add(test.testBlackBoxCountingResults(false));
    }

    // This threaded version can't be used yet because BlackPointEstimator (and possibly other code)
    // is not thread-safe. It's also pretty lame. It would be much better to spawn a group of
    // threads which pull tests from a shared, locked list as needed.
//    WorkerThread thread1 = new WorkerThread(TESTS1);
//    WorkerThread thread2 = new WorkerThread(TESTS2);
//    thread1.start();
//    thread2.start();
//    thread1.join();
//    thread2.join();
//    results.add(thread1.getResults());
//    results.add(thread2.getResults());

    now = System.currentTimeMillis() - now;
    System.out.println(results.toString() + "\n  Total time: " + now + " ms");
  }
}
