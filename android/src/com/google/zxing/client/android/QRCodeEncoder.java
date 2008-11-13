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
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.net.URI;

public final class QRCodeEncoder {

  private static final String TAG = "QRCodeEncoder";

  // Since this is an API call rather than a website, we don't use LocaleManager to change the TLD.
  private static final String CHART_SERVER_URL = "//chart.apis.google.com/chart?cht=qr&chs=";

  private final Activity mActivity;
  private String mContents;
  private String mDisplayContents;
  private String mTitle;
  private String mUserAgent;

  public QRCodeEncoder(Activity activity, Intent intent) {
    mActivity = activity;
    if (!encodeContents(intent)) {
      throw new IllegalArgumentException("No valid data to encode.");
    }
    mUserAgent = mActivity.getString(R.string.zxing_user_agent);
  }

  // Once the core ZXing library supports encoding, we'll be able to generate the bitmap
  // synchronously. For now, it's a network request, so it's handled on a thread.
  public void requestBarcode(Handler handler, int pixelResolution) {
    Thread mNetworkThread = new NetworkThread(mContents, handler, pixelResolution);
    mNetworkThread.start();
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

  // Perhaps the string encoding should live in the core ZXing library too.
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
        mDisplayContents = string;
        mTitle = mActivity.getString(R.string.contents_phone);
      }
    } else if (type.equals(Contents.Type.SMS)) {
      String string = intent.getStringExtra(Intents.Encode.DATA);
      if (string != null && string.length() > 0) {
        mContents = "sms:" + string;
        mDisplayContents = string;
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
          String phone = bundle.getString(Contacts.Intents.Insert.PHONE);
          if (phone != null && phone.length() > 0) {
            mContents += "TEL:" + phone + ";";
            mDisplayContents += "\n" + phone;
          }
          String email = bundle.getString(Contacts.Intents.Insert.EMAIL);
          if (email != null && email.length() > 0) {
            mContents += "EMAIL:" + email + ";";
            mDisplayContents += "\n" + email;
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

  private final class NetworkThread extends Thread {

    private final String mContents;
    private final Handler mHandler;
    private final int mPixelResolution;

    public NetworkThread(String contents, Handler handler, int pixelResolution) {
      mContents = contents;
      mHandler = handler;
      mPixelResolution = pixelResolution;
    }

    public final void run() {
      AndroidHttpClient client = null;
      try {
        String url = CHART_SERVER_URL + mPixelResolution + "x" + mPixelResolution + "&chl=" +
          mContents;
        URI uri = new URI("http", url, null);
        HttpUriRequest get = new HttpGet(uri);
        client = AndroidHttpClient.newInstance(mUserAgent);
        HttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        Bitmap image = BitmapFactory.decodeStream(entity.getContent());
        if (image != null) {
          Message message = Message.obtain(mHandler, R.id.encode_succeeded);
          message.obj = image;
          message.sendToTarget();
        } else {
          Log.e(TAG, "Could not decode png from the network");
          Message message = Message.obtain(mHandler, R.id.encode_failed);
          message.sendToTarget();
        }
      } catch (Exception e) {
        Log.e(TAG, e.toString());
        Message message = Message.obtain(mHandler, R.id.encode_failed);
        message.sendToTarget();
      } finally {
        if (client != null) {
          client.close();
        }
      }
    }
  }

}
