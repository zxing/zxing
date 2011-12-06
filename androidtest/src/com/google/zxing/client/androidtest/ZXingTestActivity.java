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
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

public final class ZXingTestActivity extends Activity {

  private static final String TAG = ZXingTestActivity.class.getSimpleName();
  private static final int ABOUT_ID = Menu.FIRST;
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
    super.onCreateOptionsMenu(menu);
    menu.add(0, ABOUT_ID, 0, R.string.about_menu).setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == ABOUT_ID) {
      int versionCode;
      String versionName;
      try {
        PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
        versionCode = info.versionCode;
        versionName = info.versionName;
      } catch (PackageManager.NameNotFoundException e) {
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
  

  private final Button.OnClickListener getCameraParameters = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      String stats = collectStats();
      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.putExtra(Intent.EXTRA_EMAIL, "zxing-external@google.com");
      intent.putExtra(Intent.EXTRA_SUBJECT, "Camera parameters report");
      intent.putExtra(Intent.EXTRA_TEXT, stats);
      intent.setType("text/plain");
      startActivity(intent);
    }
  };

  private final Button.OnClickListener runBenchmark = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setClassName(ZXingTestActivity.this, BenchmarkActivity.class.getName());
      startActivity(intent);
    }
  };

  private final Button.OnClickListener scanProduct = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SCAN");
      intent.putExtra("SCAN_MODE", "PRODUCT_MODE");
      intent.putExtra("SCAN_WIDTH", 800);
      intent.putExtra("SCAN_HEIGHT", 200);
      intent.putExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
      intent.putExtra("PROMPT_MESSAGE", "Custom prompt to scan a product");
      startActivityForResult(intent, IntentIntegrator.REQUEST_CODE);
    }
  };

  private final Button.OnClickListener scanQRCode = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      IntentIntegrator integrator = new IntentIntegrator(ZXingTestActivity.this);
      integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }
  };

  private final Button.OnClickListener scanAnything = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      IntentIntegrator integrator = new IntentIntegrator(ZXingTestActivity.this);
      integrator.initiateScan();
    }
  };

  private final Button.OnClickListener searchBookContents = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.SEARCH_BOOK_CONTENTS");
      intent.putExtra("ISBN", "9780441014989");
      intent.putExtra("QUERY", "future");
      startActivity(intent);
    }
  };

  private final Button.OnClickListener encodeURL = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("TEXT_TYPE", "http://www.nytimes.com");
    }
  };

  private final Button.OnClickListener encodeEmail = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("EMAIL_TYPE", "foo@example.com");
    }
  };

  private final Button.OnClickListener encodePhone = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("PHONE_TYPE", "2125551212");
    }
  };

  private final Button.OnClickListener encodeSMS = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode("SMS_TYPE", "2125551212");
    }
  };

  private final Button.OnClickListener encodeContact = new Button.OnClickListener() {
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

  private final Button.OnClickListener encodeLocation = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Bundle bundle = new Bundle();
      bundle.putFloat("LAT", 40.829208f);
      bundle.putFloat("LONG", -74.191279f);
      encodeBarcode("LOCATION_TYPE", bundle);
    }
  };

  private final Button.OnClickListener encodeHiddenData = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
      intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
      intent.putExtra("ENCODE_DATA", "SURPRISE!");
      intent.putExtra("ENCODE_SHOW_CONTENTS", false);
      startActivity(intent);
    }
  };

  private final Button.OnClickListener encodeBadData = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      encodeBarcode(null, "bar");
    }
  };

  private final Button.OnClickListener shareViaBarcode = new Button.OnClickListener() {
    @Override
    public void onClick(View v) {
      startActivity(new Intent("com.google.zxing.client.android.SHARE"));
    }
  };

  private void showDialog(int title, CharSequence message) {
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

  private static String getFlattenedParams() {
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
  
  private static String collectStats() {
    StringBuilder result = new StringBuilder(1000);
    
    result.append("BOARD=").append(Build.BOARD).append('\n');
    result.append("BRAND=").append(Build.BRAND).append('\n');
    result.append("CPU_ABI=").append(Build.CPU_ABI).append('\n');
    result.append("DEVICE=").append(Build.DEVICE).append('\n');
    result.append("DISPLAY=").append(Build.DISPLAY).append('\n');
    result.append("FINGERPRINT=").append(Build.FINGERPRINT).append('\n');
    result.append("HOST=").append(Build.HOST).append('\n');
    result.append("ID=").append(Build.ID).append('\n');
    result.append("MANUFACTURER=").append(Build.MANUFACTURER).append('\n');
    result.append("MODEL=").append(Build.MODEL).append('\n');
    result.append("PRODUCT=").append(Build.PRODUCT).append('\n');
    result.append("TAGS=").append(Build.TAGS).append('\n');
    result.append("TIME=").append(Build.TIME).append('\n');
    result.append("TYPE=").append(Build.TYPE).append('\n');
    result.append("USER=").append(Build.USER).append('\n');
    result.append("VERSION.CODENAME=").append(Build.VERSION.CODENAME).append('\n');
    result.append("VERSION.INCREMENTAL=").append(Build.VERSION.INCREMENTAL).append('\n');
    result.append("VERSION.RELEASE=").append(Build.VERSION.RELEASE).append('\n');
    result.append("VERSION.SDK_INT=").append(Build.VERSION.SDK_INT).append('\n');

    String flattened = getFlattenedParams();
    String[] params = flattened.split(";");
    Arrays.sort(params);
    for (String param : params) {
      result.append(param).append('\n');
    }

    String resultString = result.toString();
    writeStats(resultString);

    return resultString;
  }

  private static void writeStats(String resultString) {
    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(new File("/sdcard/CameraParameters.txt")), 
                                   Charset.forName("UTF-8"));
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
