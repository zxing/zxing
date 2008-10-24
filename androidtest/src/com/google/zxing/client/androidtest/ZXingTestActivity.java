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

package com.google.zxing.client.androidtest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.View;
import android.widget.Button;

public class ZXingTestActivity extends Activity {

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    setContentView(R.layout.test);

    Button test_camera = (Button) findViewById(R.id.test_camera);
    test_camera.setOnClickListener(mTestCamera);

    Button run_benchmark = (Button) findViewById(R.id.run_benchmark);
    run_benchmark.setOnClickListener(mRunBenchmark);

    Button scan_product = (Button) findViewById(R.id.scan_product);
    scan_product.setOnClickListener(mScanProduct);

    Button scan_qr_code = (Button) findViewById(R.id.scan_qr_code);
    scan_qr_code.setOnClickListener(mScanQRCode);

    Button scan_anything = (Button) findViewById(R.id.scan_anything);
    scan_anything.setOnClickListener(mScanAnything);

    Button search_book_contents = (Button) findViewById(R.id.search_book_contents);
    search_book_contents.setOnClickListener(mSearchBookContents);

    Button encode_url = (Button) findViewById(R.id.encode_url);
    encode_url.setOnClickListener(mEncodeURL);

    Button encode_email = (Button) findViewById(R.id.encode_email);
    encode_email.setOnClickListener(mEncodeEmail);

    Button encode_phone = (Button) findViewById(R.id.encode_phone);
    encode_phone.setOnClickListener(mEncodePhone);

    Button encode_sms = (Button) findViewById(R.id.encode_sms);
    encode_sms.setOnClickListener(mEncodeSMS);

    Button encode_contact = (Button) findViewById(R.id.encode_contact);
    encode_contact.setOnClickListener(mEncodeContact);

    Button encode_location = (Button) findViewById(R.id.encode_location);
    encode_location.setOnClickListener(mEncodeLocation);

    Button encode_bad_data = (Button) findViewById(R.id.encode_bad_data);
    encode_bad_data.setOnClickListener(mEncodeBadData);
  }

  public Button.OnClickListener mTestCamera = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, CameraTestActivity.class.getName());
      startActivity(intent);
    }
  };

  public Button.OnClickListener mRunBenchmark = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, BenchmarkActivity.class.getName());
      startActivity(intent);
    }
  };

  public Button.OnClickListener mScanProduct = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
      startActivityForResult(intent, 0);
    }
  };

  public Button.OnClickListener mScanQRCode = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
      startActivityForResult(intent, 0);
    }
  };

  public Button.OnClickListener mScanAnything = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      startActivityForResult(intent, 0);
    }
  };

  public Button.OnClickListener mSearchBookContents = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SEARCH_BOOK_CONTENTS");
      intent.putExtra("ISBN", "9780441014989");
      intent.putExtra("QUERY", "future");
      startActivity(intent);
    }
  };

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    if (requestCode == 0) {
      if (resultCode == RESULT_OK) {
        String contents = intent.getStringExtra("SCAN_RESULT");
        String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
        showDialog(R.string.result_succeeded, "Format: " + format + "\nContents: " + contents);
      } else if (resultCode == RESULT_CANCELED) {
        showDialog(R.string.result_failed, getString(R.string.result_failed_why));
      }
    }
  }

  public Button.OnClickListener mEncodeURL = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("TEXT_TYPE", "http://www.nytimes.com");
    }
  };

  public Button.OnClickListener mEncodeEmail = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("EMAIL_TYPE", "foo@example.com");
    }
  };

  public Button.OnClickListener mEncodePhone = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("PHONE_TYPE", "2125551212");
    }
  };

  public Button.OnClickListener mEncodeSMS = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("SMS_TYPE", "2125551212");
    }
  };

  public Button.OnClickListener mEncodeContact = new Button.OnClickListener() {
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putString(Contacts.Intents.Insert.NAME, "Jenny");
      bundle.putString(Contacts.Intents.Insert.PHONE, "8675309");
      bundle.putString(Contacts.Intents.Insert.EMAIL, "jenny@the80s.com");
      encodeBarcode("CONTACT_TYPE", bundle);
    }
  };

  public Button.OnClickListener mEncodeLocation = new Button.OnClickListener() {
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putFloat("LAT", 40.829208f);
      bundle.putFloat("LONG", -74.191279f);
      encodeBarcode("LOCATION_TYPE", bundle);
    }
  };

  public Button.OnClickListener mEncodeBadData = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode(null, "bar");
    }
  };

  private void showDialog(int title, String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setPositiveButton("OK", null);
    builder.show();
  }

  private void encodeBarcode(String type, String data) {
    Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
    intent.putExtra("ENCODE_TYPE", type);
    intent.putExtra("ENCODE_DATA", data);
    startActivity(intent);
  }

  private void encodeBarcode(String type, Bundle data) {
    Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
    intent.putExtra("ENCODE_TYPE", type);
    intent.putExtra("ENCODE_DATA", data);
    startActivity(intent);
  }

}
