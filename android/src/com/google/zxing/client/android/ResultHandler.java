/*
 * Copyright 2008 ZXing authors
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
import android.net.Uri;
import android.provider.Contacts;
import android.view.View;
import android.widget.Button;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.EmailAddressParsedResult;
import com.google.zxing.client.result.GeoParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.SMSParsedResult;
import com.google.zxing.client.result.TelParsedResult;
import com.google.zxing.client.result.UPCParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * Handles the result of barcode decoding in the context of the Android platform,
 * by dispatching the proper intents to open other activities like GMail, Maps, etc.
 *
 * @author srowen@google.com (Sean Owen)
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class ResultHandler implements Button.OnClickListener {

  private final Intent intent;
  private final BarcodeReaderCaptureActivity captureActivity;

  ResultHandler(BarcodeReaderCaptureActivity captureActivity, ParsedResult result) {
    this.captureActivity = captureActivity;
    this.intent = resultToIntent(result);
  }

  private static Intent resultToIntent(ParsedResult result) {
    Intent intent = null;
    ParsedResultType type = result.getType();
    if (type.equals(ParsedResultType.ADDRESSBOOK)) {
      AddressBookParsedResult addressResult = (AddressBookParsedResult) result;
      intent = new Intent(Contacts.Intents.Insert.ACTION, Contacts.People.CONTENT_URI);
      putExtra(intent, Contacts.Intents.Insert.NAME, addressResult.getNames());
      putExtra(intent, Contacts.Intents.Insert.PHONE, addressResult.getPhoneNumbers());
      putExtra(intent, Contacts.Intents.Insert.EMAIL, addressResult.getEmails());
      putExtra(intent, Contacts.Intents.Insert.NOTES, addressResult.getNote());
      putExtra(intent, Contacts.Intents.Insert.POSTAL, addressResult.getAddress());
    } else if (type.equals(ParsedResultType.EMAIL_ADDRESS)) {
      EmailAddressParsedResult emailResult = (EmailAddressParsedResult) result;
      intent = new Intent(Intent.SENDTO_ACTION, Uri.parse(emailResult.getMailtoURI()));
      putExtra(intent, "subject", emailResult.getSubject());
      putExtra(intent, "body", emailResult.getBody());
    } else if (type.equals(ParsedResultType.SMS)) {
      SMSParsedResult smsResult = (SMSParsedResult) result;
      intent = new Intent(Intent.SENDTO_ACTION, Uri.parse(smsResult.getSMSURI()));
    } else if (type.equals(ParsedResultType.TEL)) {
      TelParsedResult telResult = (TelParsedResult) result;
      intent = new Intent(Intent.DIAL_ACTION, Uri.parse(telResult.getTelURI()));
    } else if (type.equals(ParsedResultType.GEO)) {
      GeoParsedResult geoResult = (GeoParsedResult) result;
      intent = new Intent(Intent.VIEW_ACTION, Uri.parse(geoResult.getGeoURI()));
    } else if (type.equals(ParsedResultType.UPC)) {
      UPCParsedResult upcResult = (UPCParsedResult) result;
      Uri uri = Uri.parse("http://www.upcdatabase.com/item.asp?upc=" + upcResult.getUPC());
      intent = new Intent(Intent.VIEW_ACTION, uri);
    } else if (type.equals(ParsedResultType.URI)) {
      URIParsedResult uriResult = (URIParsedResult) result;
      intent = new Intent(Intent.VIEW_ACTION, Uri.parse(uriResult.getURI()));
    } else if (type.equals(ParsedResultType.ANDROID_INTENT)) {
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
