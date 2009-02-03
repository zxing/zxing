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

package com.google.zxing.client.j2me;

import javax.microedition.media.Controllable;

/**
 * <p>Dummy implemenation which does nothing. This is suitable for non-JSR-234 phones.</p>
 *
 * @author Sean Owen
 */
final class DefaultMultimediaManager implements MultimediaManager {

  public void setFocus(Controllable player) {
  }

  public void setZoom(Controllable player) {
  }

  public void setExposure(Controllable player) {
  }

  public void setFlash(Controllable player) {
  }

}