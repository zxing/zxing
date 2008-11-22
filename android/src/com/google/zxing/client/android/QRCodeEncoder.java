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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.util.Log;
import android.telephony.PhoneNumberUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;

public final class QRCodeEncoder {

  private static final String TAG = "QRCodeEncoder";

  private final Activity mActivity;
  private String mContents;
  private String mDisplayContents;
  private String mTitle;

  public QRCodeEncoder(Activity activity, Intent intent) {
    mActivity = activity;
    if (!encodeContents(intent)) {
      throw new IllegalArgumentException("No valid data to encode.");
    }
  }

  public void requestBarcode(Handler handler, int pixelResolution) {
    Thread encodeThread = new EncodeThread(mContents, handler, pixelResolution);
    encodeThread.start();
  }

  public String getContents() {
    return mContents;
  }

  public String getDisplayContents() {
    return mDisplayContents;
  }

  public String getTitle() {
    return mTitle;
  }

  // TODO: The string encoding should live in the core ZXing library.
  private boolean encodeContents(Intent intent) {
    if (intent == null) return false;
    String type = intent.getStringExtra(Intents.Encode.TYPE);
    if (type == null || type.length() == 0) return false;

    if (type.equals(Contents.Type.TEXT)) {
      String string = intent.getStringExtra(Intents.Encode.DATA);
      if (string != null && string.length() > 0) {
        mContents = string;
        mDisplayContents = string;
        mTitle = mActivity.getString(R.string.contents_text);
      }
    } else if (type.equals(Contents.Type.EMAIL)) {
      String string = intent.getStringExtra(Intents.Encode.DATA);
      if (string != null && string.length() > 0) {
        mContents = "mailto:" + string;
        mDisplayContents = string;
        mTitle = mActivity.getString(R.string.contents_email);
      }
    } else if (type.equals(Contents.Type.PHONE)) {
      String string = intent.getStringExtra(Intents.Encode.DATA);
      if (string != null && string.length() > 0) {
        mContents = "tel:" + string;
        mDisplayContents = PhoneNumberUtils.formatNumber(string);
        mTitle = mActivity.getString(R.string.contents_phone);
      }
    } else if (type.equals(Contents.Type.SMS)) {
      String string = intent.getStringExtra(Intents.Encode.DATA);
      if (string != null && string.length() > 0) {
        mContents = "sms:" + string;
        mDisplayContents = PhoneNumberUtils.formatNumber(string);
        mTitle = mActivity.getString(R.string.contents_sms);
      }
    } else if (type.equals(Contents.Type.CONTACT)) {
      Bundle bundle = intent.getBundleExtra(Intents.Encode.DATA);
      if (bundle != null) {
        String name = bundle.getString(Contacts.Intents.Insert.NAME);
        if (name != null && name.length() > 0) {
          mContents = "MECARD:N:" + name + ";";
          mDisplayContents = name;
          String address = bundle.getString(Contacts.Intents.Insert.POSTAL);
          if (address != null && address.length() > 0) {
            mContents += "ADR:" + address + ";";
            mDisplayContents += "\n" + address;
          }
          for (int x = 0; x < Contents.PHONE_KEYS.length; x++) {
            String phone = bundle.getString(Contents.PHONE_KEYS[x]);
            if (phone != null && phone.length() > 0) {
              mContents += "TEL:" + phone + ";";
              mDisplayContents += "\n" + PhoneNumberUtils.formatNumber(phone);
            }
          }
          for (int x = 0; x < Contents.EMAIL_KEYS.length; x++) {
            String email = bundle.getString(Contents.EMAIL_KEYS[x]);
            if (email != null && email.length() > 0) {
              mContents += "EMAIL:" + email + ";";
              mDisplayContents += "\n" + email;
            }
          }
          mContents += ";";
          mTitle = mActivity.getString(R.string.contents_contact);
        }
      }
    } else if (type.equals(Contents.Type.LOCATION)) {
      Bundle bundle = intent.getBundleExtra(Intents.Encode.DATA);
      if (bundle != null) {
        float latitude = bundle.getFloat("LAT", Float.MAX_VALUE);
        float longitude = bundle.getFloat("LONG", Float.MAX_VALUE);
        if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
          mContents = "geo:" + latitude + "," + longitude;
          mDisplayContents = latitude + "," + longitude;
          mTitle = mActivity.getString(R.string.contents_location);
        }
      }
    }
    return mContents != null && mContents.length() > 0;
  }

  private final static class EncodeThread extends Thread {

    private final String mContents;
    private final Handler mHandler;
    private final int mPixelResolution;

    public EncodeThread(String contents, Handler handler, int pixelResolution) {
      mContents = contents;
      mHandler = handler;
      mPixelResolution = pixelResolution;
    }

    public final void run() {
      try {
        ByteMatrix result = new MultiFormatWriter().encode(mContents, BarcodeFormat.QR_CODE,
             mPixelResolution, mPixelResolution);
        int width = result.width();
        int height = result.height();
        byte[][] array = result.getArray();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            int grey = array[y][x] & 0xff;
            pixels[y * width + x] = (0xff << 24) | (grey << 16) | (grey << 8) | grey;
          }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        Message message = Message.obtain(mHandler, R.id.encode_succeeded);
        message.obj = bitmap;
        message.sendToTarget();
      } catch (WriterException e) {
        Log.e(TAG, e.toString());
        Message message = Message.obtain(mHandler, R.id.encode_failed);
        message.sendToTarget();
      } catch (IllegalArgumentException e) {
        Log.e(TAG, e.toString());
        Message message = Message.obtain(mHandler, R.id.encode_failed);
        message.sendToTarget();
      }
    }
  }

}
