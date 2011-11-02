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

package com.google.zxing.client.androidtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class CameraTestActivity extends Activity implements SurfaceHolder.Callback {

  private static final String TAG = CameraTestActivity.class.getSimpleName();

  public static final String GET_CAMERA_PARAMETERS = "GET_CAMERA_PARAMETERS";
  private static final String[] EMAIL_ADDRESS = {"zxing-external@google.com"};

  private SaveThread saveThread;
  private boolean getCameraParameters;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

    getCameraParameters = getIntent().getBooleanExtra(GET_CAMERA_PARAMETERS, false);
    if (getCameraParameters) {
      setContentView(R.layout.camera_parameters);
    } else {
      setContentView(R.layout.camera_test);
    }
    CameraManager.init(getApplication());

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (saveThread == null && !getCameraParameters) {
      saveThread = new SaveThread(this);
      saveThread.start();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    CameraManager.get().stopPreview();
    if (saveThread != null) {
      Message quit = Message.obtain(saveThread.handler, R.id.quit);
      quit.sendToTarget();
      try {
        saveThread.join();
      } catch (InterruptedException e) {
        // continue
      }
      saveThread = null;
    }
    CameraManager.get().closeDriver();
  }

  final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case R.id.auto_focus:
          // Do not continuously auto focus
          break;
        case R.id.save_succeeded:
          Toast.makeText(CameraTestActivity.this, R.string.save_succeeded, 500).show();
          break;
        case R.id.save_failed:
          Toast.makeText(CameraTestActivity.this, R.string.save_failed, 2000).show();
          break;
      }
    }
  };

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (!getCameraParameters) {
      if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        if (event.getRepeatCount() == 0) {
          CameraManager.get().requestAutoFocus(handler, R.id.auto_focus);
        }
        return true;
      } else if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_SEARCH) {
        if (event.getRepeatCount() == 0) {
          CameraManager.get().requestPreviewFrame(saveThread.handler, R.id.save);
        }
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    try {
      String parameters = CameraManager.get().openDriver(holder, getCameraParameters);
      CameraManager.get().startPreview();
      if (getCameraParameters) {
        collectStatsAndSendEmail(parameters);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  private void collectStatsAndSendEmail(String parameters) {
    StringBuilder result = new StringBuilder(1000);
    result.append("Device info:");
    result.append("\n  Board: ");
    result.append(Build.BOARD);
    result.append("\n  Brand: ");
    result.append(Build.BRAND);
    result.append("\n  Device: ");
    result.append(Build.DEVICE);
    result.append("\n  Display: ");
    result.append(Build.DISPLAY);
    result.append("\n  Fingerprint: ");
    result.append(Build.FINGERPRINT);
    result.append("\n  Host: ");
    result.append(Build.HOST);
    result.append("\n  ID: ");
    result.append(Build.ID);
    result.append("\n  Model: ");
    result.append(Build.MODEL);
    result.append("\n  Product: ");
    result.append(Build.PRODUCT);
    result.append("\n  Tags: ");
    result.append(Build.TAGS);
    result.append("\n  Type: ");
    result.append(Build.TYPE);
    result.append("\n  User: ");
    result.append(Build.USER);
    result.append("\n  Version Incremental: ");
    result.append(Build.VERSION.INCREMENTAL);
    result.append("\n  Version Release: ");
    result.append(Build.VERSION.RELEASE);
    result.append("\n  Version SDK: ");
    result.append(Build.VERSION.SDK);

    result.append("\n\n");
    result.append(parameters);

    FileOutputStream stream = null;
    try {
      stream = new FileOutputStream(new File("/sdcard/CameraParameters.txt"));
      stream.write(result.toString().getBytes());
    } catch (IOException e) {
      Log.e(TAG, "Cannot write parameters file ", e);
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
          Log.w(TAG, e);
        }
      }
    }

    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.putExtra(Intent.EXTRA_EMAIL, EMAIL_ADDRESS);
    intent.putExtra(Intent.EXTRA_SUBJECT, "Camera parameters report");
    intent.putExtra(Intent.EXTRA_TEXT, result.toString());
    intent.setType("text/plain");
    startActivity(intent);
    finish();
  }

}
