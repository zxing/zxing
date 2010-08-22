/*
 * Copyright (C) 2008 ZXing authors
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.provider.Contacts;
import android.provider.BaseColumns;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.R;

/**
 * Barcode Scanner can share data like contacts and bookmarks by displaying a QR Code on screen,
 * such that another user can scan the barcode with their phone.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ShareActivity extends Activity {

  private static final String TAG = ShareActivity.class.getSimpleName();

  private static final int PICK_BOOKMARK = 0;
  private static final int PICK_CONTACT = 1;
  private static final int PICK_APP = 2;

  //private static final int METHODS_ID_COLUMN = 0;
  private static final int METHODS_KIND_COLUMN = 1;
  private static final int METHODS_DATA_COLUMN = 2;

  private static final String[] METHODS_PROJECTION = {
      BaseColumns._ID, // 0
      Contacts.ContactMethodsColumns.KIND, // 1
      Contacts.ContactMethodsColumns.DATA, // 2
  };

  private static final int PHONES_NUMBER_COLUMN = 1;

  private static final String[] PHONES_PROJECTION = {
      BaseColumns._ID, // 0
      Contacts.PhonesColumns.NUMBER // 1
  };

  private Button clipboardButton;

  private final Button.OnClickListener contactListener = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK, Contacts.People.CONTENT_URI);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      startActivityForResult(intent, PICK_CONTACT);
    }
  };

  private final Button.OnClickListener bookmarkListener = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.setClassName(ShareActivity.this, BookmarkPickerActivity.class.getName());
      startActivityForResult(intent, PICK_BOOKMARK);
    }
  };

  private final Button.OnClickListener appListener = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.setClassName(ShareActivity.this, AppPickerActivity.class.getName());
      startActivityForResult(intent, PICK_APP);
    }
  };

  private final Button.OnClickListener clipboardListener = new Button.OnClickListener() {
    public void onClick(View v) {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      // Should always be true, because we grey out the clipboard button in onResume() if it's empty
      if (clipboard.hasText()) {
        Intent intent = new Intent(Intents.Encode.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(Intents.Encode.DATA, clipboard.getText().toString());
        intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
        startActivity(intent);
      }
    }
  };

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.share);

    findViewById(R.id.contact_button).setOnClickListener(contactListener);
    findViewById(R.id.bookmark_button).setOnClickListener(bookmarkListener);
    findViewById(R.id.app_button).setOnClickListener(appListener);
    clipboardButton = (Button) findViewById(R.id.clipboard_button);
    clipboardButton.setOnClickListener(clipboardListener);
  }

  @Override
  protected void onResume() {
    super.onResume();

    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    if (clipboard.hasText()) {
      clipboardButton.setEnabled(true);
      clipboardButton.setText(R.string.button_share_clipboard);
    } else {
      clipboardButton.setEnabled(false);
      clipboardButton.setText(R.string.button_clipboard_empty);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case PICK_BOOKMARK:
        case PICK_APP:
          showTextAsBarcode(intent.getStringExtra(Browser.BookmarkColumns.URL));
          break;
        case PICK_CONTACT:
          // Data field is content://contacts/people/984
          showContactAsBarcode(intent.getData());
          break;
      }
    }
  }

  private void showTextAsBarcode(String text) {
    Log.i(TAG, "Showing text as barcode: " + text);    
    if (text == null) {
      return; // Show error?
    }
    Intent intent = new Intent(Intents.Encode.ACTION);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
    intent.putExtra(Intents.Encode.DATA, text);
    intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
    startActivity(intent);
  }

  /**
   * Takes a contact Uri and does the necessary database lookups to retrieve that person's info,
   * then sends an Encode intent to render it as a QR Code.
   *
   * @param contactUri A Uri of the form content://contacts/people/17
   */
  private void showContactAsBarcode(Uri contactUri) {
    Log.i(TAG, "Showing contact URI as barcode: " + contactUri);
    if (contactUri == null) {
      return; // Show error?
    }
    ContentResolver resolver = getContentResolver();
    Cursor contactCursor = resolver.query(contactUri, null, null, null, null);
    Bundle bundle = new Bundle();
    if (contactCursor != null && contactCursor.moveToFirst()) {
      int nameColumn = contactCursor.getColumnIndex(Contacts.PeopleColumns.NAME);
      String name = contactCursor.getString(nameColumn);

      // Don't require a name to be present, this contact might be just a phone number.
      if (name != null && name.length() > 0) {
        bundle.putString(Contacts.Intents.Insert.NAME, massageContactData(name));
      }
      contactCursor.close();

      Uri phonesUri = Uri.withAppendedPath(contactUri, Contacts.People.Phones.CONTENT_DIRECTORY);
      Cursor phonesCursor = resolver.query(phonesUri, PHONES_PROJECTION, null, null, null);
      if (phonesCursor != null) {
        int foundPhone = 0;
        while (phonesCursor.moveToNext()) {
          String number = phonesCursor.getString(PHONES_NUMBER_COLUMN);
          if (foundPhone < Contents.PHONE_KEYS.length) {
            bundle.putString(Contents.PHONE_KEYS[foundPhone], massageContactData(number));
            foundPhone++;
          }
        }
        phonesCursor.close();
      }

      Uri methodsUri = Uri.withAppendedPath(contactUri,
          Contacts.People.ContactMethods.CONTENT_DIRECTORY);
      Cursor methodsCursor = resolver.query(methodsUri, METHODS_PROJECTION, null, null, null);
      if (methodsCursor != null) {
        int foundEmail = 0;
        boolean foundPostal = false;
        while (methodsCursor.moveToNext()) {
          int kind = methodsCursor.getInt(METHODS_KIND_COLUMN);
          String data = methodsCursor.getString(METHODS_DATA_COLUMN);
          switch (kind) {
            case Contacts.KIND_EMAIL:
              if (foundEmail < Contents.EMAIL_KEYS.length) {
                bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(data));
                foundEmail++;
              }
              break;
            case Contacts.KIND_POSTAL:
              if (!foundPostal) {
                bundle.putString(Contacts.Intents.Insert.POSTAL, massageContactData(data));
                foundPostal = true;
              }
              break;
          }
        }
        methodsCursor.close();
      }

      Intent intent = new Intent(Intents.Encode.ACTION);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);      
      intent.putExtra(Intents.Encode.TYPE, Contents.Type.CONTACT);
      intent.putExtra(Intents.Encode.DATA, bundle);
      intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());

      Log.i(TAG, "Sending bundle for encoding: " + bundle);
      startActivity(intent);
    }
  }

  private static String massageContactData(String data) {
    // For now -- make sure we don't put newlines in shared contact data. It messes up
    // any known encoding of contact data. Replace with space.
    if (data.indexOf('\n') >= 0) {
      data = data.replace("\n", " ");
    }
    if (data.indexOf('\r') >= 0) {
      data = data.replace("\r", " ");
    }
    return data;
  }
}
