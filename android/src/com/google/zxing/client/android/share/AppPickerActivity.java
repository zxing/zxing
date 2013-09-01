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
import android.os.AsyncTask;
import android.provider.Browser;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;

public final class AppPickerActivity extends ListActivity {

  private AsyncTask<?,?,?> backgroundTask;

  @Override
  protected void onResume() {
    super.onResume();
    backgroundTask = new LoadPackagesAsyncTask(this);
    backgroundTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  protected void onPause() {
    AsyncTask<?,?,?> task = backgroundTask;
    if (task != null) {
      task.cancel(true);
      backgroundTask = null;
    }
    super.onPause();
  }

  @Override
  protected void onListItemClick(ListView l, View view, int position, long id) {
    Adapter adapter = getListAdapter();
    if (position >= 0 && position < adapter.getCount()) {
      String packageName = ((AppInfo) adapter.getItem(position)).getPackageName();
      Intent intent = new Intent();
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(Browser.BookmarkColumns.URL, "market://details?id=" + packageName);
      setResult(RESULT_OK, intent);
    } else {
      setResult(RESULT_CANCELED);      
    }
    finish();
  }

}
