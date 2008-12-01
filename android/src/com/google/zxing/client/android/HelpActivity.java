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
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class HelpActivity extends Activity {

  private static final String DEFAULT_URL = "file:///android_asset/html/index.html";

  private WebView mWebView;
  private Button mBackButton;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.help);

    mWebView = (WebView)findViewById(R.id.help_contents);
    mWebView.setWebViewClient(new HelpClient());
    if (icicle != null) {
      mWebView.restoreState(icicle);
    } else {
      mWebView.loadUrl(DEFAULT_URL);
    }

    mBackButton = (Button)findViewById(R.id.back_button);
    mBackButton.setOnClickListener(mBackListener);

    Button doneButton = (Button)findViewById(R.id.done_button);
    doneButton.setOnClickListener(mDoneListener);
  }

  @Override
  public void onResume() {
    super.onResume();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    mWebView.saveState(state);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (mWebView.canGoBack()) {
        mWebView.goBack();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  private final Button.OnClickListener mBackListener = new Button.OnClickListener() {
    public void onClick(View view) {
      mWebView.goBack();
    }
  };

  private final Button.OnClickListener mDoneListener = new Button.OnClickListener() {
    public void onClick(View view) {
      finish();
    }
  };

  private final class HelpClient extends WebViewClient {

    @Override
    public void onPageFinished(WebView view, String url) {
      setTitle(view.getTitle());
      mBackButton.setEnabled(view.canGoBack());
    }

  }

}
