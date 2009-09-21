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

package com.google.zxing.client.android.history;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.os.Message;

import java.util.List;
import java.util.ArrayList;

import com.google.zxing.client.android.R;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.Result;

/**
 * @author Sean Owen
 */
public final class HistoryManager {

  private final CaptureActivity activity;

  public HistoryManager(CaptureActivity activity) {
    this.activity = activity;
  }

  List<String> getHistoryItems() {

    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getReadableDatabase();
    List<String> items = new ArrayList<String>();
    try {
      Cursor cursor = db.query(DBHelper.TABLE_NAME,
                               new String[] {DBHelper.TEXT_COL},
                               null, null, null, null,
                               DBHelper.TIMESTAMP_COL + " DESC");
      while (cursor.moveToNext()) {
        items.add(cursor.getString(0));
      }
    } finally {
      db.close();
    }
    return items;
  }

  public AlertDialog buildAlert() {
    List<String> items = getHistoryItems();
    final String[] dialogItems = new String[items.size() + 1];
    for (int i = 0; i < items.size(); i++) {
      dialogItems[i] = items.get(i);
    }
    dialogItems[dialogItems.length - 1] = activity.getResources().getString(R.string.history_clear_text);
    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialogInterface, int i) {
        if (i == dialogItems.length - 1) {
          clearHistory();
        } else {
          Result result = new Result(dialogItems[i], null, null, null);
          Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, result);
          message.sendToTarget();
        }
      }
    };
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setTitle(R.string.history_title);
    builder.setItems(dialogItems, clickListener);
    return builder.create();
  }

  public void addHistoryItem(String text) {

    if (getHistoryItems().contains(text)) {
      return;
    }

    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getWritableDatabase();
    try {
      ContentValues values = new ContentValues();
      values.put(DBHelper.TEXT_COL, text);
      values.put(DBHelper.TIMESTAMP_COL, System.currentTimeMillis());
      db.insert(DBHelper.TABLE_NAME, DBHelper.TIMESTAMP_COL, values);
    } finally {
      db.close();
    }
  }

  void clearHistory() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getWritableDatabase();
    try {
      db.delete(DBHelper.TABLE_NAME, null, null);
    } finally {
      db.close();
    }
  }

}
