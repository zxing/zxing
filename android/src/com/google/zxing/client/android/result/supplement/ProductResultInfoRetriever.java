/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android.result.supplement;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import com.google.zxing.client.android.AndroidHttpClient;
import com.google.zxing.client.android.LocaleManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProductResultInfoRetriever extends SupplementalInfoRetriever {

  private static final String TAG = ProductResultInfoRetriever.class.getSimpleName();

  private static final String BASE_PRODUCT_URI =
      "http://www.google." + LocaleManager.getProductSearchCountryTLD() +
          "/m/products?ie=utf8&oe=utf8&scoring=p&source=zxing&q=";
  private static final Pattern PRODUCT_NAME_PRICE_PATTERN =
      Pattern.compile("owb63p\">([^<]+).+zdi3pb\">([^<]+)");


  private final String productID;

  ProductResultInfoRetriever(TextView textView, String productID, Handler handler,
      Context context) {
    super(textView, handler, context);
    this.productID = productID;
  }

  @Override
  void retrieveSupplementalInfo() throws IOException, InterruptedException {

    String encodedProductID = URLEncoder.encode(productID, "UTF-8");
    String uri = BASE_PRODUCT_URI + encodedProductID;

    HttpUriRequest head = new HttpGet(uri);
    AndroidHttpClient client = AndroidHttpClient.newInstance(null);
    HttpResponse response = client.execute(head);
    int status = response.getStatusLine().getStatusCode();
    if (status != 200) {
      return;
    }

    String content = consume(response.getEntity());
    Matcher matcher = PRODUCT_NAME_PRICE_PATTERN.matcher(content);
    if (matcher.find()) {
      append(matcher.group(1));
      append(matcher.group(2));
    }
    setLink(uri);
  }

  private static String consume(HttpEntity entity) {
    Log.d(TAG, "Consuming entity");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream in = null;
    try {
      in = entity.getContent();
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, bytesRead);
      }
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      // continue
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ioe) {
          // continue
        }
      }
    }
    try {
      return new String(out.toByteArray(), "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      // can't happen
      throw new IllegalStateException(uee);
    }
  }

}
