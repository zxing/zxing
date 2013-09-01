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

import android.provider.ContactsContract;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.Browser;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.zxing.client.android.clipboard.ClipboardInterface;

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

  private View clipboardButton;

  private final Button.OnClickListener contactListener = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      startActivityForResult(intent, PICK_CONTACT);
    }
  };

  private final Button.OnClickListener bookmarkListener = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.setClassName(ShareActivity.this, BookmarkPickerActivity.class.getName());
      startActivityForResult(intent, PICK_BOOKMARK);
    }
  };

  private final Button.OnClickListener appListener = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_PICK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.setClassName(ShareActivity.this, AppPickerActivity.class.getName());
      startActivityForResult(intent, PICK_APP);
    }
  };

  private final Button.OnClickListener clipboardListener = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      // Should always be true, because we grey out the clipboard button in onResume() if it's empty
      CharSequence text = ClipboardInterface.getText(ShareActivity.this);
      if (text != null) {
        launchSearch(text.toString());
      }
    }
  };

  private final View.OnKeyListener textListener = new View.OnKeyListener() {
    @Override
    public boolean onKey(View view, int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
        String text = ((TextView) view).getText().toString();
        if (text != null && !text.isEmpty()) {
          launchSearch(text);
        }
        return true;
      }
      return false;
    }
  };

  private void launchSearch(String text) {
    Intent intent = new Intent(Intents.Encode.ACTION);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
    intent.putExtra(Intents.Encode.DATA, text);
    intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
    startActivity(intent);
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.share);

    findViewById(R.id.share_contact_button).setOnClickListener(contactListener);
    findViewById(R.id.share_bookmark_button).setOnClickListener(bookmarkListener);
    findViewById(R.id.share_app_button).setOnClickListener(appListener);
    clipboardButton = findViewById(R.id.share_clipboard_button);
    clipboardButton.setOnClickListener(clipboardListener);
    findViewById(R.id.share_text_view).setOnKeyListener(textListener);
  }

  @Override
  protected void onResume() {
    super.onResume();
    clipboardButton.setEnabled(ClipboardInterface.hasText(this));
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

    Cursor cursor;
    try {
      // We're seeing about six reports a week of this exception although I don't understand why.
      cursor = resolver.query(contactUri, null, null, null, null);
    } catch (IllegalArgumentException ignored) {
      return;
    }
    if (cursor == null) {
      return;
    }

    String id;
    String name;
    boolean hasPhone;
    try {
      if (!cursor.moveToFirst()) {
        return;
      }

      id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
      name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
      hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;


    } finally {
      cursor.close();
    }

    // Don't require a name to be present, this contact might be just a phone number.
    Bundle bundle = new Bundle();
    if (name != null && !name.isEmpty()) {
      bundle.putString(ContactsContract.Intents.Insert.NAME, massageContactData(name));
    }

    if (hasPhone) {
      Cursor phonesCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                           null,
                                           ContactsContract.CommonDataKinds.Phone.CONTACT_ID + '=' + id,
                                           null,
                                           null);
      if (phonesCursor != null) {
        try {
          int foundPhone = 0;
          int phonesNumberColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
          while (phonesCursor.moveToNext() && foundPhone < Contents.PHONE_KEYS.length) {
            String number = phonesCursor.getString(phonesNumberColumn);
            if (number != null && !number.isEmpty()) {
              bundle.putString(Contents.PHONE_KEYS[foundPhone], massageContactData(number));
            }
            foundPhone++;
          }
        } finally {
          phonesCursor.close();
        }
      }
    }

    Cursor methodsCursor = resolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                                          null,
                                          ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + '=' + id,
                                          null,
                                          null);
    if (methodsCursor != null) {
      try {
        if (methodsCursor.moveToNext()) {
          String data = methodsCursor.getString(
              methodsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
          if (data != null && !data.isEmpty()) {
            bundle.putString(ContactsContract.Intents.Insert.POSTAL, massageContactData(data));
          }
        }
      } finally {
        methodsCursor.close();
      }
    }

    Cursor emailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + '=' + id,
                                        null,
                                        null);
    if (emailCursor != null) {
      try {
        int foundEmail = 0;
        int emailColumn = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
        while (emailCursor.moveToNext() && foundEmail < Contents.EMAIL_KEYS.length) {
          String email = emailCursor.getString(emailColumn);
          if (email != null && !email.isEmpty()) {
            bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(email));
          }
          foundEmail++;
        }
      } finally {
        emailCursor.close();
      }
    }

    Intent intent = new Intent(Intents.Encode.ACTION);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    intent.putExtra(Intents.Encode.TYPE, Contents.Type.CONTACT);
    intent.putExtra(Intents.Encode.DATA, bundle);
    intent.putExtra(Intents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());

    Log.i(TAG, "Sending bundle for encoding: " + bundle);
    startActivity(intent);
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
