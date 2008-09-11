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

package com.android.barcodes;

import android.content.Intent;
import android.net.Uri;
import android.provider.Contacts;
import android.view.View;
import android.widget.Button;
import com.google.zxing.Result;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.EmailAddressParsedResult;
import com.google.zxing.client.result.GeoParsedResult;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.SMSParsedResult;
import com.google.zxing.client.result.TelParsedResult;
import com.google.zxing.client.result.UPCParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * Handles the result of barcode decoding in the context of the Android platform, by dispatching the
 * proper intents to open other activities like GMail, Maps, etc.
 */
final class ResultHandler implements Button.OnClickListener {

    private static final String TAG = "ResultHandler";

    private final Intent mIntent;
    private final BarcodesCaptureActivity mCaptureActivity;

    ResultHandler(BarcodesCaptureActivity captureActivity, ParsedResult result) {
        mCaptureActivity = captureActivity;
        mIntent = resultToIntent(result);
    }

    public void onClick(View view) {
        if (mIntent != null) {
            mCaptureActivity.startActivity(mIntent);
        }
    }

    public Intent getIntent() {
        return mIntent;
    }

    public static ParsedResult parseResult(Result rawResult) {
        ParsedResult result = ResultParser.parseResult(rawResult);
        if (result.getType().equals(ParsedResultType.TEXT)) {
            String rawText = rawResult.getText();
            AndroidIntentParsedResult androidResult = AndroidIntentParsedResult.parse(rawText);
            if (androidResult != null) {
                Intent intent = androidResult.getIntent();
                if (!Intent.ACTION_VIEW.equals(intent.getAction())) {
                    // For now, don't take anything that just parses as a View action. A lot
                    // of things are accepted as a View action by default.
                    result = androidResult;
                }
            }
        }
        return result;
    }

    public static int getActionButtonText(ParsedResultType type) {
        int buttonText;
        if (type.equals(ParsedResultType.ADDRESSBOOK)) {
            buttonText = R.string.button_add_contact;
        } else if (type.equals(ParsedResultType.URI)) {
            buttonText = R.string.button_open_browser;
        } else if (type.equals(ParsedResultType.EMAIL_ADDRESS)) {
            buttonText = R.string.button_email;
        } else if (type.equals(ParsedResultType.UPC)) {
            buttonText = R.string.button_lookup_product;
        } else if (type.equals(ParsedResultType.TEL)) {
            buttonText = R.string.button_dial;
        } else if (type.equals(ParsedResultType.GEO)) {
            buttonText = R.string.button_show_map;
        } else if (type.equals(ParsedResultType.ISBN)) {
            buttonText = R.string.button_lookup_book;
        } else {
            buttonText = 0;
        }
        return buttonText;
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
            putExtra(intent, Contacts.Intents.Insert.COMPANY, addressResult.getOrg());
            putExtra(intent, Contacts.Intents.Insert.JOB_TITLE, addressResult.getTitle());
        } else if (type.equals(ParsedResultType.EMAIL_ADDRESS)) {
            EmailAddressParsedResult emailResult = (EmailAddressParsedResult) result;
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(emailResult.getMailtoURI()));
            putExtra(intent, "subject", emailResult.getSubject());
            putExtra(intent, "body", emailResult.getBody());
        } else if (type.equals(ParsedResultType.SMS)) {
            SMSParsedResult smsResult = (SMSParsedResult) result;
            intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(smsResult.getSMSURI()));
            putExtra(intent, "subject", smsResult.getSubject());
            putExtra(intent, "body", smsResult.getBody());
        } else if (type.equals(ParsedResultType.TEL)) {
            TelParsedResult telResult = (TelParsedResult) result;
            intent = new Intent(Intent.ACTION_DIAL, Uri.parse(telResult.getTelURI()));
        } else if (type.equals(ParsedResultType.GEO)) {
            GeoParsedResult geoResult = (GeoParsedResult) result;
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoResult.getGeoURI()));
        } else if (type.equals(ParsedResultType.UPC)) {
            UPCParsedResult upcResult = (UPCParsedResult) result;
            Uri uri = Uri.parse("http://www.google.com/products?q=" + upcResult.getUPC());
            intent = new Intent(Intent.ACTION_VIEW, uri);
        } else if (type.equals(ParsedResultType.ISBN)) {
            ISBNParsedResult isbnResult = (ISBNParsedResult) result;
            Uri uri = Uri.parse("http://www.google.com/products?q=" + isbnResult.getISBN());
            intent = new Intent(Intent.ACTION_VIEW, uri);
        } else if (type.equals(ParsedResultType.URI)) {
            URIParsedResult uriResult = (URIParsedResult) result;
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriResult.getURI()));
        } else if (type.equals(ParsedResultType.ANDROID_INTENT)) {
            intent = ((AndroidIntentParsedResult) result).getIntent();
        }
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
