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

package com.google.zxing.common;

import junit.framework.TestCase;

import java.util.Random;
import java.util.Vector;

/**
 * @author Sean Owen
 */
public final class CollectionsTestCase extends TestCase {

  public void testSort() {
    Random r = new Random(0xDEADBEEFL);
    Vector<Integer> v = new Vector<Integer>(100);
    for (int i = 0; i < 100; i++) {
      v.addElement(r.nextInt(1000));
    }
    Collections.insertionSort(v, new Comparator() {
      public int compare(Object o1, Object o2) {
        return (Integer) o1 - (Integer) o2;
      }
    });
    for (int i = 1; i < 100; i++) {
      assertTrue("Element " + i, v.elementAt(i - 1) <= v.elementAt(i));
    }
  }

}