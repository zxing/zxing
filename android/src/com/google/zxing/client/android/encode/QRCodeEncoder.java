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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.common.ByteMatrix;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class QRCodeEncoder {

  private static final String TAG = "QRCodeEncoder";

  private final Activity activity;
  private String contents;
  private String displayContents;
  private String title;
  private BarcodeFormat format;

  QRCodeEncoder(Activity activity, Intent intent) {
    this.activity = activity;
    if (intent == null) {
      throw new IllegalArgumentException("No valid data to encode.");
    }

    String action = intent.getAction();
    if (action.equals(Intents.Encode.ACTION)) {
      if (!encodeContentsFromZXingIntent(intent)) {
        throw new IllegalArgumentException("No valid data to encode.");
      }
    } else if (action.equals(Intent.ACTION_SEND)) {
      if (!encodeContentsFromShareIntent(intent)) {
        throw new IllegalArgumentException("No valid data to encode.");
      }
    }
  }

  public void requestBarcode(Handler handler, int pixelResolution) {
    Thread encodeThread = new EncodeThread(contents, handler, pixelResolution,
        format);
    encodeThread.start();
  }

  public String getContents() {
    return contents;
  }

  public String getDisplayContents() {
    return displayContents;
  }

  public String getTitle() {
    return title;
  }

  public String getFormat() {
    return format.toString();
  }

  // It would be nice if the string encoding lived in the core ZXing library,
  // but we use platform specific code like PhoneNumberUtils, so it can't.
  private boolean encodeContentsFromZXingIntent(Intent intent) {
     // Default to QR_CODE if no format given.
    String format = intent.getStringExtra(Intents.Encode.FORMAT);
    if (format == null || format.length() == 0 ||
        format.equals(Contents.Format.QR_CODE)) {
      String type = intent.getStringExtra(Intents.Encode.TYPE);
      if (type == null || type.length() == 0) {
        return false;
      }
      this.format = BarcodeFormat.QR_CODE;
      encodeQRCodeContents(intent, type);
    } else {
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && data.length() != 0) {
        contents = data;
        displayContents = data;
        title = activity.getString(R.string.contents_text);
        if (format.equals(Contents.Format.CODE_128)) {
          this.format = BarcodeFormat.CODE_128;
        } else if (format.equals(Contents.Format.CODE_39)) {
          this.format = BarcodeFormat.CODE_39;
        } else if (format.equals(Contents.Format.EAN_8)) {
          this.format = BarcodeFormat.EAN_8;
        } else if (format.equals(Contents.Format.EAN_13)) {
          this.format = BarcodeFormat.EAN_13;
        } else if (format.equals(Contents.Format.UPC_A)) {
          this.format = BarcodeFormat.UPC_A;
        } else if (format.equals(Contents.Format.UPC_E)) {
          this.format = BarcodeFormat.UPC_E;
        }
      }
    }
    return contents != null && contents.length() > 0;
  }

  // Handles send intents from the Contacts app, retrieving a contact as a VCARD.
  private boolean encodeContentsFromShareIntent(Intent intent) {
    format = BarcodeFormat.QR_CODE;
    try {
      Uri uri = (Uri)intent.getExtras().getParcelable(Intent.EXTRA_STREAM);
      InputStream stream = activity.getContentResolver().openInputStream(uri);
      int length = stream.available();
      byte[] vcard = new byte[length];
      stream.read(vcard, 0, length);
      String vcardString = new String(vcard, "UTF-8");
      Log.d(TAG, "Encoding share intent content: " + vcardString);
      Result result = new Result(vcardString, vcard, null, BarcodeFormat.QR_CODE);
      ParsedResult parsedResult = ResultParser.parseResult(result);
      if (!(parsedResult instanceof AddressBookParsedResult)) {
        return false;
      }
      if (!encodeQRCodeContents((AddressBookParsedResult) parsedResult)) {
        return false;
      }
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      return false;
    } catch (NullPointerException e) {
      // In case the uri was not found in the Intent.
      return false;
    }
    return contents != null && contents.length() > 0;
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
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && data.length() > 0) {
        contents = "mailto:" + data;
        displayContents = data;
        title = activity.getString(R.string.contents_email);
      }
    } else if (type.equals(Contents.Type.PHONE)) {
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && data.length() > 0) {
        contents = "tel:" + data;
        displayContents = PhoneNumberUtils.formatNumber(data);
        title = activity.getString(R.string.contents_phone);
      }
    } else if (type.equals(Contents.Type.SMS)) {
      String data = intent.getStringExtra(Intents.Encode.DATA);
      if (data != null && data.length() > 0) {
        contents = "sms:" + data;
        displayContents = PhoneNumberUtils.formatNumber(data);
        title = activity.getString(R.string.contents_sms);
      }
    } else if (type.equals(Contents.Type.CONTACT)) {
      Bundle bundle = intent.getBundleExtra(Intents.Encode.DATA);
      if (bundle != null) {
        StringBuilder newContents = new StringBuilder();
        StringBuilder newDisplayContents = new StringBuilder();
        newContents.append("MECARD:");
        String name = bundle.getString(Contacts.Intents.Insert.NAME);
        if (name != null && name.length() > 0) {
          newContents.append("N:").append(name).append(';');
          newDisplayContents.append(name);
        }
        String address = bundle.getString(Contacts.Intents.Insert.POSTAL);
        if (address != null && address.length() > 0) {
          newContents.append("ADR:").append(address).append(';');
          newDisplayContents.append('\n').append(address);
        }
        for (int x = 0; x < Contents.PHONE_KEYS.length; x++) {
          String phone = bundle.getString(Contents.PHONE_KEYS[x]);
          if (phone != null && phone.length() > 0) {
            newContents.append("TEL:").append(phone).append(';');
            newDisplayContents.append('\n').append(PhoneNumberUtils.formatNumber(phone));
          }
        }
        for (int x = 0; x < Contents.EMAIL_KEYS.length; x++) {
          String email = bundle.getString(Contents.EMAIL_KEYS[x]);
          if (email != null && email.length() > 0) {
            newContents.append("EMAIL:").append(email).append(';');
            newDisplayContents.append('\n').append(email);
          }
        }
        // Make sure we've encoded at least one field.
        if (newDisplayContents.length() > 0) {
          newContents.append(';');
          contents = newContents.toString();
          displayContents = newDisplayContents.toString();
          title = activity.getString(R.string.contents_contact);
        } else {
          contents = null;
          displayContents = null;
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

  private boolean encodeQRCodeContents(AddressBookParsedResult contact) {
    StringBuilder newContents = new StringBuilder();
    StringBuilder newDisplayContents = new StringBuilder();
    newContents.append("MECARD:");
    String[] names = contact.getNames();
    if (names != null && names.length > 0) {
      newContents.append("N:").append(names[0]).append(';');
      newDisplayContents.append(names[0]);
    }
    String[] addresses = contact.getAddresses();
    if (addresses != null) {
      for (int x = 0; x < addresses.length; x++) {
        if (addresses[x] != null && addresses[x].length() > 0) {
          newContents.append("ADR:").append(addresses[x]).append(';');
          newDisplayContents.append('\n').append(addresses[x]);
        }
      }
    }
    String[] phoneNumbers = contact.getPhoneNumbers();
    if (phoneNumbers != null) {
      for (int x = 0; x < phoneNumbers.length; x++) {
        String phone = phoneNumbers[x];
        if (phone != null && phone.length() > 0) {
          newContents.append("TEL:").append(phone).append(';');
          newDisplayContents.append('\n').append(PhoneNumberUtils.formatNumber(phone));
        }
      }
    }
    String[] emails = contact.getEmails();
    if (emails != null) {
      for (int x = 0; x < emails.length; x++) {
        String email = emails[x];
        if (email != null && email.length() > 0) {
          newContents.append("EMAIL:").append(email).append(';');
          newDisplayContents.append('\n').append(email);
        }
      }
    }
    String url = contact.getURL();
    if (url != null && url.length() > 0) {
      newContents.append("URL:").append(url).append(';');
      newDisplayContents.append('\n').append(url);
    }
    // Make sure we've encoded at least one field.
    if (newDisplayContents.length() > 0) {
      newContents.append(';');
      contents = newContents.toString();
      displayContents = newDisplayContents.toString();
      title = activity.getString(R.string.contents_contact);
      return true;
    } else {
      contents = null;
      displayContents = null;
      return false;
    }
  }

  private static final class EncodeThread extends Thread {
    private static final String TAG = "EncodeThread";

    private final String contents;
    private final Handler handler;
    private final int pixelResolution;
    private final BarcodeFormat format;

    EncodeThread(String contents, Handler handler, int pixelResolution,
        BarcodeFormat format) {
      this.contents = contents;
      this.handler = handler;
      this.pixelResolution = pixelResolution;
      this.format = format;
    }

    @Override
    public void run() {
      try {
        ByteMatrix result = new MultiFormatWriter().encode(contents, format,
            pixelResolution, pixelResolution);
        int width = result.getWidth();
        int height = result.getHeight();
        byte[][] array = result.getArray();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            int grey = array[y][x] & 0xff;
            // pixels[y * width + x] = (0xff << 24) | (grey << 16) | (grey << 8) | grey;
            pixels[y * width + x] = 0xff000000 | (0x00010101 * grey);
          }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        Message message = Message.obtain(handler, R.id.encode_succeeded);
        message.obj = bitmap;
        message.sendToTarget();
      } catch (WriterException e) {
        Log.e(TAG, e.toString());
        Message message = Message.obtain(handler, R.id.encode_failed);
        message.sendToTarget();
      } catch (IllegalArgumentException e) {
        Log.e(TAG, e.toString());
        Message message = Message.obtain(handler, R.id.encode_failed);
        message.sendToTarget();
      }
    }
  }
}
