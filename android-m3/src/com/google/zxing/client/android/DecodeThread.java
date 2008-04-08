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
import android.os.Looper;
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
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This thread does all the heavy lifting of decoding the images. It can also save images to flash
 * for debugging purposes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {

  public Handler handler;

  private final BarcodeReaderCaptureActivity activity;
  private final CameraManager cameraManager;
  private Hashtable<DecodeHintType, Object> hints;
  private Handler cameraThreadHandler;

  DecodeThread(BarcodeReaderCaptureActivity activity, CameraManager cameraManager) {
    this.activity = activity;
    this.cameraManager = cameraManager;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new Handler() {
      public void handleMessage(Message message) {
        switch (message.what) {
          case R.id.decode:
            captureAndDecode();
            break;
          case R.id.save:
            captureAndSave();
            break;
          case R.id.quit:
            Looper.myLooper().quit();
            break;
          case R.id.set_decode_all_mode:
            setDecodeAllMode();
            break;
          case R.id.set_decode_1D_mode:
            setDecode1DMode();
            break;
          case R.id.set_decode_QR_mode:
            setDecodeQRMode();
            break;
        }
      }
    };
    Looper.loop();
  }

  public void setCameraThreadHandler(Handler cameraThreadHandler) {
    this.cameraThreadHandler = cameraThreadHandler;
  }

  private void setDecodeAllMode() {
    hints = null;
  }

  // TODO: This is fragile in case we add new formats. It would be better to have a new enum
  // value which represented all 1D formats.
  private void setDecode1DMode() {
    hints = new Hashtable<DecodeHintType, Object>(3);
    Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>();
    vector.addElement(BarcodeFormat.UPC_A);
    vector.addElement(BarcodeFormat.UPC_E);
    vector.addElement(BarcodeFormat.EAN_13);
    vector.addElement(BarcodeFormat.EAN_8);
    vector.addElement(BarcodeFormat.CODE_39);
    vector.addElement(BarcodeFormat.CODE_128);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
  }

  private void setDecodeQRMode() {
    hints = new Hashtable<DecodeHintType, Object>(3);
    Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>();
    vector.addElement(BarcodeFormat.QR_CODE);
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
  }

  private void captureAndDecode() {
    Date startDate = new Date();
    Bitmap bitmap = cameraManager.captureStill();
    // Let the CameraThread know it can resume previews while the decoding continues in parallel.
    Message restart = Message.obtain(cameraThreadHandler, R.id.decode_started);
    restart.sendToTarget();

    Result rawResult;
    try {
      MonochromeBitmapSource source = new RGBMonochromeBitmapSource(bitmap);
      rawResult = new MultiFormatReader().decode(source, hints);
    } catch (ReaderException e) {
      Message failure = Message.obtain(cameraThreadHandler, R.id.decode_failed);
      failure.sendToTarget();
      return;
    }
    Date endDate = new Date();
    Message success = Message.obtain(cameraThreadHandler, R.id.decode_succeeded, rawResult);
    success.arg1 = (int) (endDate.getTime() - startDate.getTime());
    success.sendToTarget();
  }

  /**
   * This is a debugging feature used to take photos and save them as JPEGs using the exact camera
   * setup as in normal decoding. This is useful for building up a library of test images.
   */
  private void captureAndSave() {
    Bitmap bitmap = cameraManager.captureStill();
    OutputStream outStream = getNewPhotoOutputStream();
    if (outStream != null) {
      bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
      try {
        outStream.close();
      } catch (IOException e) {
      }
      Message success = Message.obtain(cameraThreadHandler, R.id.save_succeeded);
      success.sendToTarget();
    } else {
      Message failure = Message.obtain(cameraThreadHandler, R.id.save_failed);
      failure.sendToTarget();
    }
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
      String fileName = getNewPhotoName();
      try {
        return new FileOutputStream(new File(barcodes, fileName));
      } catch (FileNotFoundException e) {
      }
    } else {
      Application application = activity.getApplication();
      String fileName = getNewPhotoName();
      try {
        return application.openFileOutput(fileName, 0);
      } catch (FileNotFoundException e) {
      }
    }
    return null;
  }

  private String getNewPhotoName() {
    Date now = new Date();
    return "capture" + now.getTime() + ".jpg";
  }

}
