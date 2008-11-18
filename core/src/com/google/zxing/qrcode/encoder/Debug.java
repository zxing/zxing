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

package com.google.zxing.qrcode.encoder;

/**
 * JAVAPORT: Equivalent methods to the C++ versions. It's debateable whether these should throw or
 * assert. We can revisit that decision, or remove these throughout the code later.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public class Debug {

  public static void LOG_ERROR(String message) {
    // Can't use IllegalStateException unfortunately in J2ME
    // TODO do something else with this anyway
    throw new RuntimeException(message);
  }

  public static void LOG_INFO(String message) {
    throw new RuntimeException(message);
  }

  public static void DCHECK(boolean condition) {
    if (!condition) {
      throw new RuntimeException();
    }
  }

  public static void DCHECK_LT(int a, int b) {
    DCHECK(a < b);
  }

  public static void DCHECK_LE(int a, int b) {
    DCHECK(a <= b);
  }

  public static void DCHECK_GT(int a, int b) {
    DCHECK(a > b);
  }

  public static void DCHECK_GE(int a, int b) {
    DCHECK(a >= b);
  }

  public static void DCHECK_EQ(int a, int b) {
    DCHECK(a == b);
  }

}
