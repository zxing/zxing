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
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * This class encodes data from an Intent into a QR code, and then displays it full screen so that
 * another person can scan it with their device.
 */
public class BarcodesEncodeActivity extends Activity {

    private QRCodeEncoder mQRCodeEncoder;
    private ProgressDialog mProgressDialog;
    private boolean mFirstLayout;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Intent intent = getIntent();
        if (intent == null || !intent.getAction().equals(Intents.Encode.ACTION)) {
            finish();
        } else {
            setContentView(R.layout.encode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LinearLayout layout = (LinearLayout) findViewById(R.id.encode_view);
        layout.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
        mFirstLayout = true;
    }

    /**
     * This needs to be delayed until after the first layout so that the view dimensions will be
     * available.
     */
    public OnGlobalLayoutListener mLayoutListener = new OnGlobalLayoutListener() {
        public void onGlobalLayout() {
            if (mFirstLayout) {
                LinearLayout layout = (LinearLayout) findViewById(R.id.encode_view);
                int width = layout.getWidth();
                int height = layout.getHeight();
                int smallerDimension = (width < height) ? width : height;

                Intent intent = getIntent();
                try {
                    mQRCodeEncoder = new QRCodeEncoder(BarcodesEncodeActivity.this, intent);
                    setTitle(getString(R.string.app_name) + " - " + mQRCodeEncoder.getTitle());
                    mQRCodeEncoder.requestBarcode(mHandler, smallerDimension);
                    mProgressDialog = ProgressDialog.show(BarcodesEncodeActivity.this, null,
                            getString(R.string.msg_encode_in_progress), true, true, mCancelListener);
                } catch (IllegalArgumentException e) {
                    showErrorMessage(R.string.msg_encode_contents_failed);
                }
                mFirstLayout = false;
            }
        }
    };

    public Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case R.id.encode_succeeded:
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                    Bitmap image = (Bitmap)message.obj;
                    ImageView view = (ImageView) findViewById(R.id.image_view);
                    view.setImageBitmap(image);
                    TextView contents = (TextView) findViewById(R.id.contents_text_view);
                    contents.setText(mQRCodeEncoder.getDisplayContents());
                    mQRCodeEncoder = null;
                    break;
                case R.id.encode_failed:
                    showErrorMessage(R.string.msg_encode_barcode_failed);
                    mQRCodeEncoder = null;
                    break;
            }
        }
    };

    private void showErrorMessage(int message) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.button_ok, mClickListener);
        builder.show();
    }

    private OnClickListener mClickListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            finish();
        }
    };

    private OnCancelListener mCancelListener = new OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
            finish();
        }
    };

}
