/*
 * Copyright (C) 2009 ZXing authors
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

package com.google.zxing.client.android.share;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Browser;
import android.view.View;
import android.widget.ListView;
import com.google.zxing.client.android.R;

import java.util.ArrayList;
import java.util.List;

public final class AppPickerActivity extends ListActivity {

  private final List<String[]> labelsPackages = new ArrayList<String[]>();
  private DialogInterface dialog;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    dialog = ProgressDialog.show(this, "", getString(R.string.msg_loading_apps), true, true);
    if (labelsPackages.isEmpty()) {
      new LoadPackagesAsyncTask(this).execute(labelsPackages);
    }
    // otherwise use last copy we loaded -- apps don't change much, and it takes
    // forever to load for some reason
  }

  @Override
  protected void onListItemClick(ListView l, View view, int position, long id) {
    if (position >= 0 && position < labelsPackages.size()) {
      String url = "market://search?q=pname:" + labelsPackages.get(position)[1];
      Intent intent = new Intent();
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);      
      intent.putExtra(Browser.BookmarkColumns.URL, url);
      setResult(RESULT_OK, intent);
    } else {
      setResult(RESULT_CANCELED);
    }
    finish();
  }

  DialogInterface getProgressDialog() {
    return dialog;
  }

}
