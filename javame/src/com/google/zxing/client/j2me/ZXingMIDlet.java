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

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.amms.control.camera.ZoomControl;
import java.io.IOException;

/**
 * <p>The actual reader application {@link MIDlet}.</p>
 *
 * @author Sean Owen (srowen@google.com)
 */
public final class ZXingMIDlet extends MIDlet {

  private static final int NO_ZOOM = 100;
  private static final int MAX_ZOOM = 250;

  private Canvas canvas;
  private Player player;
  private VideoControl videoControl;

  Player getPlayer() {
    return player;
  }

  VideoControl getVideoControl() {
    return videoControl;
  }

  protected void startApp() throws MIDletStateChangeException {
    try {
      player = Manager.createPlayer("capture://video");
      player.realize();
      setZoom(player);
      videoControl = (VideoControl) player.getControl("VideoControl");
      canvas = new VideoCanvas(this);
      canvas.setFullScreenMode(true);
      videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, canvas);
      videoControl.setDisplayLocation(0, 0);
      videoControl.setDisplaySize(canvas.getWidth(), canvas.getHeight());
      videoControl.setVisible(true);
      player.start();
      Display.getDisplay(this).setCurrent(canvas);
    } catch (IOException ioe) {
      throw new MIDletStateChangeException(ioe.toString());
    } catch (MediaException me) {
      throw new MIDletStateChangeException(me.toString());
    }
  }

  private static void setZoom(Player player) {
    ZoomControl zoomControl = (ZoomControl) player.getControl("javax.microedition.amms.control.camera.ZoomControl");
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

  protected void pauseApp() {
    if (player != null) {
      try {
        player.stop();
      } catch (MediaException me) {
        // continue?
        showError(me);        
      }
    }
  }

  protected void destroyApp(boolean unconditional) {
    if (player != null) {
      videoControl = null;      
      try {
        player.stop();
      } catch (MediaException me) {
        // continue
      }
      player.deallocate();
      player.close();
      player = null;
    }
  }

  void stop() {
    destroyApp(false);
    notifyDestroyed();
  }

  // Convenience methods to show dialogs

  void showYesNo(String title, final String text) {
    Alert alert = new Alert(title, text, null, AlertType.CONFIRMATION);
    alert.setTimeout(Alert.FOREVER);
    final Command cancel = new Command("Cancel", Command.CANCEL, 1);
    alert.addCommand(cancel);
    CommandListener listener = new CommandListener() {
      public void commandAction(Command command, Displayable displayable) {
        if (command.getCommandType() == Command.OK) {
          try {
            platformRequest(text);
          } catch (ConnectionNotFoundException cnfe) {
            showError(cnfe);
          } finally {
            stop();
          }
        } else {
          // cancel
          Display.getDisplay(ZXingMIDlet.this).setCurrent(canvas);
        }
      }
    };
    alert.setCommandListener(listener);
    showAlert(alert);
  }

  void showAlert(String title, String text) {
    Alert alert = new Alert(title, text, null, AlertType.INFO);
    alert.setTimeout(Alert.FOREVER);
    showAlert(alert);
  }

  void showError(Throwable t) {
    showAlert(new Alert("Error", t.getMessage(), null, AlertType.ERROR));
  }

  private void showAlert(Alert alert) {
    Display display = Display.getDisplay(this);
    display.setCurrent(alert, canvas);
  }

  void handleDecodedText(String text) {
    // This is a crude imitation of the code found in module core-ext, which handles the contents
    // in a more sophisticated way. It can't be accessed from JavaME just yet because it relies
    // on URL parsing routines in java.net. This should be somehow worked around: TODO
    // For now, detect URLs in a simple way, and treat everything else as text
    if (text.startsWith("http://") || text.startsWith("https://") || maybeURLWithoutScheme(text)) {
      showYesNo("Open web page?", text);
    } else {
      showAlert("Barcode detected", text);
    }
  }

  private static boolean maybeURLWithoutScheme(String text) {
    return text.indexOf((int) '.') >= 0 && text.indexOf((int) ' ') < 0;
  }

}
