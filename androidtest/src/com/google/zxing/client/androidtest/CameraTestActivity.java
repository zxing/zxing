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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public final class CameraTestActivity extends Activity implements SurfaceHolder.Callback {

  private SaveThread mSaveThread;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.camera_test);

    CameraManager.init(getApplication());

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    surfaceHolder.addCallback(this);
    surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mSaveThread == null) {
      mSaveThread = new SaveThread(this, CameraManager.get().getFramingRect());
      mSaveThread.start();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();

    CameraManager.get().stopPreview();
    if (mSaveThread != null) {
      Message quit = Message.obtain(mSaveThread.mHandler, R.id.quit);
      quit.sendToTarget();
      try {
        mSaveThread.join();
      } catch (InterruptedException e) {
      }
      mSaveThread = null;
    }
    CameraManager.get().closeDriver();
  }

  public final Handler mHandler = new Handler() {
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
    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
      if (event.getRepeatCount() == 0) {
        CameraManager.get().requestAutoFocus(mHandler, R.id.auto_focus);
      }
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
      if (event.getRepeatCount() == 0) {
        CameraManager.get().requestPreviewFrame(mSaveThread.mHandler, R.id.save);
      }
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    CameraManager.get().openDriver(holder);
    CameraManager.get().startPreview();
  }

  public void surfaceDestroyed(SurfaceHolder holder) {

  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

}
