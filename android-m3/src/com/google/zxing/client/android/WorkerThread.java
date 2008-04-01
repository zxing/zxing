/*
 * Copyright (C) 2008 Google Inc.
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

package com.google.zxing.client.android;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This thread does all the heavy lifting, both during preview and for the final capture and
 * decoding. That leaves the main thread free to handle UI tasks.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class WorkerThread extends Thread {

  private final BarcodeReaderCaptureActivity activity;
  private final CameraSurfaceView surfaceView;
  private final CameraManager cameraManager;
  private final Handler handler;
  private final Object idleLock;
  private State state;

  private enum State {
    IDLE,
    PREVIEW_LOOP,
    STILL_AND_DECODE,
    STILL_AND_DECODE_1D,
    STILL_AND_DECODE_QR,
    STILL_AND_SAVE,
    DONE
  }

  WorkerThread(BarcodeReaderCaptureActivity activity, CameraSurfaceView surfaceView,
               CameraManager cameraManager, Handler handler) {
    this.activity = activity;
    this.surfaceView = surfaceView;
    this.cameraManager = cameraManager;
    this.handler = handler;
    this.idleLock = new Object();
    state = State.IDLE;
  }

  @Override
  public void run() {
    while (true) {
      switch (state) {
        case IDLE:
          idle();
          break;
        case PREVIEW_LOOP:
          surfaceView.capturePreviewAndDraw();
          break;
        case STILL_AND_DECODE:
          takeStillAndDecode(null);
          break;
        case STILL_AND_DECODE_1D: {
          Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
          // TODO: This is fragile in case we add new formats. It would be better to have a new enum
          // value which represented all 1D formats.
          Vector vector = new Vector();
          vector.addElement(BarcodeFormat.UPC_A);
          vector.addElement(BarcodeFormat.UPC_E);
          vector.addElement(BarcodeFormat.EAN_13);
          vector.addElement(BarcodeFormat.EAN_8);
          vector.addElement(BarcodeFormat.CODE_39);
          vector.addElement(BarcodeFormat.CODE_128);
          hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
          takeStillAndDecode(hints);
          break;
        }
        case STILL_AND_DECODE_QR: {
          Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
          Vector vector = new Vector();
          vector.addElement(BarcodeFormat.QR_CODE);
          hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
          takeStillAndDecode(hints);
          break;
        }
        case STILL_AND_SAVE:
          takeStillAndSave();
          break;
        case DONE:
          return;
      }
    }
  }

  public void requestPreviewLoop() {
    state = State.PREVIEW_LOOP;
    wakeFromIdle();
  }

  public void requestStillAndDecode() {
    state = State.STILL_AND_DECODE;
    wakeFromIdle();
  }

  public void requestStillAndDecode1D() {
    state = State.STILL_AND_DECODE_1D;
    wakeFromIdle();
  }

  public void requestStillAndDecodeQR() {
    state = State.STILL_AND_DECODE_QR;
    wakeFromIdle();
  }

  public void requestStillAndSave() {
    state = State.STILL_AND_SAVE;
    wakeFromIdle();
  }

  public void requestExitAndWait() {
    state = State.DONE;
    wakeFromIdle();
    try {
      join();
    } catch (InterruptedException e) {
    }
  }

  private void idle() {
    try {
      synchronized (idleLock) {
        idleLock.wait();
      }
    } catch (InterruptedException e) {
      // Continue
    }
  }

  private void wakeFromIdle() {
    synchronized (idleLock) {
      idleLock.notifyAll();
    }
  }

  private void takeStillAndDecode(Hashtable<DecodeHintType, Object> hints) {
    Bitmap bitmap = cameraManager.captureStill();
    Result rawResult;
    try {
      MonochromeBitmapSource source = new RGBMonochromeBitmapSource(bitmap);
      rawResult = new MultiFormatReader().decode(source, hints);
    } catch (ReaderException e) {
      Message message = Message.obtain(handler, R.id.decoding_failed_message);
      message.sendToTarget();
      state = State.PREVIEW_LOOP;
      return;
    }
    Message message = Message.obtain(handler, R.id.decoding_succeeded_message, rawResult);
    message.sendToTarget();
    state = State.IDLE;
  }

  /**
   * This is a debugging feature used to take photos and save them as JPEGs using the exact camera
   * setup as in normal decoding. This is useful for building up a library of test images.
   */
  private void takeStillAndSave() {
    Bitmap bitmap = cameraManager.captureStill();
    OutputStream outStream = getNewPhotoOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
    try {
      outStream.close();
    } catch (IOException e) {
    }
    state = State.PREVIEW_LOOP;
  }

  /**
   * We prefer to write to the SD Card because it has more space, and is automatically mounted as a
   * drive over USB. If it's not present, fall back to the package's private file area here:
   *
   * /data/data/com.google.zxing.client.android/files
   *
   * @return A stream which represents the new file where the photo will be saved.
   */
  private OutputStream getNewPhotoOutputStream() {
    File sdcard = new File("/sdcard");
    if (sdcard.exists()) {
      File barcodes = new File(sdcard, "barcodes");
      if (!barcodes.exists()) {
        if (!barcodes.mkdir()) {
          return null;
        }
      }
      String fileName = getNewPhotoName(barcodes.list());
      try {
        return new FileOutputStream(new File(barcodes, fileName));
      } catch (FileNotFoundException e) {
      }
    } else {
      Application application = activity.getApplication();
      String fileName = getNewPhotoName(application.fileList());
      try {
        return application.openFileOutput(fileName, 0);
      } catch (FileNotFoundException e) {
      }
    }
    return null;
  }

  private String getNewPhotoName(String[] listOfFiles) {
    int existingFileCount = (listOfFiles != null) ? listOfFiles.length : 0;
    return "capture" + existingFileCount + ".jpg";
  }

}
