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

package com.google.zxing.client.android.encode;

import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * This class encodes data from an Intent into a QR code, and then displays it full screen so that
 * another person can scan it with their device.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class EncodeActivity extends Activity {
  private QRCodeEncoder qrCodeEncoder;
  private ProgressDialog progressDialog;
  private boolean firstLayout;

  /**
   * This needs to be delayed until after the first layout so that the view dimensions will be
   * available.
   */
  private final OnGlobalLayoutListener layoutListener = new OnGlobalLayoutListener() {
    public void onGlobalLayout() {
      if (firstLayout) {
        View layout = findViewById(R.id.encode_view);
        int width = layout.getWidth();
        int height = layout.getHeight();
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 7 / 8;

        Intent intent = getIntent();
        try {
          qrCodeEncoder = new QRCodeEncoder(EncodeActivity.this, intent);
          setTitle(getString(R.string.app_name) + " - " + qrCodeEncoder.getTitle());
          qrCodeEncoder.requestBarcode(handler, smallerDimension);
          progressDialog = ProgressDialog.show(EncodeActivity.this, null,
              getString(R.string.msg_encode_in_progress), true, true, cancelListener);
        } catch (IllegalArgumentException e) {
          showErrorMessage(R.string.msg_encode_contents_failed);
        }
        firstLayout = false;
      }
    }
  };

  private final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.encode_succeeded:
          progressDialog.dismiss();
          progressDialog = null;
          Bitmap image = (Bitmap) message.obj;
          ImageView view = (ImageView) findViewById(R.id.image_view);
          view.setImageBitmap(image);
          TextView contents = (TextView) findViewById(R.id.contents_text_view);
          contents.setText(qrCodeEncoder.getDisplayContents());
          qrCodeEncoder = null;
          break;
        case R.id.encode_failed:
          showErrorMessage(R.string.msg_encode_barcode_failed);
          qrCodeEncoder = null;
          break;
      }
    }
  };

  private final OnClickListener clickListener = new OnClickListener() {
    public void onClick(DialogInterface dialog, int which) {
      finish();
    }
  };

  private final OnCancelListener cancelListener = new OnCancelListener() {
    public void onCancel(DialogInterface dialog) {
      finish();
    }
  };

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Intent intent = getIntent();
    if (intent != null) {
      String action = intent.getAction();
      if (action.equals(Intents.Encode.ACTION) || action.equals(Intent.ACTION_SEND)) {
        setContentView(R.layout.encode);
        return;
      }
    }
    finish();
  }

  @Override
  protected void onResume() {
    super.onResume();

    View layout = findViewById(R.id.encode_view);
    layout.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    firstLayout = true;
  }

  private void showErrorMessage(int message) {
    if (progressDialog != null) {
      progressDialog.dismiss();
      progressDialog = null;
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(message);
    builder.setPositiveButton(R.string.button_ok, clickListener);
    builder.show();
  }
}
