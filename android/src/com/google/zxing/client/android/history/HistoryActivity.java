/*
 * Copyright 2012 ZXing authors
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

package com.google.zxing.client.android.history;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

import java.util.List;

public final class HistoryActivity extends ListActivity {

  private static final int SEND_ID = Menu.FIRST;
  private static final int CLEAR_ID = Menu.FIRST + 1;

  private HistoryManager historyManager;
  private HistoryItemAdapter adapter;
  
  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    this.historyManager = new HistoryManager(this);  
    adapter = new HistoryItemAdapter(this);
    setListAdapter(adapter);
    ListView listview = getListView();
    registerForContextMenu(listview);
  }

  @Override
  protected void onResume() {
    super.onResume();
    List<HistoryItem> items = historyManager.buildHistoryItems();
    adapter.clear();
    for (HistoryItem item : items) {
      adapter.add(item);
    }
    if (adapter.isEmpty()) {
      adapter.add(new HistoryItem(null, null, null));
    }
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    if (adapter.getItem(position).getResult() != null) {
      Intent intent = new Intent(this, CaptureActivity.class);
      intent.putExtra(Intents.History.ITEM_NUMBER, position);
      setResult(Activity.RESULT_OK, intent);
      finish();
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu,
                                  View v,
                                  ContextMenu.ContextMenuInfo menuInfo) {
    int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
    menu.add(Menu.NONE, position, position, R.string.history_clear_one_history_text);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    int position = item.getItemId();
    historyManager.deleteHistoryItem(position);
    finish();
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (historyManager.hasHistoryItems()) {
      menu.add(0, SEND_ID, 0, R.string.history_send).setIcon(android.R.drawable.ic_menu_share);
      menu.add(0, CLEAR_ID, 0, R.string.history_clear_text).setIcon(android.R.drawable.ic_menu_delete);
      return true;
    }
    return false;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case SEND_ID:
        CharSequence history = historyManager.buildHistory();
        Uri historyFile = HistoryManager.saveHistory(history.toString());
        if (historyFile == null) {
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setMessage(R.string.msg_unmount_usb);
          builder.setPositiveButton(R.string.button_ok, null);
          builder.show();
        } else {
          Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
          String subject = getResources().getString(R.string.history_email_title);
          intent.putExtra(Intent.EXTRA_SUBJECT, subject);
          intent.putExtra(Intent.EXTRA_TEXT, subject);
          intent.putExtra(Intent.EXTRA_STREAM, historyFile);
          intent.setType("text/csv");
          startActivity(intent);
        }
        break;
      case CLEAR_ID:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.msg_sure);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int i2) {
            historyManager.clearHistory();
            dialog.dismiss();
            finish();
          }
        });
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.show();
        break;
      default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }

}
