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
 * <p>This class encapsulates optional multimedia-related operations that the device
 * may support, like setting focus and zoom. This implementation itself will do nothing.
 * It will attempt to dynamically instantiate {@link com.google.zxing.client.j2me.AdvancedMultimediaManager}
 * which has methods that call JSR-234 APIs to actually set focus, zoom, etc. If successful,
 * this class will delegate to that implementation. But if the phone does not support these
 * APIs, instantiation will simply fail and this implementation will do nothing.</p>
 *
 * <p>Credit to Paul Hackenberger for the nice workaround</p>
 *
 * @author Sean Owen (srowen@google.com)
 * @author Paul Hackenberger
 */
class DefaultMultimediaManager implements MultimediaManager {

  private MultimediaManager advancedMultimediaManager;

  DefaultMultimediaManager() {
    try {
      advancedMultimediaManager = (MultimediaManager)
          Class.forName("com.google.zxing.client.j2me.AdvancedMultimediaManager").newInstance();
    } catch (ClassNotFoundException cnfe) {
      // continue
    } catch (IllegalAccessException iae) {
      // continue
    } catch (InstantiationException ie) {
      // continue
    } catch (NoClassDefFoundError ncdfe) {
      // continue
    }
  }

  public void setFocus(Controllable player) {
    if (advancedMultimediaManager != null) {
      advancedMultimediaManager.setFocus(player);
    }
  }

  public void setZoom(Controllable player) {
    if (advancedMultimediaManager != null) {
      advancedMultimediaManager.setZoom(player);
    }
  }

  public void setExposure(Controllable player) {
    if (advancedMultimediaManager != null) {
      advancedMultimediaManager.setExposure(player);
    }
  }

}