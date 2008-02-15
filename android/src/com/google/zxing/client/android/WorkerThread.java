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

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;

/**
 * This thread does all the heavy lifting, both during preview and for the final capture and
 * decoding. That leaves the main thread free to handle UI tasks.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class WorkerThread extends Thread {

  private final CameraSurfaceView surfaceView;
  private final CameraManager cameraManager;
  private final Handler handler;
  private final Object idleLock;
  private State state;

  private enum State {
    IDLE,
    PREVIEW_LOOP,
    STILL_AND_DECODE,
    DONE
  }

  WorkerThread(CameraSurfaceView surfaceView, CameraManager cameraManager, Handler handler) {
    this.surfaceView = surfaceView;
    this.cameraManager = cameraManager;
    this.handler = handler;
    this.idleLock = new Object();
    state = State.IDLE;
  }

  @Override
  public void run() {
    while (true) {
      switch (state) {
        case IDLE:
          idle();
          break;
        case PREVIEW_LOOP:
          surfaceView.capturePreviewAndDraw();
          break;
        case STILL_AND_DECODE:
          Bitmap bitmap = cameraManager.captureStill();
          Result rawResult;
          try {
            MonochromeBitmapSource source = new YUVMonochromeBitmapSource(bitmap);
            rawResult = new MultiFormatReader().decode(source);
          } catch (ReaderException e) {
            Message message = Message.obtain(handler, R.id.decoding_failed_message);
            message.sendToTarget();
            state = State.PREVIEW_LOOP;
            break;
          }
          Message message = Message.obtain(handler, R.id.decoding_succeeded_message, rawResult);
          message.sendToTarget();
          state = State.IDLE;
          break;
        case DONE:
          return;
      }
    }
  }

  public void requestPreviewLoop() {
    state = State.PREVIEW_LOOP;
    wakeFromIdle();
  }

  public void requestStillAndDecode() {
    state = State.STILL_AND_DECODE;
    wakeFromIdle();
  }

  public void requestExitAndWait() {
    state = State.DONE;
    wakeFromIdle();
    try {
      join();
    } catch (InterruptedException e) {
    }
  }

  private void idle() {
    try {
      synchronized (idleLock) {
        idleLock.wait();
      }
    } catch (InterruptedException ie) {
      // continue
    }
  }

  private void wakeFromIdle() {
    synchronized (idleLock) {
      idleLock.notifyAll();
    }
  }

}
