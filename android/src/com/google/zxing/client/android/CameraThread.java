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
  private State state;

  private enum State {
    PREVIEW,
    DECODE,
    SAVE,
    DONE
  }

  CameraThread(BarcodeReaderCaptureActivity activity, CameraSurfaceView surfaceView,
               CameraManager cameraManager, Handler activityHandler) {
    this.surfaceView = surfaceView;
    this.activityHandler = activityHandler;

    decodeThread = new DecodeThread(activity, cameraManager);
    decodeThread.start();
    state = State.DONE;
  }

  @Override
  public void run() {
    Looper.prepare();
    handler = new Handler() {
      public void handleMessage(Message message) {
        switch (message.what) {
          case R.id.preview:
            if (state == State.PREVIEW) {
              surfaceView.capturePreviewAndDraw();
            }
            break;
          case R.id.save:
            state = State.SAVE;
            Message save = Message.obtain(decodeThread.handler, R.id.save);
            save.sendToTarget();
            break;
          case R.id.restart_preview:
            restartPreviewAndDecode();
            break;
          case R.id.quit:
            state = State.DONE;
            Message quit = Message.obtain(decodeThread.handler, R.id.quit);
            quit.sendToTarget();
            try {
              decodeThread.join();
            } catch (InterruptedException e) {
            }
            Looper.myLooper().quit();
            break;
          case R.id.decode_started:
            // Since the decoder is done with the camera, continue fetching preview frames.
            state = State.PREVIEW;
            break;
          case R.id.decode_succeeded:
            state = State.DONE;
            // Message.copyFrom() did not work as expected, hence this workaround.
            Message success = Message.obtain(activityHandler, R.id.decode_succeeded, message.obj);
            success.arg1 = message.arg1;
            success.sendToTarget();
            break;
          case R.id.decode_failed:
            // We're decoding as fast as possible, so when one fails, start another.
            startDecode();
            break;
          case R.id.save_succeeded:
            // TODO: Put up a non-blocking status message
            restartPreviewAndDecode();
            break;
          case R.id.save_failed:
            // TODO: Put up a blocking error message
            restartPreviewAndDecode();
            break;
        }

        if (state == State.PREVIEW) {
          Message preview = Message.obtain(handler, R.id.preview);
          preview.sendToTarget();
        }
      }
    };
    decodeThread.setCameraThreadHandler(handler);

    // Start ourselves capturing previews
    restartPreviewAndDecode();
    Looper.loop();
  }

  public void quitSynchronously() {
    Message quit = Message.obtain(handler, R.id.quit);
    quit.sendToTarget();
    try {
      join();
    } catch (InterruptedException e) {
    }
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

  public void toggleTracing() {
    Message message = Message.obtain(decodeThread.handler, R.id.toggle_tracing);
    message.sendToTarget();
  }

  /**
   * Start a decode if possible, but not now if the DecodeThread is in the middle of saving.
   */
  private void startDecode() {
    if (state != State.SAVE) {
      state = State.DECODE;
      Message decode = Message.obtain(decodeThread.handler, R.id.decode);
      decode.sendToTarget();
    }
  }

  /**
   * Take one preview to update the screen, then do a decode and continue previews.
   */
  private void restartPreviewAndDecode() {
    state = State.PREVIEW;
    surfaceView.capturePreviewAndDraw();
    startDecode();
  }

}
