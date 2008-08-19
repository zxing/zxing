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

package com.android.barcodes;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This thread does all the heavy lifting of decoding the images.
 */
final class DecodeThread extends Thread {

    public Handler mHandler;
    private BarcodesCaptureActivity mActivity;
    private MultiFormatReader mMultiFormatReader;

    DecodeThread(BarcodesCaptureActivity activity, String mode) {
        mActivity = activity;
        mMultiFormatReader = new MultiFormatReader();

        // The prefs can't change while the thread is running, so pick them up once here.
        if (mode == null || mode.length() == 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            boolean decode1D = prefs.getBoolean(BarcodesPreferenceActivity.KEY_DECODE_1D, true);
            boolean decodeQR = prefs.getBoolean(BarcodesPreferenceActivity.KEY_DECODE_QR, true);
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

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new Handler() {
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
        Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
        Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>();
        vector.addElement(BarcodeFormat.UPC_A);
        vector.addElement(BarcodeFormat.UPC_E);
        vector.addElement(BarcodeFormat.EAN_13);
        vector.addElement(BarcodeFormat.EAN_8);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
        mMultiFormatReader.setHints(hints);
    }

    // TODO: This is fragile in case we add new formats. It would be better to have a new enum
    // value which represented all 1D formats.
    private void setDecode1DMode() {
        Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
        Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>();
        vector.addElement(BarcodeFormat.UPC_A);
        vector.addElement(BarcodeFormat.UPC_E);
        vector.addElement(BarcodeFormat.EAN_13);
        vector.addElement(BarcodeFormat.EAN_8);
        vector.addElement(BarcodeFormat.CODE_39);
        vector.addElement(BarcodeFormat.CODE_128);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
        mMultiFormatReader.setHints(hints);
    }

    private void setDecodeQRMode() {
        Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
        Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>();
        vector.addElement(BarcodeFormat.QR_CODE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
        mMultiFormatReader.setHints(hints);
    }

    private void setDecodeAllMode() {
        mMultiFormatReader.setHints(null);
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
        Date startDate = new Date();
        boolean success;
        Result rawResult = null;
        try {
            MonochromeBitmapSource source = new YUVMonochromeBitmapSource(data, width, height,
                    CameraManager.get().getFramingRect());
            rawResult = mMultiFormatReader.decodeWithState(source);
            success = true;
        } catch (ReaderException e) {
            success = false;
        }
        Date endDate = new Date();

        if (success) {
            Message message = Message.obtain(mActivity.mHandler, R.id.decode_succeeded, rawResult);
            message.arg1 = (int) (endDate.getTime() - startDate.getTime());
            message.sendToTarget();
        } else {
            Message message = Message.obtain(mActivity.mHandler, R.id.decode_failed);
            message.arg1 = (int) (endDate.getTime() - startDate.getTime());
            message.sendToTarget();
        }
    }

}
