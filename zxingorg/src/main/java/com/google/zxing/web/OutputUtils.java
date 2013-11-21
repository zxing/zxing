/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.web;

/**
 * Utility functions for {@code decoderesult.jspx}.
 * 
 * @author Sean Owen
 */
public final class OutputUtils {

  private static final int BYTES_PER_LINE = 16;
  private static final int HALF_BYTES_PER_LINE = BYTES_PER_LINE / 2;

  private OutputUtils() {
  }

  public static String arrayToString(byte[] bytes) {
    StringBuilder result = new StringBuilder(bytes.length * 4);
    int i = 0;
    while (i < bytes.length) {
      int value = bytes[i] & 0xFF;
      result.append(hexChar(value / 16));
      result.append(hexChar(value % 16));
      i++;
      if (i % BYTES_PER_LINE == 0) {
        result.append('\n');
      } else if (i % HALF_BYTES_PER_LINE == 0) {
        result.append("   ");
      } else {
        result.append(' ');        
      }
    }
    return result.toString();
  }
  
  private static char hexChar(int value) {
    if (value < 0 || value > 15) {
      throw new IllegalArgumentException();
    }
    return (char) (value < 10 ? ('0' + value) : ('a' + (value - 10)));
  }
  
}
