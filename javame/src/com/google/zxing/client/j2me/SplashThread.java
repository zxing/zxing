/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.j2me;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * <p>Any professional software renders a "splash" screen which not only looks
 * great, but also keeps a user entertained (and instantly acknowledging the
 * user's request to load the application) while important application
 * background initialisation takes place.</p>
 *
 * @author Simon Flannery (Ericsson)
 */
class SplashThread extends Canvas implements Runnable {

  private final ZXingMIDlet zXingMIDlet;
  private final long tout;
  private final Image image;

  /**
   * Creates a new Splash Canvas with the given Parent Form and self Time out
   * dismissal. The Time out is described in milliseconds. If the Time out is
   * assigned Zero, then the Splash Screen will NOT Self dismiss!
   *
   * When the Splash Screen is dismissed, the splashDone method of the parent form
   * is called.
   *
   * The Splash screen may be dismissed using any of the following procedures:
   * (1) The specified timeout has elapsed. (Recommended). If Time out is zero
   * however, then the timeout is not taken into consideration and the Splash
   * screen simply waits (forever) until notified (see below)!
   * (2) By invoking the stop method. (Recommended). This would be used to
   * prematurely dismiss the splash screen BEFORE timeout has been reached
   * or if a timeout of Zero was given.
   *
   * @param parent      ZXing MIDlet Parent.
   * @param timeOut     timeout in milliseconds.
   * @param splashImage image to display.
   */
  SplashThread(ZXingMIDlet parent, long timeOut, Image splashImage) {
    zXingMIDlet = parent;
    tout = timeOut;
    image = splashImage;
    setFullScreenMode(true);
    new Thread(this).start();
  }

  /**
   * Thread Implementation Required. Invoked via calling start method.
   * DO NOT manually call this method!
   */
  public void run() {
    synchronized (this) {
      try {
        repaint();
        wait(tout);  // Let's wake up
      } catch (InterruptedException ie) {
        // will not occur in MIDP, no thread interrupt method
      }
    }

    zXingMIDlet.splashDone();
  }

  /**
   * Allows Early dismissal of the splash Screen, for example, when all background
   * initialisations are complete.
   */
  public void stop() {
    // Invoke the notify method of the Splash Object ("and the correct thread just
    // happens to be arbitrarily chosen as the thread to be awakened"), and thus
    // dismiss the Splash Screen. The current implementation only uses a single
    // thread, so invoking the notify method should work, however, the
    // implementation may change in the future. Thus lets' make use of the
    // notifyAll method of the Splash Object.
    synchronized (this) {
      notifyAll(); // Wake everyone up
    }
  }

  /**
   * Must provide this implementation - used to paint the canvas.
   *
   * @param g Some Canvas Graphics.
   */
  public void paint(Graphics g) {
    int width = getWidth();
    int height = getHeight();

    g.setColor(0x00FFFFFF);
    g.fillRect(0, 0, width, height);

    if (image != null) {
      g.drawImage(image, width / 2, height / 2, Graphics.VCENTER | Graphics.HCENTER);
    }

    Font F = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

    g.setFont(F);
    g.setColor(0x00000000);

    String vendor = zXingMIDlet.getAppProperty("MIDlet-Description");

    if (vendor != null) {
      g.drawString(vendor, width / 2, height - (height / 8), Graphics.BOTTOM | Graphics.HCENTER);
    }
  }

}
