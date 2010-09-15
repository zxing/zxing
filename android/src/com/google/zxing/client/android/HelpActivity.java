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

  private static final String TAG = HelpActivity.class.getSimpleName();

  // Actually guessing at the Desire's MODEL for now:
  private static final String[] BUGGY_MODEL_SUBSTRINGS = {
      "Desire",
      "Pulse", // Camera doesn't come on
      "Geeksphone", // Doesn't support YUV?
      "supersonic", // aka Evo
  };
  private static final Uri BUGGY_URI =
      Uri.parse("http://code.google.com/p/zxing/wiki/FrequentlyAskedQuestions");

  // Use this key and one of the values below when launching this activity via intent. If not
  // present, the default page will be loaded.
  public static final String REQUESTED_PAGE_KEY = "requested_page_key";
  public static final String DEFAULT_PAGE = "index.html";
  public static final String WHATS_NEW_PAGE = "whatsnew.html";

  private static final String BASE_URL = "file:///android_asset/html/";
  private static final String WEBVIEW_STATE_PRESENT = "webview_state_present";

  private static boolean initialized = false;
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

  private final DialogInterface.OnClickListener groupsListener =
      new DialogInterface.OnClickListener() {
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

    // Froyo has a bug with calling onCreate() twice in a row, which causes the What's New page
    // that's auto-loaded on first run to appear blank. As a workaround we only call restoreState()
    // if a valid URL was loaded at the time the previous activity was torn down.
    Intent intent = getIntent();
    if (icicle != null && icicle.getBoolean(WEBVIEW_STATE_PRESENT, false)) {
      webView.restoreState(icicle);
    } else if (intent != null) {
      String page = intent.getStringExtra(REQUESTED_PAGE_KEY);
      if (page != null && page.length() > 0) {
        webView.loadUrl(BASE_URL + page);
      } else {
        webView.loadUrl(BASE_URL + DEFAULT_PAGE);
      }
    } else {
      webView.loadUrl(BASE_URL + DEFAULT_PAGE);
    }

    backButton = (Button) findViewById(R.id.back_button);
    backButton.setOnClickListener(backListener);
    View doneButton = findViewById(R.id.done_button);
    doneButton.setOnClickListener(doneListener);

    if (!initialized) {
      initialized = true;
      checkBuggyDevice();
    }
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
          builder.show();
          break;
        }
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    String url = webView.getUrl();
    if (url != null && url.length() > 0) {
      webView.saveState(state);
      state.putBoolean(WEBVIEW_STATE_PRESENT, true);
    }
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

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
      if (url.startsWith("file")) {
        // Keep local assets in this WebView.
        return false;
      } else {
        // Open external URLs in Browser.
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        return true;
      }
    }
  }
}
