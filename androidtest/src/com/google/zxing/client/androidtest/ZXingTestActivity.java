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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public final class ZXingTestActivity extends Activity {

  private static final int ABOUT_ID = Menu.FIRST;
  private static final String PACKAGE_NAME = "com.google.zxing.client.androidtest";

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.test);

    findViewById(R.id.take_test_photos).setOnClickListener(mTakeTestPhotos);
    findViewById(R.id.get_camera_parameters).setOnClickListener(mGetCameraParameters);
    findViewById(R.id.run_benchmark).setOnClickListener(mRunBenchmark);
    findViewById(R.id.scan_product).setOnClickListener(mScanProduct);
    findViewById(R.id.scan_qr_code).setOnClickListener(mScanQRCode);
    findViewById(R.id.scan_anything).setOnClickListener(mScanAnything);
    findViewById(R.id.search_book_contents).setOnClickListener(mSearchBookContents);
    findViewById(R.id.encode_url).setOnClickListener(mEncodeURL);
    findViewById(R.id.encode_email).setOnClickListener(mEncodeEmail);
    findViewById(R.id.encode_phone).setOnClickListener(mEncodePhone);
    findViewById(R.id.encode_sms).setOnClickListener(mEncodeSMS);
    findViewById(R.id.encode_contact).setOnClickListener(mEncodeContact);
    findViewById(R.id.encode_location).setOnClickListener(mEncodeLocation);
    findViewById(R.id.encode_bad_data).setOnClickListener(mEncodeBadData);
    findViewById(R.id.share_via_barcode).setOnClickListener(mShareViaBarcode);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, ABOUT_ID, 0, R.string.about_menu)
        .setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case ABOUT_ID:
        int versionCode = 0;
        String versionName = "unknown";
        try {
          PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
          versionCode = info.versionCode;
          versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name) + " " + versionName + " (" + versionCode +
            ")");
        builder.setMessage(getString(R.string.about_message));
        builder.setPositiveButton(R.string.ok_button, null);
        builder.show();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  public final Button.OnClickListener mTakeTestPhotos = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, CameraTestActivity.class.getName());
      intent.putExtra(CameraTestActivity.GET_CAMERA_PARAMETERS, false);
      startActivity(intent);
    }
  };

  public final Button.OnClickListener mGetCameraParameters = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, CameraTestActivity.class.getName());
      intent.putExtra(CameraTestActivity.GET_CAMERA_PARAMETERS, true);
      startActivity(intent);
    }
  };

  public final Button.OnClickListener mRunBenchmark = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, BenchmarkActivity.class.getName());
      startActivity(intent);
    }
  };

  public final Button.OnClickListener mScanProduct = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
      startActivityForResult(intent, 0);
    }
  };

  public final Button.OnClickListener mScanQRCode = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
      startActivityForResult(intent, 0);
    }
  };

  public final Button.OnClickListener mScanAnything = new Button.OnClickListener() {
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      startActivityForResult(intent, 0);
    }
  };

  public final Button.OnClickListener mSearchBookContents = new Button.OnClickListener() {
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

  public final Button.OnClickListener mEncodeURL = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("TEXT_TYPE", "http://www.nytimes.com");
    }
  };

  public final Button.OnClickListener mEncodeEmail = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("EMAIL_TYPE", "foo@example.com");
    }
  };

  public final Button.OnClickListener mEncodePhone = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("PHONE_TYPE", "2125551212");
    }
  };

  public final Button.OnClickListener mEncodeSMS = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode("SMS_TYPE", "2125551212");
    }
  };

  public final Button.OnClickListener mEncodeContact = new Button.OnClickListener() {
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putString(Contacts.Intents.Insert.NAME, "Jenny");
      bundle.putString(Contacts.Intents.Insert.PHONE, "8675309");
      bundle.putString(Contacts.Intents.Insert.EMAIL, "jenny@the80s.com");
      bundle.putString(Contacts.Intents.Insert.POSTAL, "123 Fake St. San Francisco, CA 94102");
      encodeBarcode("CONTACT_TYPE", bundle);
    }
  };

  public final Button.OnClickListener mEncodeLocation = new Button.OnClickListener() {
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putFloat("LAT", 40.829208f);
      bundle.putFloat("LONG", -74.191279f);
      encodeBarcode("LOCATION_TYPE", bundle);
    }
  };

  public final Button.OnClickListener mEncodeBadData = new Button.OnClickListener() {
    public void onClick(View v) {
      encodeBarcode(null, "bar");
    }
  };

  public final Button.OnClickListener mShareViaBarcode = new Button.OnClickListener() {
    public void onClick(View v) {
      startActivity(new Intent("com.google.zxing.client.android.SHARE"));
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
