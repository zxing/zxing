/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android.encode;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.R;

final class EncodeThread extends Thread {

  private static final String TAG = EncodeThread.class.getSimpleName();

  private final String contents;
  private final Handler handler;
  private final int pixelResolution;
  private final BarcodeFormat format;

  EncodeThread(String contents, Handler handler, int pixelResolution, BarcodeFormat format) {
    this.contents = contents;
    this.handler = handler;
    this.pixelResolution = pixelResolution;
    this.format = format;
  }

  @Override
  public void run() {
    try {
      Bitmap bitmap = QRCodeEncoder.encodeAsBitmap(contents, format, pixelResolution, pixelResolution);
      Message message = Message.obtain(handler, R.id.encode_succeeded);
      message.obj = bitmap;
      message.sendToTarget();
    } catch (WriterException e) {
      Log.e(TAG, "Could not encode barcode", e);
      Message message = Message.obtain(handler, R.id.encode_failed);
      message.sendToTarget();
    } catch (IllegalArgumentException e) {
      Log.e(TAG, "Could not encode barcode", e);
      Message message = Message.obtain(handler, R.id.encode_failed);
      message.sendToTarget();
    }
  }
}
