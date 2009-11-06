/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.androidtest;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

final class SaveThread extends Thread {

  private static final String TAG = "SaveThread";

  public Handler mHandler;

  private final CameraTestActivity mActivity;

  SaveThread(CameraTestActivity activity) {
    mActivity = activity;
  }

  @Override
  public void run() {
    Looper.prepare();
    mHandler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        switch (message.what) {
          case R.id.save:
            save((byte[]) message.obj, message.arg1, message.arg2);
            break;
          case R.id.quit:
            Looper.myLooper().quit();
            break;
        }
      }
    };
    Looper.loop();
  }

  // Save the center rectangle of the Y channel as a greyscale PNG to the SD card.
  private void save(byte[] data, int width, int height) {
    final Rect framingRect = CameraManager.get().getFramingRect();
    int framingWidth = framingRect.width();
    int framingHeight = framingRect.height();
    if (framingWidth > width || framingHeight > height) {
      throw new IllegalArgumentException();
    }

    int leftOffset = framingRect.left;
    int topOffset = framingRect.top;
    int[] colors = new int[framingWidth * framingHeight];

    for (int y = 0; y < framingHeight; y++) {
      int rowOffset = (y + topOffset) * width + leftOffset;
      for (int x = 0; x < framingWidth; x++) {
        int pixel = (int) data[rowOffset + x];
        pixel = 0xff000000 + (pixel << 16) + (pixel << 8) + pixel;
        colors[y * framingWidth + x] = pixel;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(colors, framingWidth, framingHeight,
        Bitmap.Config.ARGB_8888);
    OutputStream outStream = getNewPhotoOutputStream();
    if (outStream != null) {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
      try {
        outStream.close();
        Message message = Message.obtain(mActivity.mHandler, R.id.save_succeeded);
        message.sendToTarget();
        return;
      } catch (IOException e) {
        Log.e(TAG, "Exception closing stream: " + e.toString());
      }
    }

    Message message = Message.obtain(mActivity.mHandler, R.id.save_failed);
    message.sendToTarget();
  }

  private static OutputStream getNewPhotoOutputStream() {
    File sdcard = new File("/sdcard");
    if (sdcard.exists()) {
      File barcodes = new File(sdcard, "barcodes");
      if (barcodes.exists()) {
        if (!barcodes.isDirectory()) {
          Log.e(TAG, "/sdcard/barcodes exists but is not a directory");
          return null;
        }
      } else {
        if (!barcodes.mkdir()) {
          Log.e(TAG, "Could not create /sdcard/barcodes directory");
          return null;
        }
      }
      Date now = new Date();
      String fileName = now.getTime() + ".png";
      try {
        return new FileOutputStream(new File(barcodes, fileName));
      } catch (FileNotFoundException e) {
        Log.e(TAG, "Could not create FileOutputStream");
      }
    } else {
      Log.e(TAG, "/sdcard does not exist");
    }
    return null;
  }

}
