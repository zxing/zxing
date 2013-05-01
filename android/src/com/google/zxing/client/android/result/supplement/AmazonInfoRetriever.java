/*
 * Copyright 2013 ZXing authors
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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import android.content.Context;
import android.widget.TextView;
import com.google.zxing.client.android.HttpHelper;
import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.history.HistoryManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * @author Sean Owen
 */
final class AmazonInfoRetriever extends SupplementalInfoRetriever {

  private final String type;
  private final String productID;
  private final String country;
  
  AmazonInfoRetriever(TextView textView,
                      String type,
                      String productID,
                      HistoryManager historyManager,
                      Context context) {
    super(textView, historyManager);
    String country = LocaleManager.getCountry(context);
    if ("ISBN".equals(type) && !Locale.US.getCountry().equals(country)) {
      type = "EAN";
    }
    this.type = type;
    this.productID = productID;
    this.country = country;
  }

  @Override
  void retrieveSupplementalInfo() throws IOException {

    CharSequence contents =  
        HttpHelper.downloadViaHttp("https://bsplus.srowen.com/ss?c=" + country + "&t=" + type + "&i=" + productID,
                                   HttpHelper.ContentType.XML);

    String detailPageURL = null;
    Collection<String> authors = new ArrayList<String>();
    String title = null;
    String formattedPrice = null;
    boolean error = false;

    try {
      XmlPullParser xpp = buildParser(contents);

      boolean seenItem = false;
      boolean seenLowestNewPrice = false;
      for (int eventType = xpp.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xpp.next()) {
        if (eventType == XmlPullParser.START_TAG) {
          String name = xpp.getName();
          if ("Item".equals(name)) {
            if (seenItem) {
              break;
            } else {
              seenItem = true;
            }
          } else if ("DetailPageURL".equals(name)) {
            assertTextNext(xpp);
            detailPageURL = xpp.getText();
          } else if ("Author".equals(name)) {
            assertTextNext(xpp);
            authors.add(xpp.getText());
          } else if ("Title".equals(name)) {
            assertTextNext(xpp);
            title = xpp.getText();
          } else if ("LowestNewPrice".equals(name)) {
            seenLowestNewPrice = true;
          } else if ("FormattedPrice".equals(name)) {
            if (seenLowestNewPrice) {
              assertTextNext(xpp);
              formattedPrice = xpp.getText();
              seenLowestNewPrice = false;
            }
          } else if ("Errors".equals(name)) {
            error = true;
            break;
          }
        }
      }

    } catch (XmlPullParserException xppe) {
      throw new IOException(xppe.toString());
    }
    
    if (error || detailPageURL == null) {
      return;
    }

    Collection<String> newTexts = new ArrayList<String>();
    maybeAddText(title, newTexts);
    maybeAddTextSeries(authors, newTexts);
    maybeAddText(formattedPrice, newTexts);

    append(productID, "Amazon", newTexts.toArray(new String[newTexts.size()]), detailPageURL);
  }
  
  private static void assertTextNext(XmlPullParser xpp) throws XmlPullParserException, IOException {
    if (xpp.next() != XmlPullParser.TEXT) {
      throw new IOException();
    }
  }
  
  private static XmlPullParser buildParser(CharSequence contents) throws XmlPullParserException {
    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    factory.setNamespaceAware(true);
    XmlPullParser xpp = factory.newPullParser();
    xpp.setInput(new StringReader(contents.toString()));
    return xpp;
  }

}
