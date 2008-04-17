/*
 * Copyright (C) 2008 Google Inc.
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

package com.google.zxing.client.android;

import android.content.Intent;
import android.net.ContentURI;
import android.provider.Contacts;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.google.zxing.client.result.*;

import java.net.URISyntaxException;

/**
 * Handles the result of barcode decoding in the context of the Android platform,
 * by dispatching the proper intents to open other activities like GMail, Maps, etc.
 *
 * @author srowen@google.com (Sean Owen)
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class ResultHandler implements Button.OnClickListener {

  private static final String TAG = "ResultHandler";

  private final Intent intent;
  private final BarcodeReaderCaptureActivity captureActivity;

  ResultHandler(BarcodeReaderCaptureActivity captureActivity, ParsedReaderResult result) {
    this.captureActivity = captureActivity;
    this.intent = resultToIntent(result);
  }

  private static Intent resultToIntent(ParsedReaderResult result) {
    Intent intent = null;
    ParsedReaderResultType type = result.getType();
    if (type.equals(ParsedReaderResultType.ADDRESSBOOK)) {
      AddressBookDoCoMoParsedResult addressResult = (AddressBookDoCoMoParsedResult) result;
      intent = new Intent(Contacts.Intents.Insert.ACTION, Contacts.People.CONTENT_URI);
      putExtra(intent, Contacts.Intents.Insert.NAME, addressResult.getName());
      putExtra(intent, Contacts.Intents.Insert.PHONE, addressResult.getPhoneNumbers());
      putExtra(intent, Contacts.Intents.Insert.EMAIL, addressResult.getEmail());
      putExtra(intent, Contacts.Intents.Insert.NOTES, addressResult.getNote());
      putExtra(intent, Contacts.Intents.Insert.POSTAL, addressResult.getAddress());
    } else if (type.equals(ParsedReaderResultType.ADDRESSBOOK_AU)) {
      AddressBookAUParsedResult addressResult = (AddressBookAUParsedResult) result;
      intent = new Intent(Contacts.Intents.Insert.ACTION, Contacts.People.CONTENT_URI);
      putExtra(intent, Contacts.Intents.Insert.NAME, addressResult.getNames());
      putExtra(intent, Contacts.Intents.Insert.PHONE, addressResult.getPhoneNumbers());
      putExtra(intent, Contacts.Intents.Insert.EMAIL, addressResult.getEmails());
      putExtra(intent, Contacts.Intents.Insert.NOTES, addressResult.getNote());
      putExtra(intent, Contacts.Intents.Insert.POSTAL, addressResult.getAddress());
    } else if (type.equals(ParsedReaderResultType.BOOKMARK)) {
      // For now, we can only open the browser, and not actually add a bookmark
      try {
        intent = new Intent(Intent.VIEW_ACTION, new ContentURI(((BookmarkDoCoMoParsedResult) result).getURI()));
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.URLTO)) {
      try {
        intent = new Intent(Intent.VIEW_ACTION, new ContentURI(((URLTOParsedResult) result).getURI()));
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.EMAIL)) {
      EmailDoCoMoParsedResult emailResult = (EmailDoCoMoParsedResult) result;
      try {
        intent = new Intent(Intent.SENDTO_ACTION, new ContentURI(emailResult.getTo()));
      } catch (URISyntaxException e) {
      }
      putExtra(intent, "subject", emailResult.getSubject());
      putExtra(intent, "body", emailResult.getBody());
    } else if (type.equals(ParsedReaderResultType.EMAIL_ADDRESS)) {
      EmailAddressParsedResult emailResult = (EmailAddressParsedResult) result;
      try {
        intent = new Intent(Intent.SENDTO_ACTION, new ContentURI("mailto:" + emailResult.getEmailAddress()));
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.TEL)) {
      TelParsedResult telResult = (TelParsedResult) result;
      try {
        intent = new Intent(Intent.DIAL_ACTION, new ContentURI("tel:" + telResult.getNumber()));
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.GEO)) {
      GeoParsedResult geoResult = (GeoParsedResult) result;
      try {
        ContentURI geoURI = new ContentURI(geoResult.getGeoURI());
        Log.v(TAG, "Created geo URI: " + geoURI.toString());
        intent = new Intent(Intent.VIEW_ACTION, geoURI);
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.UPC)) {
      UPCParsedResult upcResult = (UPCParsedResult) result;
      try {
        ContentURI uri = new ContentURI("http://www.upcdatabase.com/item.asp?upc=" + upcResult.getUPC());
        intent = new Intent(Intent.VIEW_ACTION, uri);
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.URI)) {
      URIParsedResult uriResult = (URIParsedResult) result;
      try {
        intent = new Intent(Intent.VIEW_ACTION, new ContentURI(uriResult.getURI()));
      } catch (URISyntaxException e) {
      }
    } else if (type.equals(ParsedReaderResultType.ANDROID_INTENT)) {
      intent = ((AndroidIntentParsedResult) result).getIntent();
    }
    return intent;
  }

  public void onClick(View view) {
    if (intent != null) {
      captureActivity.startActivity(intent);
    }
  }

  Intent getIntent() {
    return intent;
  }

  private static void putExtra(Intent intent, String key, String value) {
    if (value != null && value.length() > 0) {
      intent.putExtra(key, value);
    }
  }

  private static void putExtra(Intent intent, String key, String[] value) {
    if (value != null && value.length > 0) {
      putExtra(intent, key, value[0]);
    }
  }

}
