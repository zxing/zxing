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

package com.google.zxing.client.android.encode;

import android.provider.ContactsContract;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.BitMatrix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class QRCodeEncoder {

  private static final String TAG = QRCodeEncoder.class.getSimpleName();

  private static final int WHITE = 0xFFFFFFFF;
  private static final int BLACK = 0xFF000000;

  private final Activity activity;
  private String contents;
  private String displayContents;
  private String title;
  private BarcodeFormat format;
  private final int dimension;
  private final boolean useVCard;

  QRCodeEncoder(Activity activity, Intent intent, int dimension, boolean useVCard) throws WriterException {
    this.activity = activity;
    this.dimension = dimension;
    this.useVCard = useVCard;
    String action = intent.getAction();
    if (action.equals(Intents.Encode.ACTION)) {
      encodeContentsFromZXingIntent(intent);
    } else if (action.equals(Intent.ACTION_SEND)) {
      encodeContentsFromShareIntent(intent);
    }
  }

  String getContents() {
    return contents;
  }

  String getDisplayContents() {
    return displayContents;
  }

  String getTitle() {
    return title;
  }

  boolean isUseVCard() {
    return useVCard;
  }

  // It would be nice if the string encoding lived in the core ZXing library,
  // but we use platform specific code like PhoneNumberUtils, so it can't.
  private boolean encodeContentsFromZXingIntent(Intent intent) {
     // Default to QR_CODE if no format given.
    String formatString = intent.getStringExtra(Intents.Encode.FORMAT);
    format = null;
    if (formatString != null) {
      try {
        format = BarcodeFormat.valueOf(formatString);
      } catch (IllegalArgumentException iae) {
        // Ignore it then
      }
    }
    if (format == null || format == BarcodeFormat.QR_CODE) {
      String type = intent.getStringExtra(Intents.Encode.TYPE);
      if (type == null || type.length() == 0) {
        return false;
      }
      this.format = BarcodeFormat.QR_CODE;
      encodeQRCodeContents(intent, type);
    } else {
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && data.length() > 0) {
        contents = data;
        displayContents = data;
        title = activity.getString(R.string.contents_text);
      }
    }
    return contents != null && contents.length() > 0;
  }

  // Handles send intents from multitude of Android applications
  private void encodeContentsFromShareIntent(Intent intent) throws WriterException {
    // Check if this is a plain text encoding, or contact
    if (intent.hasExtra(Intent.EXTRA_TEXT)) {
      encodeContentsFromShareIntentPlainText(intent);
    } else {
      // Attempt default sharing.
      encodeContentsFromShareIntentDefault(intent);
    }
  }

  private void encodeContentsFromShareIntentPlainText(Intent intent) throws WriterException {
    // Notice: Google Maps shares both URL and details in one text, bummer!
    String theContents = ContactEncoder.trim(intent.getStringExtra(Intent.EXTRA_TEXT));
    // We only support non-empty and non-blank texts.
    // Trim text to avoid URL breaking.
    if (theContents == null || theContents.length() == 0) {
      throw new WriterException("Empty EXTRA_TEXT");
    }
    contents = theContents;
    // We only do QR code.
    format = BarcodeFormat.QR_CODE;
    if (intent.hasExtra(Intent.EXTRA_SUBJECT)) {
      displayContents = intent.getStringExtra(Intent.EXTRA_SUBJECT);
    } else if (intent.hasExtra(Intent.EXTRA_TITLE)) {
      displayContents = intent.getStringExtra(Intent.EXTRA_TITLE);
    } else {
      displayContents = contents;
    }
    title = activity.getString(R.string.contents_text);
  }

  // Handles send intents from the Contacts app, retrieving a contact as a VCARD.
  private void encodeContentsFromShareIntentDefault(Intent intent) throws WriterException {
    format = BarcodeFormat.QR_CODE;
    Bundle bundle = intent.getExtras();
    if (bundle == null) {
      throw new WriterException("No extras");
    }
    Uri uri = (Uri) bundle.getParcelable(Intent.EXTRA_STREAM);
    if (uri == null) {
      throw new WriterException("No EXTRA_STREAM");
    }
    byte[] vcard;
    String vcardString;
    try {
      InputStream stream = activity.getContentResolver().openInputStream(uri);
      int length = stream.available();
      if (length <= 0) {
        throw new WriterException("Content stream is empty");
      }
      vcard = new byte[length];
      int bytesRead = stream.read(vcard, 0, length);
      if (bytesRead < length) {
        throw new WriterException("Unable to fully read available bytes from content stream");
      }
      vcardString = new String(vcard, 0, bytesRead, "UTF-8");
    } catch (IOException ioe) {
      throw new WriterException(ioe);
    }
    Log.d(TAG, "Encoding share intent content:");
    Log.d(TAG, vcardString);
    Result result = new Result(vcardString, vcard, null, BarcodeFormat.QR_CODE);
    ParsedResult parsedResult = ResultParser.parseResult(result);
    if (!(parsedResult instanceof AddressBookParsedResult)) {
      throw new WriterException("Result was not an address");
    }
    encodeQRCodeContents((AddressBookParsedResult) parsedResult);
    if (contents == null || contents.length() == 0) {
      throw new WriterException("No content to encode");
    }
  }

  private void encodeQRCodeContents(Intent intent, String type) {
    if (type.equals(Contents.Type.TEXT)) {
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && data.length() > 0) {
        contents = data;
        displayContents = data;
        title = activity.getString(R.string.contents_text);
      }
    } else if (type.equals(Contents.Type.EMAIL)) {
      String data = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA));
      if (data != null) {
        contents = "mailto:" + data;
        displayContents = data;
        title = activity.getString(R.string.contents_email);
      }
    } else if (type.equals(Contents.Type.PHONE)) {
      String data = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA));
      if (data != null) {
        contents = "tel:" + data;
        displayContents = PhoneNumberUtils.formatNumber(data);
        title = activity.getString(R.string.contents_phone);
      }
    } else if (type.equals(Contents.Type.SMS)) {
      String data = ContactEncoder.trim(intent.getStringExtra(Intents.Encode.DATA));
      if (data != null) {
        contents = "sms:" + data;
        displayContents = PhoneNumberUtils.formatNumber(data);
        title = activity.getString(R.string.contents_sms);
      }
    } else if (type.equals(Contents.Type.CONTACT)) {

      Bundle bundle = intent.getBundleExtra(Intents.Encode.DATA);
      if (bundle != null) {

        String name = bundle.getString(ContactsContract.Intents.Insert.NAME);
        String organization = bundle.getString(ContactsContract.Intents.Insert.COMPANY);
        String address = bundle.getString(ContactsContract.Intents.Insert.POSTAL);
        Collection<String> phones = new ArrayList<String>(Contents.PHONE_KEYS.length);
        for (int x = 0; x < Contents.PHONE_KEYS.length; x++) {
          phones.add(bundle.getString(Contents.PHONE_KEYS[x]));
        }
        Collection<String> emails = new ArrayList<String>(Contents.EMAIL_KEYS.length);
        for (int x = 0; x < Contents.EMAIL_KEYS.length; x++) {
          emails.add(bundle.getString(Contents.EMAIL_KEYS[x]));
        }
        String url = bundle.getString(Contents.URL_KEY);
        String note = bundle.getString(Contents.NOTE_KEY);

        ContactEncoder mecardEncoder = useVCard ? new VCardContactEncoder() : new MECARDContactEncoder();
        String[] encoded = mecardEncoder.encode(Collections.singleton(name),
                                                organization,
                                                Collections.singleton(address),
                                                phones,
                                                emails,
                                                url,
                                                note);
        // Make sure we've encoded at least one field.
        if (encoded[1].length() > 0) {
          contents = encoded[0];
          displayContents = encoded[1];
          title = activity.getString(R.string.contents_contact);
        }

      }

    } else if (type.equals(Contents.Type.LOCATION)) {
      Bundle bundle = intent.getBundleExtra(Intents.Encode.DATA);
      if (bundle != null) {
        // These must use Bundle.getFloat(), not getDouble(), it's part of the API.
        float latitude = bundle.getFloat("LAT", Float.MAX_VALUE);
        float longitude = bundle.getFloat("LONG", Float.MAX_VALUE);
        if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
          contents = "geo:" + latitude + ',' + longitude;
          displayContents = latitude + "," + longitude;
          title = activity.getString(R.string.contents_location);
        }
      }
    }
  }

  private void encodeQRCodeContents(AddressBookParsedResult contact) {
    ContactEncoder encoder = useVCard ? new VCardContactEncoder() : new MECARDContactEncoder();
    String[] encoded = encoder.encode(toIterable(contact.getNames()),
                                      contact.getOrg(),
                                      toIterable(contact.getAddresses()),
                                      toIterable(contact.getPhoneNumbers()),
                                      toIterable(contact.getEmails()),
                                      contact.getURL(),
                                      null);
    // Make sure we've encoded at least one field.
    if (encoded[1].length() > 0) {
      contents = encoded[0];
      displayContents = encoded[1];
      title = activity.getString(R.string.contents_contact);
    }
  }

  private static Iterable<String> toIterable(String[] values) {
    return values == null ? null : Arrays.asList(values);
  }

  Bitmap encodeAsBitmap() throws WriterException {
    String contentsToEncode = contents;
    if (contentsToEncode == null) {
      return null;
    }
    Map<EncodeHintType,Object> hints = null;
    String encoding = guessAppropriateEncoding(contentsToEncode);
    if (encoding != null) {
      hints = new EnumMap<EncodeHintType,Object>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, encoding);
    }
    MultiFormatWriter writer = new MultiFormatWriter();
    BitMatrix result = writer.encode(contentsToEncode, format, dimension, dimension, hints);
    int width = result.getWidth();
    int height = result.getHeight();
    int[] pixels = new int[width * height];
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

  private static String guessAppropriateEncoding(CharSequence contents) {
    // Very crude at the moment
    for (int i = 0; i < contents.length(); i++) {
      if (contents.charAt(i) > 0xFF) {
        return "UTF-8";
      }
    }
    return null;
  }

}
