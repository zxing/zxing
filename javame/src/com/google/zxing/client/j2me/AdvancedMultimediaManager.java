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

import javax.microedition.amms.control.camera.ExposureControl;
import javax.microedition.amms.control.camera.FocusControl;
import javax.microedition.amms.control.camera.ZoomControl;
import javax.microedition.amms.control.camera.FlashControl;
import javax.microedition.media.Controllable;
import javax.microedition.media.MediaException;
import javax.microedition.media.Control;

/**
 * <p>Implementation suitable for JSR-234 phones which takes advantage of advanced camera
 * capability.</p>
 *
 * @author Sean Owen
 */
final class AdvancedMultimediaManager implements MultimediaManager {

  private static final int NO_ZOOM = 100;
  private static final int MAX_ZOOM = 200;
  private static final long FOCUS_TIME_MS = 750L;
  private static final String DESIRED_METERING = "center-weighted";
  private static final int DESIRED_FLASH = FlashControl.AUTO;

  public void setFocus(Controllable player) {
    FocusControl focusControl =
        (FocusControl) getControl(player, "javax.microedition.amms.control.camera.FocusControl");
    if (focusControl != null) {
      try {
        if (focusControl.isMacroSupported() && !focusControl.getMacro()) {
          focusControl.setMacro(true);
        }
        if (focusControl.isAutoFocusSupported()) {
          focusControl.setFocus(FocusControl.AUTO);
          try {
            Thread.sleep(FOCUS_TIME_MS); // let it focus...
          } catch (InterruptedException ie) {
            // continue
          }
          focusControl.setFocus(FocusControl.AUTO_LOCK);
        }
      } catch (MediaException me) {
        // continue
      }
    }
  }

  public void setZoom(Controllable player) {
    ZoomControl zoomControl = (ZoomControl) getControl(player, "javax.microedition.amms.control.camera.ZoomControl");
    if (zoomControl != null) {
      // We zoom in if possible to encourage the viewer to take a snapshot from a greater distance.
      // This is a crude way of dealing with the fact that many phone cameras will not focus at a
      // very close range.
      int maxZoom = zoomControl.getMaxOpticalZoom();
      if (maxZoom > NO_ZOOM) {
        zoomControl.setOpticalZoom(maxZoom > MAX_ZOOM ? MAX_ZOOM : maxZoom);
      } else {
        int maxDigitalZoom = zoomControl.getMaxDigitalZoom();
        if (maxDigitalZoom > NO_ZOOM) {
          zoomControl.setDigitalZoom(maxDigitalZoom > MAX_ZOOM ? MAX_ZOOM : maxDigitalZoom);
        }
      }
    }
  }

  public void setExposure(Controllable player) {
    ExposureControl exposureControl =
        (ExposureControl) getControl(player, "javax.microedition.amms.control.camera.ExposureControl");
    if (exposureControl != null) {

      int[] supportedISOs = exposureControl.getSupportedISOs();
      if (supportedISOs != null && supportedISOs.length > 0) {
        int maxISO = Integer.MIN_VALUE;
        for (int i = 0; i < supportedISOs.length; i++) {
          if (supportedISOs[i] > maxISO) {
            maxISO = supportedISOs[i];
          }
        }
        try {
          exposureControl.setISO(maxISO);
        } catch (MediaException me) {
          // continue
        }
      }

      String[] supportedMeterings = exposureControl.getSupportedLightMeterings();
      if (supportedMeterings != null) {
        for (int i = 0; i < supportedMeterings.length; i++) {
          if (DESIRED_METERING.equals(supportedMeterings[i])) {
            exposureControl.setLightMetering(DESIRED_METERING);
            break;
          }
        }
      }

    }
  }

  public void setFlash(Controllable player) {
    FlashControl flashControl =
        (FlashControl) getControl(player, "javax.microedition.amms.control.camera.FlashControl");
    if (flashControl != null) {
      int[] supportedFlash = flashControl.getSupportedModes();
      if (supportedFlash != null && supportedFlash.length > 0) {
        for (int i = 0; i < supportedFlash.length; i++) {
          if (supportedFlash[i] == DESIRED_FLASH) {
            try {
              flashControl.setMode(DESIRED_FLASH);
            } catch (IllegalArgumentException iae) {
              // continue
            }
            break;
          }
        }
      }
    }
  }

  private static Control getControl(Controllable player, String fullName) {
    Control control = player.getControl(fullName);
    if (control == null) {
      String shortName = fullName.substring(fullName.lastIndexOf('.') + 1);
      control = player.getControl(shortName);
    }
    return control;
  }

}