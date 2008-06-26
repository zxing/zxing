/*
 * Copyright 2008 ZXing authors
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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;

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
  private String lastResult;

  private static final int ABOUT_ID = Menu.FIRST;
  private static final int HELP_ID = Menu.FIRST + 1;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.main);

    cameraManager = new CameraManager(getApplication());
    surfaceView = new CameraSurfaceView(getApplication(), cameraManager);
    surfaceView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
        ViewGroup.LayoutParams.FILL_PARENT));

    ViewGroup previewView = (ViewGroup) findViewById(R.id.preview_view);
    previewView.addView(surfaceView);
    cameraThread = null;

    // TODO re-enable this when issues with Matrix.setPolyToPoly() are resolved
    //GridSampler.setGridSampler(new AndroidGraphicsGridSampler());
  }

  @Override
  protected void onResume() {
    super.onResume();
    resetStatusView();
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
    switch (keyCode) {
      case KeyEvent.KEYCODE_A:
        cameraThread.setDecodeAllMode();
        break;
      case KeyEvent.KEYCODE_C:
        Message save = Message.obtain(cameraThread.handler, R.id.save);
        save.sendToTarget();
        break;
      case KeyEvent.KEYCODE_P:
        cameraManager.setUsePreviewForDecode(true);
        break;
      case KeyEvent.KEYCODE_Q:
        cameraThread.setDecodeQRMode();
        break;
      case KeyEvent.KEYCODE_S:
        cameraManager.setUsePreviewForDecode(false);
        break;
      case KeyEvent.KEYCODE_T:
        cameraThread.toggleTracing();
        break;
      case KeyEvent.KEYCODE_U:
        cameraThread.setDecode1DMode();
        break;
      default:
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
        showAlert(context.getString(R.string.title_about), 0,
          context.getString(R.string.msg_about),
          context.getString(R.string.button_ok),
          true);
        break;
      case HELP_ID:
        showAlert(context.getString(R.string.title_help), 0,
            context.getString(R.string.msg_help),
            context.getString(R.string.button_ok), true);
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
        case R.id.restart_preview:
          restartPreview();
          break;
      }
    }
  };

  void restartPreview() {
    resetStatusViewColor();
    Message restart = Message.obtain(cameraThread.handler, R.id.restart_preview);
    restart.sendToTarget();
  }

  private void handleDecode(Result rawResult, int duration) {
    if (!rawResult.toString().equals(lastResult)) {
      lastResult = rawResult.toString();
      playBeepSound();

      ResultPoint[] points = rawResult.getResultPoints();
      if (points != null && points.length > 0) {
        surfaceView.drawResultPoints(points);
      }

      TextView textView = (TextView) findViewById(R.id.status_text_view);
      ParsedResult result = parseReaderResult(rawResult);
      textView.setText(result.getDisplayResult() + " (" + duration + " ms)");

      TextView actionButton = (TextView) findViewById(R.id.status_action_button);
      int buttonText = getActionButtonText(result.getType());
      if (buttonText != 0) {
        actionButton.setVisibility(View.VISIBLE);
        actionButton.setText(buttonText);
        View.OnClickListener handler = new ResultHandler(this, result);
        actionButton.setOnClickListener(handler);
        actionButton.requestFocus();
      } else {
        actionButton.setVisibility(View.GONE);
      }

      View statusView = findViewById(R.id.status_view);
      statusView.setBackgroundColor(0xc000ff00);

      // Show the green finder patterns for one second, then restart the preview
      Message message = Message.obtain(messageHandler, R.id.restart_preview);
      messageHandler.sendMessageDelayed(message, 1000);
    } else {
      restartPreview();
    }
  }

  private void playBeepSound() {
    MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.beep);
    mediaPlayer.start();
  }

  private void resetStatusView() {
    resetStatusViewColor();
    TextView textView = (TextView) findViewById(R.id.status_text_view);
    textView.setText(R.string.msg_default_status);
    View actionButton = findViewById(R.id.status_action_button);
    actionButton.setVisibility(View.GONE);
    lastResult = "";
  }

  private void resetStatusViewColor() {
    View statusView = findViewById(R.id.status_view);
    statusView.setBackgroundColor(0x50000000);
  }

  private static ParsedResult parseReaderResult(Result rawResult) {
    ParsedResult result = ResultParser.parseReaderResult(rawResult);
    if (result.getType().equals(ParsedResultType.TEXT)) {
      String rawText = rawResult.getText();
      AndroidIntentParsedResult androidResult = AndroidIntentParsedResult.parse(rawText);
      if (androidResult != null) {
        Intent intent = androidResult.getIntent();
        if (!Intent.VIEW_ACTION.equals(intent.getAction())) {
          // For now, don't take anything that just parses as a View action. A lot
          // of things are accepted as a View action by default.
          result = androidResult;
        }
      }
    }
    return result;
  }

  private static int getActionButtonText(ParsedResultType type) {
    int buttonText;
    if (type.equals(ParsedResultType.ADDRESSBOOK)) {
      buttonText = R.string.button_add_contact;
    } else if (type.equals(ParsedResultType.URI)) {
      buttonText = R.string.button_open_browser;
    } else if (type.equals(ParsedResultType.EMAIL_ADDRESS)) {
      buttonText = R.string.button_email;
    } else if (type.equals(ParsedResultType.UPC)) {
      buttonText = R.string.button_lookup_product;
    } else if (type.equals(ParsedResultType.TEL)) {
      buttonText = R.string.button_dial;
    } else if (type.equals(ParsedResultType.GEO)) {
      buttonText = R.string.button_show_map;
    } else {
      buttonText = 0;
    }
    return buttonText;
  }

}