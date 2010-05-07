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
import android.os.Environment;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import android.util.Log;
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

  private static final String TAG = HistoryManager.class.getSimpleName();

  private static final int MAX_ITEMS = 50;
  private static final String[] TEXT_COL_PROJECTION = { DBHelper.TEXT_COL };
  private static final String[] GET_ITEM_COL_PROJECTION = {
      DBHelper.TEXT_COL,
      DBHelper.FORMAT_COL,
      DBHelper.TIMESTAMP_COL,
  };
  private static final String[] EXPORT_COL_PROJECTION = {
      DBHelper.TEXT_COL,
      DBHelper.DISPLAY_COL,
      DBHelper.FORMAT_COL,
      DBHelper.TIMESTAMP_COL,
  };
  private static final String[] ID_COL_PROJECTION = { DBHelper.ID_COL };
  private static final DateFormat EXPORT_DATE_TIME_FORMAT = DateFormat.getDateTimeInstance();

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
                        GET_ITEM_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      while (cursor.moveToNext()) {
        Result result = new Result(cursor.getString(0),
                                   null,
                                   null,
                                   BarcodeFormat.valueOf(cursor.getString(1)),
                                   cursor.getLong(2));
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
    List<Result> items = getHistoryItems();
    int size = items.size();
    String[] dialogItems = new String[size + 2];
    for (int i = 0; i < size; i++) {
      dialogItems[i] = items.get(i).getText();
    }
    Resources res = activity.getResources();
    dialogItems[dialogItems.length - 2] = res.getString(R.string.history_send);
    dialogItems[dialogItems.length - 1] = res.getString(R.string.history_clear_text);
    DialogInterface.OnClickListener clickListener = new HistoryClickListener(dialogItems, items);
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

  /**
   * <p>Builds a text representation of the scanning history. Each scan is encoded on one
   * line, terminated by a line break (\r\n). The values in each line are comma-separated,
   * and double-quoted. Double-quotes within values are escaped with a sequence of two
   * double-quotes. The fields output are:</p>
   *
   * <ul>
   *  <li>Raw text</li>
   *  <li>Display text</li>
   *  <li>Format (e.g. QR_CODE)</li>
   *  <li>Timestamp</li>
   *  <li>Formatted version of timestamp</li>
   * </ul>
   */
  private CharSequence buildHistory() {
    StringBuilder historyText = new StringBuilder(1000);
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = helper.getReadableDatabase();
    Cursor cursor = null;
    try {
      cursor = db.query(DBHelper.TABLE_NAME,
                        EXPORT_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      while (cursor.moveToNext()) {
        for (int col = 0; col < EXPORT_COL_PROJECTION.length; col++) {
          historyText.append('"').append(massageHistoryField(cursor.getString(col))).append("\",");
        }
        // Add timestamp again, formatted
        long timestamp = cursor.getLong(EXPORT_COL_PROJECTION.length - 1);
        historyText.append('"').append(massageHistoryField(
            EXPORT_DATE_TIME_FORMAT.format(new Date(timestamp)))).append("\"\r\n");
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      db.close();
    }
    return historyText;
  }

  private Uri saveHistory(String history) {
    File bsRoot = new File(Environment.getExternalStorageDirectory(), "BarcodeScanner");
    File historyRoot = new File(bsRoot, "History");
    if (!historyRoot.exists() && !historyRoot.mkdirs()) {
      Log.w(TAG, "Couldn't make dir " + historyRoot);
      return null;
    }
    File historyFile = new File(historyRoot, "history-" + System.currentTimeMillis() + ".csv");
    OutputStreamWriter out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(historyFile), Charset.forName("UTF-8"));
      out.write(history);
      return Uri.parse("file://" + historyFile.getAbsolutePath());
    } catch (IOException ioe) {
      Log.w(TAG, "Couldn't access file " + historyFile + " due to " + ioe);
      return null;
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ioe) {
          // do nothing
        }
      }
    }
  }

  private static String massageHistoryField(String value) {
    return value.replace("\"","\"\"");
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

  private class HistoryClickListener implements DialogInterface.OnClickListener {

    private final String[] dialogItems;
    private final List<Result> items;

    private HistoryClickListener(String[] dialogItems, List<Result> items) {
      this.dialogItems = dialogItems;
      this.items = items;
    }

    public void onClick(DialogInterface dialogInterface, int i) {
      if (i == dialogItems.length - 1) {
        clearHistory();
      } else if (i == dialogItems.length - 2) {
        CharSequence history = buildHistory();
        Uri historyFile = saveHistory(history.toString());
        if (historyFile == null) {
          AlertDialog.Builder builder = new AlertDialog.Builder(activity);
          builder.setMessage(R.string.msg_unmount_usb);
          builder.setPositiveButton(R.string.button_ok, null);
          builder.show();
          return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        String subject = activity.getResources().getString(R.string.history_email_title);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, subject);
        intent.putExtra(Intent.EXTRA_STREAM, historyFile);
        intent.setType("text/csv");
        activity.startActivity(intent);
      } else {
        Result result = items.get(i);
        Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, result);
        message.sendToTarget();
      }
    }
  }
}
