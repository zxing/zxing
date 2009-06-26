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

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;

import javax.microedition.lcdui.Image;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VideoControl;

/**
 * Thread which does the work of capturing a frame and decoding it.
 *
 * @author Sean Owen
 */
final class SnapshotThread implements Runnable {

  private final ZXingMIDlet zXingMIDlet;
  private final Object waitLock;
  private volatile boolean done;
  private final MultimediaManager multimediaManager;
  private String bestEncoding;

  SnapshotThread(ZXingMIDlet zXingMIDlet) {
    this.zXingMIDlet = zXingMIDlet;
    waitLock = new Object();
    done = false;
    multimediaManager = ZXingMIDlet.buildMultimediaManager();
  }

  void continueRun() {
    synchronized (waitLock) {
      waitLock.notifyAll();
    }
  }

  private void waitForSignal() {
    synchronized (waitLock) {
      try {
        waitLock.wait();
      } catch (InterruptedException ie) {
        // continue
      }
    }
  }

  void stop() {
    done = true;
    continueRun();
  }

  public void run() {
    Player player = zXingMIDlet.getPlayer();
    do {
      waitForSignal();
      try {
        multimediaManager.setFocus(player);
        byte[] snapshot = takeSnapshot();
        Image capturedImage = Image.createImage(snapshot, 0, snapshot.length);
        LuminanceSource source = new LCDUIImageLuminanceSource(capturedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        Reader reader = new MultiFormatReader();
        Result result = reader.decode(bitmap);
        zXingMIDlet.handleDecodedText(result);
      } catch (ReaderException re) {
        // Show a friendlier message on a mere failure to read the barcode
        zXingMIDlet.showError("Sorry, no barcode was found.");
      } catch (MediaException me) {
        zXingMIDlet.showError(me);
      } catch (RuntimeException re) {
        zXingMIDlet.showError(re);
      }
    } while (!done);
  }

  private byte[] takeSnapshot() throws MediaException {

    String bestEncoding = guessBestEncoding();

    VideoControl videoControl = zXingMIDlet.getVideoControl();
    byte[] snapshot = null;
    try {
      snapshot = videoControl.getSnapshot("".equals(bestEncoding) ? null : bestEncoding);
    } catch (MediaException me) {
    }
    if (snapshot == null) {
      // Fall back on JPEG; seems that some cameras default to PNG even
      // when PNG isn't supported!
      snapshot = videoControl.getSnapshot("encoding=jpeg");
      if (snapshot == null) {
        throw new MediaException("Can't obtain a snapshot");
      }
    }
    return snapshot;
  }

  private synchronized String guessBestEncoding() throws MediaException {
    if (bestEncoding == null) {
      // Check this property, present on some Nokias?
      String supportsVideoCapture = System.getProperty("supports.video.capture");
      if ("false".equals(supportsVideoCapture)) {
        throw new MediaException("supports.video.capture is false");
      }

      bestEncoding = "";
      String videoSnapshotEncodings = System.getProperty("video.snapshot.encodings");
      if (videoSnapshotEncodings != null) {
        // We know explicitly what the camera supports; see if PNG is among them since
        // Image.createImage() should always support it
        int pngEncodingStart = videoSnapshotEncodings.indexOf("encoding=png");
        if (pngEncodingStart >= 0) {
          int space = videoSnapshotEncodings.indexOf(' ', pngEncodingStart);
          bestEncoding = space >= 0 ?
              videoSnapshotEncodings.substring(pngEncodingStart, space) :
              videoSnapshotEncodings.substring(pngEncodingStart);
        }
      }
    }
    return bestEncoding;
  }

}
