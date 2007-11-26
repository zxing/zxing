/*
 * Copyright 2007 Google Inc.
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

package com.google.zxing.client.j2me;

import javax.microedition.media.Controllable;
import javax.microedition.media.MediaException;

/**
 * <p>See this exact same class under the "src" source root for a full explanation.
 * This is a "no-op" version of the class that gets built into the .jar file
 * which is suitable for non-JSR-234 devices.</p>
 * 
 * @author Sean Owen (srowen@google.com)
 */
final class AdvancedMultimediaManager {

  private AdvancedMultimediaManager() {
    // do nothing
  }

  // These signatures must match those in the other class exactly

  static void setFocus(Controllable player) throws MediaException, InterruptedException {
    // do nothing
  }

  static void setZoom(Controllable player) {
    // do nothing
  }

}