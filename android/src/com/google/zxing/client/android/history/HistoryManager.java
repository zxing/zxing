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

import android.database.sqlite.SQLiteException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.result.ResultHandler;

import android.app.Activity;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>Manages functionality related to scan history.</p>
 *
 * @author Sean Owen
 */
public final class HistoryManager {

  private static final String TAG = HistoryManager.class.getSimpleName();

  private static final int MAX_ITEMS = 500;

  private static final String[] COLUMNS = {
      DBHelper.TEXT_COL,
      DBHelper.DISPLAY_COL,
      DBHelper.FORMAT_COL,
      DBHelper.TIMESTAMP_COL,
      DBHelper.DETAILS_COL,
  };

  private static final String[] COUNT_COLUMN = { "COUNT(1)" };

  private static final String[] ID_COL_PROJECTION = { DBHelper.ID_COL };
  private static final String[] ID_DETAIL_COL_PROJECTION = { DBHelper.ID_COL, DBHelper.DETAILS_COL };
  private static final DateFormat EXPORT_DATE_TIME_FORMAT =
      DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

  private final Activity activity;

  public HistoryManager(Activity activity) {
    this.activity = activity;
  }

  public boolean hasHistoryItems() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = helper.getReadableDatabase();
      cursor = db.query(DBHelper.TABLE_NAME, COUNT_COLUMN, null, null, null, null, null);
      cursor.moveToFirst();
      return cursor.getInt(0) > 0;
    } finally {
      close(cursor, db);
    }
  }

  public List<HistoryItem> buildHistoryItems() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    List<HistoryItem> items = new ArrayList<HistoryItem>();
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = helper.getReadableDatabase();
      cursor = db.query(DBHelper.TABLE_NAME, COLUMNS, null, null, null, null, DBHelper.TIMESTAMP_COL + " DESC");
      while (cursor.moveToNext()) {
        String text = cursor.getString(0);
        String display = cursor.getString(1);
        String format = cursor.getString(2);
        long timestamp = cursor.getLong(3);
        String details = cursor.getString(4);
        Result result = new Result(text, null, null, BarcodeFormat.valueOf(format), timestamp);
        items.add(new HistoryItem(result, display, details));
      }
    } finally {
      close(cursor, db);
    }
    return items;
  }

  public HistoryItem buildHistoryItem(int number) {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = helper.getReadableDatabase();
      cursor = db.query(DBHelper.TABLE_NAME, COLUMNS, null, null, null, null, DBHelper.TIMESTAMP_COL + " DESC");
      cursor.move(number + 1);
      String text = cursor.getString(0);
      String display = cursor.getString(1);
      String format = cursor.getString(2);
      long timestamp = cursor.getLong(3);
      String details = cursor.getString(4);
      Result result = new Result(text, null, null, BarcodeFormat.valueOf(format), timestamp);
      return new HistoryItem(result, display, details);
    } finally {
      close(cursor, db);
    }
  }
  
  public void deleteHistoryItem(int number) {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = helper.getWritableDatabase();      
      cursor = db.query(DBHelper.TABLE_NAME,
                        ID_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      cursor.move(number + 1);
      db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getString(0), null);
    } finally {
      close(cursor, db);
    }
  }

  public void addHistoryItem(Result result, ResultHandler handler) {
    // Do not save this item to the history if the preference is turned off, or the contents are
    // considered secure.
    if (!activity.getIntent().getBooleanExtra(Intents.Scan.SAVE_HISTORY, true) ||
        handler.areContentsSecure()) {
      return;
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    if (!prefs.getBoolean(PreferencesActivity.KEY_REMEMBER_DUPLICATES, false)) {
      deletePrevious(result.getText());
    }

    ContentValues values = new ContentValues();
    values.put(DBHelper.TEXT_COL, result.getText());
    values.put(DBHelper.FORMAT_COL, result.getBarcodeFormat().toString());
    values.put(DBHelper.DISPLAY_COL, handler.getDisplayContents().toString());
    values.put(DBHelper.TIMESTAMP_COL, System.currentTimeMillis());

    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    try {
      db = helper.getWritableDatabase();      
      // Insert the new entry into the DB.
      db.insert(DBHelper.TABLE_NAME, DBHelper.TIMESTAMP_COL, values);
    } finally {
      close(null, db);
    }
  }

  public void addHistoryItemDetails(String itemID, String itemDetails) {
    // As we're going to do an update only we don't need need to worry
    // about the preferences; if the item wasn't saved it won't be udpated
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;    
    Cursor cursor = null;
    try {
      db = helper.getWritableDatabase();
      cursor = db.query(DBHelper.TABLE_NAME,
                        ID_DETAIL_COL_PROJECTION,
                        DBHelper.TEXT_COL + "=?",
                        new String[] { itemID },
                        null,
                        null,
                        DBHelper.TIMESTAMP_COL + " DESC",
                        "1");
      String oldID = null;
      String oldDetails = null;
      if (cursor.moveToNext()) {
        oldID = cursor.getString(0);
        oldDetails = cursor.getString(1);
      }

      if (oldID != null) {
        String newDetails = oldDetails == null ? itemDetails : oldDetails + " : " + itemDetails;
        ContentValues values = new ContentValues();
        values.put(DBHelper.DETAILS_COL, newDetails);
        db.update(DBHelper.TABLE_NAME, values, DBHelper.ID_COL + "=?", new String[] { oldID });
      }

    } finally {
      close(cursor, db);
    }
  }

  private void deletePrevious(String text) {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    try {
      db = helper.getWritableDatabase();      
      db.delete(DBHelper.TABLE_NAME, DBHelper.TEXT_COL + "=?", new String[] { text });
    } finally {
      close(null, db);
    }
  }

  public void trimHistory() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = helper.getWritableDatabase();      
      cursor = db.query(DBHelper.TABLE_NAME,
                        ID_COL_PROJECTION,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");
      cursor.move(MAX_ITEMS);
      while (cursor.moveToNext()) {
        db.delete(DBHelper.TABLE_NAME, DBHelper.ID_COL + '=' + cursor.getString(0), null);
      }
    } catch (SQLiteException sqle) {
      // We're seeing an error here when called in CaptureActivity.onCreate() in rare cases
      // and don't understand it. First theory is that it's transient so can be safely ignored.
      // TODO revisit this after live in a future version to see if it 'worked'
      Log.w(TAG, sqle);
      // continue
    } finally {
      close(cursor, db);
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
  CharSequence buildHistory() {
    StringBuilder historyText = new StringBuilder(1000);
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    Cursor cursor = null;
    try {
      db = helper.getWritableDatabase();
      cursor = db.query(DBHelper.TABLE_NAME,
                        COLUMNS,
                        null, null, null, null,
                        DBHelper.TIMESTAMP_COL + " DESC");

      while (cursor.moveToNext()) {

        historyText.append('"').append(massageHistoryField(cursor.getString(0))).append("\",");
        historyText.append('"').append(massageHistoryField(cursor.getString(1))).append("\",");
        historyText.append('"').append(massageHistoryField(cursor.getString(2))).append("\",");
        historyText.append('"').append(massageHistoryField(cursor.getString(3))).append("\",");

        // Add timestamp again, formatted
        long timestamp = cursor.getLong(3);
        historyText.append('"').append(massageHistoryField(
            EXPORT_DATE_TIME_FORMAT.format(new Date(timestamp)))).append("\",");

        // Above we're preserving the old ordering of columns which had formatted data in position 5

        historyText.append('"').append(massageHistoryField(cursor.getString(4))).append("\"\r\n");
      }
      return historyText;
    } finally {
      close(cursor, db);
    }
  }
  
  void clearHistory() {
    SQLiteOpenHelper helper = new DBHelper(activity);
    SQLiteDatabase db = null;
    try {
      db = helper.getWritableDatabase();      
      db.delete(DBHelper.TABLE_NAME, null, null);
    } finally {
      close(null, db);
    }
  }

  static Uri saveHistory(String history) {
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
    return value == null ? "" : value.replace("\"","\"\"");
  }
  
  private static void close(Cursor cursor, SQLiteDatabase database) {
    if (cursor != null) {
      cursor.close();
    }
    if (database != null) {
      database.close();
    }
  }

}
