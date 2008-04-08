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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * This thread continuously pulls preview frames from the camera and draws them to the screen. It
 * also asks the DecodeThread to process as many images as it can keep up with, and coordinates with
 * the main thread to display the results.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class CameraThread extends Thread {

  public Handler handler;

  private final CameraSurfaceView surfaceView;
  private final Handler activityHandler;
  private final DecodeThread decodeThread;
  private boolean requestDecode;
  private boolean suspendPreview;

  CameraThread(BarcodeReaderCaptureActivity activity, CameraSurfaceView surfaceView,
               CameraManager cameraManager, Handler activityHandler) {
    this.surfaceView = surfaceView;
    this.activityHandler = activityHandler;

    decodeThread = new DecodeThread(activity, cameraManager);
    decodeThread.start();
    requestDecode = true;
    suspendPreview = false;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new Handler() {
      public void handleMessage(Message message) {
        switch (message.what) {
          case R.id.preview:
            if (!suspendPreview) {
              surfaceView.capturePreviewAndDraw();
            }
            break;
          case R.id.save:
            suspendPreview = true;
            Message save = Message.obtain(decodeThread.handler, R.id.save);
            save.sendToTarget();
            break;
          case R.id.restart_preview:
            restartPreviewAndDecode();
            return;
          case R.id.quit:
            Message quit = Message.obtain(decodeThread.handler, R.id.quit);
            quit.sendToTarget();
            Looper.myLooper().quit();
            break;
          case R.id.decode_started:
            // Since the decoder is done with the camera, continue fetching preview frames.
            suspendPreview = false;
            break;
          case R.id.decode_succeeded:
            // Message.copyFrom() did not work as expected, hence this workaround.
            Message success = Message.obtain(activityHandler, R.id.decode_succeeded, message.obj);
            success.arg1 = message.arg1;
            success.sendToTarget();
            suspendPreview = true;
            break;
          case R.id.decode_failed:
            // We're decoding as fast as possible, so when one fails, start another.
            requestDecode = true;
            break;
          case R.id.save_succeeded:
            // TODO: Put up a non-blocking status message
            restartPreviewAndDecode();
            break;
          case R.id.save_failed:
            // TODO: Put up a blocking error message
            restartPreviewAndDecode();
            return;
        }

        if (requestDecode) {
          requestDecode = false;
          suspendPreview = true;
          Message decode = Message.obtain(decodeThread.handler, R.id.decode);
          decode.sendToTarget();
        } else if (!suspendPreview) {
          Message preview = Message.obtain(handler, R.id.preview);
          preview.sendToTarget();
        }
      }
    };
    decodeThread.setCameraThreadHandler(handler);

    // Start ourselves capturing previews
    Message preview = Message.obtain(handler, R.id.preview);
    preview.sendToTarget();
    Looper.loop();
  }

  public void setDecodeAllMode() {
    Message message = Message.obtain(decodeThread.handler, R.id.set_decode_all_mode);
    message.sendToTarget();
  }

  public void setDecode1DMode() {
    Message message = Message.obtain(decodeThread.handler, R.id.set_decode_1D_mode);
    message.sendToTarget();
  }

  public void setDecodeQRMode() {
    Message message = Message.obtain(decodeThread.handler, R.id.set_decode_QR_mode);
    message.sendToTarget();
  }

  /**
   * Take one preview to update the screen, then do a decode and continue previews.
   */
  private void restartPreviewAndDecode() {
    requestDecode = true;
    suspendPreview = false;
    Message preview = Message.obtain(handler, R.id.preview);
    preview.sendToTarget();
  }

}
