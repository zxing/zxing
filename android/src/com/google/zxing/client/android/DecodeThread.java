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

package com.google.zxing.client.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.GlobalHistogramBinarizer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Hashtable;
import java.util.Vector;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {
  public static final String BARCODE_BITMAP = "barcode_bitmap";
  private static final String TAG = "DecodeThread";

  private Handler handler;
  private final CaptureActivity activity;
  private final MultiFormatReader multiFormatReader;
  private final ResultPointCallback resultPointCallback;

  DecodeThread(CaptureActivity activity, String mode, ResultPointCallback resultPointCallback) {
    this.activity = activity;
    multiFormatReader = new MultiFormatReader();
    this.resultPointCallback = resultPointCallback;

    // The prefs can't change while the thread is running, so pick them up once here.
    if (mode == null || mode.length() == 0) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
      boolean decode1D = prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D, true);
      boolean decodeQR = prefs.getBoolean(PreferencesActivity.KEY_DECODE_QR, true);
      if (decode1D && decodeQR) {
        setDecodeAllMode();
      } else if (decode1D) {
        setDecode1DMode();
      } else if (decodeQR) {
        setDecodeQRMode();
      }
    } else {
      if (mode.equals(Intents.Scan.PRODUCT_MODE)) {
        setDecodeProductMode();
      } else if (mode.equals(Intents.Scan.ONE_D_MODE)) {
        setDecode1DMode();
      } else if (mode.equals(Intents.Scan.QR_CODE_MODE)) {
        setDecodeQRMode();
      } else {
        setDecodeAllMode();
      }
    }
  }

  Handler getHandler() {
    return handler;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new Handler() {
      @Override
      public void handleMessage(Message message) {
        switch (message.what) {
          case R.id.decode:
            decode((byte[]) message.obj, message.arg1, message.arg2);
            break;
          case R.id.quit:
            Looper.myLooper().quit();
            break;
        }
      }
    };
    Looper.loop();
  }

  private void setDecodeProductMode() {
    doSetDecodeMode(BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8);
  }

  /**
   * Select the 1D formats we want this client to decode by hand.
   */
  private void setDecode1DMode() {
    doSetDecodeMode(BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.ITF);
  }

  private void setDecodeQRMode() {
    doSetDecodeMode(BarcodeFormat.QR_CODE);
  }

  /**
   * Instead of calling setHints(null), which would allow new formats to sneak in, we
   * explicitly set which formats are available.
   */
  private void setDecodeAllMode() {
    doSetDecodeMode(BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.ITF,
                    BarcodeFormat.QR_CODE);
  }

  private void doSetDecodeMode(BarcodeFormat... formats) {
    Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
    Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>(formats.length);
    for (BarcodeFormat format : formats) {
      vector.addElement(format);
    }
    hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
    multiFormatReader.setHints(hints);
  }

  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   *
   * @param data   The YUV preview frame.
   * @param width  The width of the preview frame.
   * @param height The height of the preview frame.
   */
  private void decode(byte[] data, int width, int height) {
    long start = System.currentTimeMillis();
    Result rawResult = null;
    PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);
    BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
    try {
      rawResult = multiFormatReader.decodeWithState(bitmap);
    } catch (ReaderException re) {
      // continue
    }

    if (rawResult != null) {
      long end = System.currentTimeMillis();
      Log.v(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());
      Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
      Bundle bundle = new Bundle();
      bundle.putParcelable(BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
      message.setData(bundle);
      message.sendToTarget();
    } else {
      Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
      message.sendToTarget();
    }
  }
}
