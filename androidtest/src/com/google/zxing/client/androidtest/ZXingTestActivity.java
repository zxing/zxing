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
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public final class ZXingTestActivity extends Activity {

  private static final String TAG = ZXingTestActivity.class.getSimpleName();
  private static final String PACKAGE_NAME = ZXingTestActivity.class.getPackage().getName();

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.test);
    findViewById(R.id.get_camera_parameters).setOnClickListener(getCameraParameters);
    findViewById(R.id.scan_product).setOnClickListener(scanProduct);
    findViewById(R.id.scan_qr_code).setOnClickListener(scanQRCode);
    findViewById(R.id.scan_anything).setOnClickListener(scanAnything);
    findViewById(R.id.search_book_contents).setOnClickListener(searchBookContents);
    findViewById(R.id.encode_url).setOnClickListener(encodeURL);
    findViewById(R.id.encode_email).setOnClickListener(encodeEmail);
    findViewById(R.id.encode_phone).setOnClickListener(encodePhone);
    findViewById(R.id.encode_sms).setOnClickListener(encodeSMS);
    findViewById(R.id.encode_contact).setOnClickListener(encodeContact);
    findViewById(R.id.encode_location).setOnClickListener(encodeLocation);
    findViewById(R.id.encode_hidden_data).setOnClickListener(encodeHiddenData);
    findViewById(R.id.encode_bad_data).setOnClickListener(encodeBadData);
    findViewById(R.id.share_via_barcode).setOnClickListener(shareViaBarcode);
    findViewById(R.id.run_benchmark).setOnClickListener(runBenchmark);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater menuInflater = getMenuInflater();
    menuInflater.inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_about) {
      int versionCode;
      String versionName;
      try {
        PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
        versionCode = info.versionCode;
        versionName = info.versionName;
      } catch (PackageManager.NameNotFoundException ignored) {
        versionCode = 0;
        versionName = "unknown";
      }
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(
          getString(R.string.app_name) + ' ' + versionName + " (" + versionCode + ')');
      builder.setMessage(getString(R.string.about_message));
      builder.setPositiveButton(R.string.ok_button, null);
      builder.show();

    }
    return super.onOptionsItemSelected(item);
  }
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
    if (result != null) {
      String contents = result.getContents();
      if (contents != null) {
        showDialog(R.string.result_succeeded, result.toString());
      } else {
        showDialog(R.string.result_failed, getString(R.string.result_failed_why));
      }
    }
  }
  

  private final View.OnClickListener getCameraParameters = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      String stats = CameraConfigurationUtils.collectStats(getFlattenedParams());
      writeStats(stats);
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.putExtra(Intent.EXTRA_EMAIL, "zxing-external@google.com");
      intent.putExtra(Intent.EXTRA_SUBJECT, "Camera parameters report");
      intent.putExtra(Intent.EXTRA_TEXT, stats);
      intent.setType("text/plain");
      startActivity(intent);
    }
  };

  private final View.OnClickListener runBenchmark = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, BenchmarkActivity.class.getName());
      startActivity(intent);
    }
  };

  private final View.OnClickListener scanProduct = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      IntentIntegrator integrator = new IntentIntegrator(ZXingTestActivity.this);
      integrator.addExtra("SCAN_WIDTH", 800);
      integrator.addExtra("SCAN_HEIGHT", 200);
      integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
      integrator.addExtra("PROMPT_MESSAGE", "Custom prompt to scan a product");
      integrator.initiateScan(IntentIntegrator.PRODUCT_CODE_TYPES);
    }
  };

  private final View.OnClickListener scanQRCode = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      IntentIntegrator integrator = new IntentIntegrator(ZXingTestActivity.this);
      integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }
  };

  private final View.OnClickListener scanAnything = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      IntentIntegrator integrator = new IntentIntegrator(ZXingTestActivity.this);
      integrator.initiateScan();
    }
  };

  private final View.OnClickListener searchBookContents = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SEARCH_BOOK_CONTENTS");
      intent.putExtra("ISBN", "9780441014989");
      intent.putExtra("QUERY", "future");
      startActivity(intent);
    }
  };

  private final View.OnClickListener encodeURL = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("TEXT_TYPE", "http://www.nytimes.com");
    }
  };

  private final View.OnClickListener encodeEmail = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("EMAIL_TYPE", "foo@example.com");
    }
  };

  private final View.OnClickListener encodePhone = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("PHONE_TYPE", "2125551212");
    }
  };

  private final View.OnClickListener encodeSMS = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("SMS_TYPE", "2125551212");
    }
  };

  private final View.OnClickListener encodeContact = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putString(ContactsContract.Intents.Insert.NAME, "Jenny");
      bundle.putString(ContactsContract.Intents.Insert.PHONE, "8675309");
      bundle.putString(ContactsContract.Intents.Insert.EMAIL, "jenny@the80s.com");
      bundle.putString(ContactsContract.Intents.Insert.POSTAL, "123 Fake St. San Francisco, CA 94102");
      encodeBarcode("CONTACT_TYPE", bundle);
    }
  };

  private final View.OnClickListener encodeLocation = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putFloat("LAT", 40.829208f);
      bundle.putFloat("LONG", -74.191279f);
      encodeBarcode("LOCATION_TYPE", bundle);
    }
  };

  private final View.OnClickListener encodeHiddenData = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      IntentIntegrator integrator = new IntentIntegrator(ZXingTestActivity.this);
      integrator.addExtra("ENCODE_SHOW_CONTENTS", false);
      integrator.shareText("SURPRISE!");
    }
  };

  private final View.OnClickListener encodeBadData = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode(null, "bar");
    }
  };

  private final View.OnClickListener shareViaBarcode = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
      startActivity(new Intent("com.google.zxing.client.android.SHARE"));
    }
  };

  private void showDialog(int title, CharSequence message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setPositiveButton(R.string.ok_button, null);
    builder.show();
  }

  private void encodeBarcode(CharSequence type, CharSequence data) {
    IntentIntegrator integrator = new IntentIntegrator(this);
    integrator.shareText(data, type);
  }

  private void encodeBarcode(CharSequence type, Bundle data) {
    IntentIntegrator integrator = new IntentIntegrator(this);
    integrator.addExtra("ENCODE_DATA", data);
    integrator.shareText(data.toString(), type); // data.toString() isn't used
  }

  private static CharSequence getFlattenedParams() {
    Camera camera = Camera.open();
    if (camera == null) {
      return null;
    }
    try {
      Camera.Parameters parameters = camera.getParameters();
      if (parameters == null) {
        return null;
      }
      return parameters.flatten();
    } finally {
      camera.release();
    }
  }

  private static void writeStats(String resultString) {
    File cameraParamsFile = new File(Environment.getExternalStorageDirectory().getPath() + "/CameraParameters.txt");
    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(cameraParamsFile), Charset.forName("UTF-8"));
      out.write(resultString);
    } catch (IOException e) {
      Log.e(TAG, "Cannot write parameters file ", e);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          Log.w(TAG, e);
        }
      }
    }
  }

}
