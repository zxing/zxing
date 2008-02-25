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
  private WorkerThread workerThread;

  private static final int ABOUT_ID = Menu.FIRST;

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
    workerThread = new WorkerThread(surfaceView, cameraManager, messageHandler);
    workerThread.requestPreviewLoop();
    workerThread.start();

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
    if (workerThread == null) {
      workerThread = new WorkerThread(surfaceView, cameraManager, messageHandler);
      workerThread.requestPreviewLoop();
      workerThread.start();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (workerThread != null) {
      workerThread.requestExitAndWait();
      workerThread = null;
    }
    cameraManager.closeDriver();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
      workerThread.requestStillAndDecode();
      return true;
    } else {
      return super.onKeyDown(keyCode, event);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, ABOUT_ID, R.string.menu_about);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(Menu.Item item) {
    switch (item.getId()) {
      case ABOUT_ID:
        Context context = getApplication();
        showAlert(context.getString(R.string.title_about),
            context.getString(R.string.msg_about),
            context.getString(R.string.button_ok), null, true, null);
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private final Handler messageHandler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.decoding_succeeded_message:
          handleDecode((Result) message.obj);
          break;
        case R.id.decoding_failed_message:
          Context context = getApplication();
          showAlert(context.getString(R.string.title_no_barcode_detected),
              context.getString(R.string.msg_no_barcode_detected),
              context.getString(R.string.button_ok), null, true, null);
          break;
      }
    }
  };

  public void restartPreview() {
    workerThread.requestPreviewLoop();
  }

  // TODO(dswitkin): These deprecated showAlert calls need to be updated.
  private void handleDecode(Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      surfaceView.drawResultPoints(points);
    }

    Context context = getApplication();
    ParsedReaderResult readerResult = ParsedReaderResult.parseReaderResult(rawResult.getText());
    Handler handler = new ResultHandler(this, readerResult);
    if (canBeHandled(readerResult.getType())) {
      // Can be handled by some external app; ask if the user wants to
      // proceed first though
      Message yesMessage = handler.obtainMessage(R.string.button_yes);
      Message noMessage = handler.obtainMessage(R.string.button_no);
      showAlert(context.getString(getDialogTitleID(readerResult.getType())),
          readerResult.getDisplayResult(), context.getString(R.string.button_yes),
          yesMessage, context.getString(R.string.button_no), noMessage, true, noMessage);
    } else {
      // Just show information to user
      Message okMessage = handler.obtainMessage(R.string.button_ok);
      showAlert(context.getString(R.string.title_barcode_detected),
          readerResult.getDisplayResult(), context.getString(R.string.button_ok), okMessage, null,
          null, true, okMessage);
    }
  }

  private static boolean canBeHandled(ParsedReaderResultType type) {
    return type != ParsedReaderResultType.TEXT;
  }

  private static int getDialogTitleID(ParsedReaderResultType type) {
    if (type == ParsedReaderResultType.ADDRESSBOOK) {
      return R.string.title_add_contact;
    } else if (type == ParsedReaderResultType.URI ||
               type == ParsedReaderResultType.BOOKMARK ||
               type == ParsedReaderResultType.URLTO) {
      return R.string.title_open_url;
    } else if (type == ParsedReaderResultType.EMAIL ||
               type == ParsedReaderResultType.EMAIL_ADDRESS) {
      return R.string.title_compose_email;
    } else if (type == ParsedReaderResultType.UPC) {
      return R.string.title_lookup_barcode;
    } else {
      return R.string.title_barcode_detected;
    }
  }

}