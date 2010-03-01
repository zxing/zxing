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
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;

import java.util.List;
import java.util.ArrayList;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.Result;

/**
 * <p>Manages functionality related to scan history.</p>
 * 
 * @author Sean Owen
 */
public final class HistoryManager {

  private static final int MAX_ITEMS = 50;
  private static final String[] TEXT_COL_PROJECTION = { DBHelper.TEXT_COL };
  private static final String[] TEXT_FORMAT_COL_PROJECTION = { DBHelper.TEXT_COL, DBHelper.FORMAT_COL };
  private static final String[] ID_COL_PROJECTION = { DBHelper.ID_COL };

  private final CaptureActivity activity;

  public HistoryManager(CaptureActivity activity) {
    this.activity = activity;
  }

  List<Result> getHistoryItems() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    List<Result> items = new ArrayList<Result>();
    SQLiteDatabase db = helper.getReadableDatabase();
    Cursor cursor = null;
    try {
      cursor = db.query(DBHelper.TABLE_NAME,
                        TEXT_FORMAT_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      while (cursor.moveToNext()) {
        Result result = new Result(cursor.getString(0), null, null, BarcodeFormat.valueOf(cursor.getString(1)));
        items.add(result);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.close();
    }
    return items;
  }

  public AlertDialog buildAlert() {
    final List<Result> items = getHistoryItems();
    final String[] dialogItems = new String[items.size() + 2];
    for (int i = 0; i < items.size(); i++) {
      dialogItems[i] = items.get(i).getText();
    }
    final Resources res = activity.getResources();
    dialogItems[dialogItems.length - 2] = res.getString(R.string.history_send);
    dialogItems[dialogItems.length - 1] = res.getString(R.string.history_clear_text);
    DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialogInterface, int i) {
        if (i == dialogItems.length - 1) {
          clearHistory();
        } else if (i == dialogItems.length - 2) {
          String history = buildHistory();
          Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);          
          intent.putExtra(Intent.EXTRA_SUBJECT, res.getString(R.string.history_email_title));
          intent.putExtra(Intent.EXTRA_TEXT, history);
          intent.setType("text/plain");
          activity.startActivity(intent);
        } else {
          Result result = items.get(i);
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

  public void addHistoryItem(Result result) {

    if (!activity.getIntent().getBooleanExtra(Intents.Scan.SAVE_HISTORY, true)) {
      return; // Do not save this item to the history.
    }

    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getWritableDatabase();
    Cursor cursor = null;
    try {
      cursor = db.query(DBHelper.TABLE_NAME,
                        TEXT_COL_PROJECTION,
                        DBHelper.TEXT_COL + "=?",
                        new String[] { result.getText() },
                        null, null, null, null);
      if (cursor.moveToNext()) {
        return;
      }
      ContentValues values = new ContentValues();
      values.put(DBHelper.TEXT_COL, result.getText());
      values.put(DBHelper.FORMAT_COL, result.getBarcodeFormat().toString());
      values.put(DBHelper.DISPLAY_COL, result.getText()); // TODO use parsed result display value?
      values.put(DBHelper.TIMESTAMP_COL, System.currentTimeMillis());
      db.insert(DBHelper.TABLE_NAME, DBHelper.TIMESTAMP_COL, values);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.close();
    }
  }

  public void trimHistory() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getWritableDatabase();
    Cursor cursor = null;
    try {
      cursor = db.query(DBHelper.TABLE_NAME,
                        ID_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      int count = 0;
      while (count < MAX_ITEMS && cursor.moveToNext()) {
        count++;
      }
      while (cursor.moveToNext()) {
        db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getString(0), null);
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.close();
    }
  }

  private String buildHistory() {
    StringBuilder historyText = new StringBuilder();
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getReadableDatabase();
    Cursor cursor = null;
    try {
      cursor = db.query(DBHelper.TABLE_NAME,
                        TEXT_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      while (cursor.moveToNext()) {
        historyText.append(cursor.getString(0)).append('\n');
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.close();
    }
    return historyText.toString();
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
