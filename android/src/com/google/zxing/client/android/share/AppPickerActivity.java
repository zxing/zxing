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
import android.content.Intent;
import android.provider.Browser;
import android.view.View;
import android.widget.ListView;

import com.google.zxing.client.android.common.executor.AsyncTaskExecInterface;
import com.google.zxing.client.android.common.executor.AsyncTaskExecManager;

import java.util.ArrayList;
import java.util.List;

public final class AppPickerActivity extends ListActivity {

  private final List<String[]> labelsPackages;
  private LoadPackagesAsyncTask backgroundTask;
  private final AsyncTaskExecInterface taskExec;

  public AppPickerActivity() {
    labelsPackages = new ArrayList<String[]>();
    taskExec = new AsyncTaskExecManager().build();
  }

  @Override
  protected void onResume() {
    super.onResume();
    labelsPackages.clear();
    backgroundTask = new LoadPackagesAsyncTask(this);
    taskExec.execute(backgroundTask, labelsPackages);
  }

  @Override
  protected void onPause() {
    LoadPackagesAsyncTask task = backgroundTask;
    if (task != null) {
      task.cancel(true);
      backgroundTask = null;
    }
    super.onPause();
  }

  @Override
  protected void onListItemClick(ListView l, View view, int position, long id) {
    if (position >= 0 && position < labelsPackages.size()) {
      String url = "market://details?id=" + labelsPackages.get(position)[1];
      Intent intent = new Intent();
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(Browser.BookmarkColumns.URL, url);
      setResult(RESULT_OK, intent);
    } else {
      setResult(RESULT_CANCELED);
    }
    finish();
  }

}
