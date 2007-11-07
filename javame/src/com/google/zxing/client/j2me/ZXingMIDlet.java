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

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import java.io.IOException;

/**
 * @author Sean Owen (srowen@google.com)
 */
public final class ZXingMIDlet extends MIDlet implements CommandListener {

  private static final Command DECODE = new Command("Decode", Command.SCREEN, 1);
  private static final Command EXIT = new Command("Exit", Command.EXIT, 1);

  private Player player;
  private VideoControl videoControl;

  protected void startApp() throws MIDletStateChangeException {
    try {
      player = Manager.createPlayer("capture://video");
      player.realize();
      videoControl = (VideoControl) player.getControl("VideoControl");
      Displayable canvas = new VideoCanvas();
      videoControl.initDisplayMode(VideoControl.USE_DIRECT_VIDEO, canvas);
      videoControl.setDisplayLocation(0, 0);
      videoControl.setDisplaySize(canvas.getWidth(), canvas.getHeight());
      videoControl.setVisible(true);
      /*
      FocusControl focusControl = (FocusControl)
          player.getControl("javax.microedition.amms.control.FocusControl");
      if (focusControl != null) {
        if (focusControl.isAutoFocusSupported()) {
          focusControl.setFocus(FocusControl.AUTO);
        }
        if (focusControl.isMacroSupported()) {
          focusControl.setMacro(true);
        }
      } else {
        System.out.println("FocusControl not supported");
      }
       */
      canvas.addCommand(DECODE);
      canvas.addCommand(EXIT);
      canvas.setCommandListener(this);
      Display.getDisplay(this).setCurrent(canvas);
      player.start();
    } catch (IOException ioe) {
      throw new MIDletStateChangeException(ioe.toString());
    } catch (MediaException me) {
      throw new MIDletStateChangeException(me.toString());
    }
  }

  public void commandAction(Command command, Displayable displayable) {
    if (command.equals(DECODE)) {
      new SnapshotThread().start();
    } else if (command.equals(EXIT)) {
      destroyApp(false);
      notifyDestroyed();
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
      player.close();
      player = null;
      videoControl = null;
    }
  }

  private void showAlert(String title, String text) {
    Alert alert = new Alert(title, text, null, AlertType.INFO);
    alert.setTimeout(Alert.FOREVER);
    showAlert(alert);
  }

  private void showError(Throwable t) {
    showAlert(new Alert("Error", t.getMessage(), null, AlertType.ERROR));
  }

  private void showAlert(Alert alert) {
    Display display = Display.getDisplay(this);
    display.setCurrent(alert, display.getCurrent());
  }

  private static class VideoCanvas extends Canvas {
    protected void paint(Graphics graphics) {
      // do nothing
    }
  }

  private class SnapshotThread extends Thread {
    public void run() {
      try {
        player.stop();
        byte[] snapshot = videoControl.getSnapshot(null);
        Image capturedImage = Image.createImage(snapshot, 0, snapshot.length);
        MonochromeBitmapSource source = new LCDUIImageMonochromeBitmapSource(capturedImage);
        Reader reader = new MultiFormatReader();
        Result result = reader.decode(source);
        showAlert("Barcode detected", result.getText());
      } catch (ReaderException re) {
        showError(re);
      } catch (MediaException me) {
        showError(me);
      } catch (Throwable t) {
        showError(t);
      } finally {
        try {
          player.start();
        } catch (MediaException me) {
          // continue?
          showError(me);
        }
      }

    }
  }

}
