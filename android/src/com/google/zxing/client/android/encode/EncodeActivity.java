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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.FinishListener;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class encodes data from an Intent into a QR code, and then displays it full screen so that
 * another person can scan it with their device.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class EncodeActivity extends Activity {

  private static final String TAG = EncodeActivity.class.getSimpleName();

  private static final int SHARE_BARCODE_DIMENSION = 300;
  private static final int MAX_BARCODE_FILENAME_LENGTH = 24;

  private QRCodeEncoder qrCodeEncoder;
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
          Bitmap image = (Bitmap) message.obj;
          ImageView view = (ImageView) findViewById(R.id.image_view);
          view.setImageBitmap(image);
          TextView contents = (TextView) findViewById(R.id.contents_text_view);
          contents.setText(qrCodeEncoder.getDisplayContents());
          //qrCodeEncoder = null;
          break;
        case R.id.encode_failed:
          showErrorMessage(R.string.msg_encode_barcode_failed);
          qrCodeEncoder = null;
          break;
      }
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
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, Menu.FIRST, 0, R.string.menu_share).setIcon(android.R.drawable.ic_menu_share);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (qrCodeEncoder == null) { // Odd
      Log.w(TAG, "No existing barcode to send?");
      return true;
    }

    String contents = qrCodeEncoder.getContents();
    Bitmap bitmap;
    try {
      bitmap = QRCodeEncoder.encodeAsBitmap(contents, BarcodeFormat.QR_CODE,
          SHARE_BARCODE_DIMENSION, SHARE_BARCODE_DIMENSION);
    } catch (WriterException we) {
      Log.w(TAG, we);
      return true;
    }

    File bsRoot = new File(Environment.getExternalStorageDirectory(), "BarcodeScanner");
    File barcodesRoot = new File(bsRoot, "Barcodes");
    if (!barcodesRoot.exists() && !barcodesRoot.mkdirs()) {
      Log.w(TAG, "Couldn't make dir " + barcodesRoot);
      showErrorMessage(R.string.msg_unmount_usb);
      return true;
    }
    File barcodeFile = new File(barcodesRoot, makeBarcodeFileName(contents) + ".png");
    barcodeFile.delete();
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(barcodeFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
    } catch (FileNotFoundException fnfe) {
      Log.w(TAG, "Couldn't access file " + barcodeFile + " due to " + fnfe);
      showErrorMessage(R.string.msg_unmount_usb);
      return true;
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ioe) {
          // do nothing
        }
      }
    }

    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " +
        qrCodeEncoder.getTitle());
    intent.putExtra(Intent.EXTRA_TEXT, qrCodeEncoder.getContents());
    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + barcodeFile.getAbsolutePath()));
    intent.setType("image/png");
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    startActivity(Intent.createChooser(intent, null));
    return true;
  }

  private static CharSequence makeBarcodeFileName(CharSequence contents) {
    int fileNameLength = Math.min(MAX_BARCODE_FILENAME_LENGTH, contents.length());
    StringBuilder fileName = new StringBuilder(fileNameLength);
    for (int i = 0; i < fileNameLength; i++) {
      char c = contents.charAt(i);
      if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
        fileName.append(c);
      } else {
        fileName.append('_');
      }
    }
    return fileName;
  }

  @Override
  protected void onResume() {
    super.onResume();
    View layout = findViewById(R.id.encode_view);
    layout.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    firstLayout = true;
  }

  private void showErrorMessage(int message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(message);
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }
}
