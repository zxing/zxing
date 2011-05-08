/*
 * Copyright 2011 ZXing authors
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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the collection of all images files/URLs to decode.
 */
final class Inputs {

  private final List<String> inputs = new ArrayList<String>(10);
  private int position = 0;

  public synchronized void addInput(String pathOrUrl) {
    inputs.add(pathOrUrl);
  }

  public synchronized String getNextInput() {
    if (position < inputs.size()) {
      String result = inputs.get(position);
      position++;
      return result;
    } else {
      return null;
    }
  }

  public synchronized int getInputCount() {
    return inputs.size();
  }
}
