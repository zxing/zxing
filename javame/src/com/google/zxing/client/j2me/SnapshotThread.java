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
import com.google.zxing.Result;

import javax.microedition.amms.control.camera.FocusControl;
import javax.microedition.lcdui.Image;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;

/**
 * @author Sean Owen (srowen@google.com)
 */
final class SnapshotThread extends Thread {

  private static SnapshotThread currentThread;

  private final ZXingMIDlet zXingMIDlet;

  SnapshotThread(ZXingMIDlet zXingMIDlet) {
    this.zXingMIDlet = zXingMIDlet;
  }

  static synchronized void startThread(ZXingMIDlet zXingMIDlet) {
    if (currentThread == null) {
      currentThread = new SnapshotThread(zXingMIDlet);
      currentThread.start();
    }
  }

  public void run() {
    Player player = zXingMIDlet.getPlayer();
    try {
      setFocus(player);
      player.stop();
      byte[] snapshot = zXingMIDlet.getVideoControl().getSnapshot(null);
      Image capturedImage = Image.createImage(snapshot, 0, snapshot.length);
      MonochromeBitmapSource source = new LCDUIImageMonochromeBitmapSource(capturedImage);
      Reader reader = new MultiFormatReader();
      Result result = reader.decode(source);
      zXingMIDlet.handleDecodedText(result.getText());
    } catch (Throwable t) {
      zXingMIDlet.showError(t);
    } finally {
      try {
        player.start();
      } catch (MediaException me) {
        // continue?
        zXingMIDlet.showError(me);
      }
      currentThread = null;
    }

  }

  private static void setFocus(Player player) throws MediaException, InterruptedException {
    FocusControl focusControl = (FocusControl)
      player.getControl("javax.microedition.amms.control.camera.FocusControl");
    if (focusControl != null) {
      if (focusControl.isMacroSupported() && !focusControl.getMacro()) {
        focusControl.setMacro(true);
      }
      if (focusControl.isAutoFocusSupported()) {
        focusControl.setFocus(FocusControl.AUTO);
        Thread.sleep(1500L); // let it focus...
        focusControl.setFocus(FocusControl.AUTO_LOCK);
      }
    }
  }

}
