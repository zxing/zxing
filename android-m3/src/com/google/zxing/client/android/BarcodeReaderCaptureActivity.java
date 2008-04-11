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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ParsedReaderResult;
import com.google.zxing.client.result.ParsedReaderResultType;

/**
 * The barcode reader activity itself. This is loosely based on the CameraPreview
 * example included in the Android SDK.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Android Team (for CameraPreview example)
 */
public final class BarcodeReaderCaptureActivity extends Activity {

  private CameraManager cameraManager;
  private CameraSurfaceView surfaceView;
  private CameraThread cameraThread;

  private static final int ABOUT_ID = Menu.FIRST;
  private static final int HELP_ID = Menu.FIRST + 1;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Make sure to create a TRANSLUCENT window. This is required for SurfaceView to work.
    // Eventually this'll be done by the system automatically.
    getWindow().setAttributes(new LayoutParams(LayoutParams.APPLICATION_TYPE,
        LayoutParams.NO_STATUS_BAR_FLAG));
    getWindow().setFormat(PixelFormat.TRANSLUCENT);

    cameraManager = new CameraManager(getApplication());
    surfaceView = new CameraSurfaceView(getApplication(), cameraManager);
    setContentView(surfaceView);
    cameraThread = new CameraThread(this, surfaceView, cameraManager, messageHandler);
    cameraThread.start();

    // TODO re-enable this when issues with Matrix.setPolyToPoly() are resolved
    //GridSampler.setGridSampler(new AndroidGraphicsGridSampler());
  }

  @Override
  protected boolean isFullscreenOpaque() {
    // Our main window is set to translucent, but we know that we will
    // fill it with opaque data. Tell the system that so it can perform
    // some important optimizations.
    return true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    cameraManager.openDriver();
    if (cameraThread == null) {
      cameraThread = new CameraThread(this, surfaceView, cameraManager, messageHandler);
      cameraThread.start();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (cameraThread != null) {
      cameraThread.quitSynchronously();
      cameraThread = null;
    }
    cameraManager.closeDriver();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_A) {
      cameraThread.setDecodeAllMode();
    } else if (keyCode == KeyEvent.KEYCODE_C) {
      Message save = Message.obtain(cameraThread.handler, R.id.save);
      save.sendToTarget();
    } else if (keyCode == KeyEvent.KEYCODE_P) {
      cameraManager.setUsePreviewForDecode(true);
    } else if (keyCode == KeyEvent.KEYCODE_Q) {
      cameraThread.setDecodeQRMode();
    } else if (keyCode == KeyEvent.KEYCODE_S) {
      cameraManager.setUsePreviewForDecode(false);
    } else if (keyCode == KeyEvent.KEYCODE_U) {
      cameraThread.setDecode1DMode();
    } else {
      return super.onKeyDown(keyCode, event);
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, ABOUT_ID, R.string.menu_about);
    menu.add(0, HELP_ID, R.string.menu_help);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(Menu.Item item) {
    Context context = getApplication();
    switch (item.getId()) {
      case ABOUT_ID:
        showAlert(context.getString(R.string.title_about),
            context.getString(R.string.msg_about),
            context.getString(R.string.button_ok), null, true, null);
        break;
      case HELP_ID:
        showAlert(context.getString(R.string.title_help),
            context.getString(R.string.msg_help),
            context.getString(R.string.button_ok), null, true, null);
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private final Handler messageHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.decode_succeeded:
          int duration = message.arg1;
          handleDecode((Result) message.obj, duration);
          break;
      }
    }
  };

  public void restartPreview() {
    Message restart = Message.obtain(cameraThread.handler, R.id.restart_preview);
    restart.sendToTarget();
  }

  // TODO(dswitkin): These deprecated showAlert calls need to be updated.
  private void handleDecode(Result rawResult, int duration) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      surfaceView.drawResultPoints(points);
    }

    Context context = getApplication();
    ParsedReaderResult readerResult = parseReaderResult(rawResult);
    ResultHandler handler = new ResultHandler(this, readerResult);
    if (handler.getIntent() != null) {
      // Can be handled by some external app; ask if the user wants to
      // proceed first though
      Message yesMessage = handler.obtainMessage(R.string.button_yes);
      Message noMessage = handler.obtainMessage(R.string.button_no);
      String title = context.getString(getDialogTitleID(readerResult.getType())) +
          " (" + duration + " ms)";
      showAlert(title, readerResult.getDisplayResult(), context.getString(R.string.button_yes),
          yesMessage, context.getString(R.string.button_no), noMessage, true, noMessage);
    } else {
      // Just show information to user
      Message okMessage = handler.obtainMessage(R.string.button_ok);
      String title = context.getString(R.string.title_barcode_detected) +
          " (" + duration + " ms)";
      showAlert(title, readerResult.getDisplayResult(), context.getString(R.string.button_ok),
          okMessage, null, null, true, okMessage);
    }
  }

  private static ParsedReaderResult parseReaderResult(Result rawResult) {
    ParsedReaderResult readerResult = ParsedReaderResult.parseReaderResult(rawResult);
    if (readerResult.getType().equals(ParsedReaderResultType.TEXT)) {
      String rawText = rawResult.getText();
      AndroidIntentParsedResult androidResult = AndroidIntentParsedResult.parse(rawText);
      if (androidResult != null) {
        Intent intent = androidResult.getIntent();
        if (!Intent.VIEW_ACTION.equals(intent.getAction())) {
          // For now, don't take anything that just parses as a View action. A lot
          // of things are accepted as a View action by default.
          readerResult = androidResult;          
        }
      }
    }
    return readerResult;
  }

  private static int getDialogTitleID(ParsedReaderResultType type) {
    if (type.equals(ParsedReaderResultType.ADDRESSBOOK)) {
      return R.string.title_add_contact;
    } else if (type.equals(ParsedReaderResultType.URI) ||
               type.equals(ParsedReaderResultType.BOOKMARK) ||
               type.equals(ParsedReaderResultType.URLTO)) {
      return R.string.title_open_url;
    } else if (type.equals(ParsedReaderResultType.EMAIL) ||
               type.equals(ParsedReaderResultType.EMAIL_ADDRESS)) {
      return R.string.title_compose_email;
    } else if (type.equals(ParsedReaderResultType.UPC)) {
      return R.string.title_lookup_barcode;
    } else if (type.equals(ParsedReaderResultType.TEL)) {
      return R.string.title_dial;
    } else if (type.equals(ParsedReaderResultType.GEO)) {
      return R.string.title_view_maps;
    } else {
      return R.string.title_barcode_detected;
    }
  }

}