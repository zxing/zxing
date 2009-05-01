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

package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.zxing.Result;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 */
public final class CaptureActivityHandler extends Handler {

  private final CaptureActivity mActivity;
  private final DecodeThread mDecodeThread;
  private State mState;

  private enum State {
    PREVIEW,
    SUCCESS,
    DONE
  }

  CaptureActivityHandler(CaptureActivity activity, String decodeMode,
                                 boolean beginScanning) {
    mActivity = activity;
    mDecodeThread = new DecodeThread(activity, decodeMode);
    mDecodeThread.start();
    mState = State.SUCCESS;

    // Start ourselves capturing previews and decoding.
    CameraManager.get().startPreview();
    if (beginScanning) {
      restartPreviewAndDecode();
    }
  }

  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case R.id.auto_focus:
        // When one auto focus pass finishes, start another. This is the closest thing to
        // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
        if (mState == State.PREVIEW) {
          CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
        }
        break;
      case R.id.restart_preview:
        restartPreviewAndDecode();
        break;
      case R.id.decode_succeeded:
        mState = State.SUCCESS;
        Bundle bundle = message.getData();
        Bitmap barcode = bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
        mActivity.handleDecode((Result) message.obj, barcode);
        break;
      case R.id.decode_failed:
        // We're decoding as fast as possible, so when one decode fails, start another.
        mState = State.PREVIEW;
        CameraManager.get().requestPreviewFrame(mDecodeThread.mHandler, R.id.decode);
        break;
      case R.id.return_scan_result:
        mActivity.setResult(Activity.RESULT_OK, (Intent) message.obj);
        mActivity.finish();
        break;
      case R.id.launch_product_query:
        String url = (String) message.obj;
        mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        break;
    }
  }

  public void quitSynchronously() {
    mState = State.DONE;
    CameraManager.get().stopPreview();
    Message quit = Message.obtain(mDecodeThread.mHandler, R.id.quit);
    quit.sendToTarget();
    try {
      mDecodeThread.join();
    } catch (InterruptedException e) {
    }

    // Be absolutely sure we don't send any queued up messages
    removeMessages(R.id.decode_succeeded);
    removeMessages(R.id.decode_failed);
  }

  private void restartPreviewAndDecode() {
    if (mState == State.SUCCESS) {
      mState = State.PREVIEW;
      CameraManager.get().requestPreviewFrame(mDecodeThread.mHandler, R.id.decode);
      CameraManager.get().requestAutoFocus(this, R.id.auto_focus);
      mActivity.drawViewfinder();
    }
  }

}
