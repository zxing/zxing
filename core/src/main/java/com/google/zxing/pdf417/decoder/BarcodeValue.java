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

package com.google.zxing.pdf417.decoder;

import com.google.zxing.pdf417.PDF417Common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Guenther Grau
 */
final class BarcodeValue {
  private final Map<Integer,Integer> values = new HashMap<>();

  /**
   * Add an occurrence of a value
   */
  void setValue(int value) {
    Integer confidence = values.get(value);
    if (confidence == null) {
      confidence = 0;
    }
    confidence++;
    values.put(value, confidence);
  }

  /**
   * Determines the maximum occurrence of a set value and returns all values which were set with this occurrence. 
   * @return an array of int, containing the values with the highest occurrence, or null, if no value was set
   */
  int[] getValue() {
    int maxConfidence = -1;
    Collection<Integer> result = new ArrayList<>();
    for (Entry<Integer,Integer> entry : values.entrySet()) {
      if (entry.getValue() > maxConfidence) {
        maxConfidence = entry.getValue();
        result.clear();
        result.add(entry.getKey());
      } else if (entry.getValue() == maxConfidence) {
        result.add(entry.getKey());
      }
    }
    return PDF417Common.toIntArray(result);
  }

  public Integer getConfidence(int value) {
    return values.get(value);
  }

}
