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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * An HTML-based help screen with Back and Done buttons at the bottom.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class HelpActivity extends Activity {

  private static final String TAG = HelpActivity.class.getName();

  private static final String[] BUGGY_MODEL_SUBSTRINGS = {"Behold II", "Pulse"};
  private static final Uri BUGGY_URI = Uri.parse("http://code.google.com/p/zxing/wiki/FrequentlyAskedQuestions");
  private static final String DEFAULT_URL = "file:///android_asset/html/index.html";

  private WebView webView;
  private Button backButton;

  private final Button.OnClickListener backListener = new Button.OnClickListener() {
    public void onClick(View view) {
      webView.goBack();
    }
  };

  private final Button.OnClickListener doneListener = new Button.OnClickListener() {
    public void onClick(View view) {
      finish();
    }
  };

  private final DialogInterface.OnClickListener groupsListener = new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialogInterface, int i) {
      Intent intent = new Intent(Intent.ACTION_VIEW, BUGGY_URI);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);              
      HelpActivity.this.startActivity(intent);
    }
  };

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.help);

    webView = (WebView)findViewById(R.id.help_contents);
    webView.setWebViewClient(new HelpClient());
    if (icicle != null) {
      webView.restoreState(icicle);
    } else {
      webView.loadUrl(DEFAULT_URL);
    }

    backButton = (Button)findViewById(R.id.back_button);
    backButton.setOnClickListener(backListener);

    Button doneButton = (Button)findViewById(R.id.done_button);
    doneButton.setOnClickListener(doneListener);
  }

  @Override
  public void onResume() {
    super.onResume();
    checkBuggyDevice();
  }

  private void checkBuggyDevice() {
    String model = Build.MODEL;
    Log.i(TAG, "Build model is " + model);
    if (model != null) {
      for (String buggyModelSubstring : BUGGY_MODEL_SUBSTRINGS) {
        if (model.contains(buggyModelSubstring)) {
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setMessage(R.string.msg_buggy);
          builder.setPositiveButton(R.string.button_ok, groupsListener);
          builder.setNegativeButton(R.string.button_cancel, null);
          builder.create().show();
          break;
        }
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    webView.saveState(state);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (webView.canGoBack()) {
        webView.goBack();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  private final class HelpClient extends WebViewClient {
    @Override
    public void onPageFinished(WebView view, String url) {
      setTitle(view.getTitle());
      backButton.setEnabled(view.canGoBack());
    }
  }

}
