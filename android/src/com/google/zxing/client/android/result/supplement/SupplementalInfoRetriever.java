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
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

import com.google.zxing.client.android.history.HistoryManager;
import com.google.zxing.client.result.ISBNParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ProductParsedResult;
import com.google.zxing.client.result.URIParsedResult;

/**
 * Superclass of implementations which can asynchronously retrieve more information
 * about a barcode scan.
 */
public abstract class SupplementalInfoRetriever extends AsyncTask<Object,Object,Object> {

  private static final String TAG = "SupplementalInfo";

  public static void maybeInvokeRetrieval(TextView textView,
                                          ParsedResult result,
                                          HistoryManager historyManager,
                                          Context context) {
    try {
      if (result instanceof URIParsedResult) {
        SupplementalInfoRetriever uriRetriever =
            new URIResultInfoRetriever(textView, (URIParsedResult) result, historyManager, context);
        uriRetriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        SupplementalInfoRetriever titleRetriever =
            new TitleRetriever(textView, (URIParsedResult) result, historyManager);
        titleRetriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      } else if (result instanceof ProductParsedResult) {
        ProductParsedResult productParsedResult = (ProductParsedResult) result;
        String productID = productParsedResult.getProductID();
        SupplementalInfoRetriever productRetriever =
            new ProductResultInfoRetriever(textView, productID, historyManager, context);
        productRetriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      } else if (result instanceof ISBNParsedResult) {
        String isbn = ((ISBNParsedResult) result).getISBN();
        SupplementalInfoRetriever productInfoRetriever =
            new ProductResultInfoRetriever(textView, isbn, historyManager, context);
        productInfoRetriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        SupplementalInfoRetriever bookInfoRetriever =
            new BookResultInfoRetriever(textView, isbn, historyManager, context);
        bookInfoRetriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    } catch (RejectedExecutionException ree) {
      // do nothing
    }
  }

  private final WeakReference<TextView> textViewRef;
  private final WeakReference<HistoryManager> historyManagerRef;
  private final Collection<Spannable> newContents;
  private final Collection<String[]> newHistories;

  SupplementalInfoRetriever(TextView textView, HistoryManager historyManager) {
    textViewRef = new WeakReference<>(textView);
    historyManagerRef = new WeakReference<>(historyManager);
    newContents = new ArrayList<>();
    newHistories = new ArrayList<>();
  }

  @Override
  public final Object doInBackground(Object... args) {
    try {
      retrieveSupplementalInfo();
    } catch (IOException e) {
      Log.w(TAG, e);
    }
    return null;
  }

  @Override
  protected final void onPostExecute(Object arg) {
    TextView textView = textViewRef.get();
    if (textView != null) {
      for (CharSequence content : newContents) {
        textView.append(content);
      }
      textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
    HistoryManager historyManager = historyManagerRef.get();
    if (historyManager != null) {
      for (String[] text : newHistories) {
        historyManager.addHistoryItemDetails(text[0], text[1]);
      }
    }
  }

  abstract void retrieveSupplementalInfo() throws IOException;

  final void append(String itemID, String source, String[] newTexts, String linkURL) {

    StringBuilder newTextCombined = new StringBuilder();

    if (source != null) {
      newTextCombined.append(source).append(' ');
    }

    int linkStart = newTextCombined.length();

    boolean first = true;
    for (String newText : newTexts) {
      if (first) {
        newTextCombined.append(newText);
        first = false;
      } else {
        newTextCombined.append(" [");
        newTextCombined.append(newText);
        newTextCombined.append(']');
      }
    }

    int linkEnd = newTextCombined.length();

    String newText = newTextCombined.toString();
    Spannable content = new SpannableString(newText + "\n\n");
    if (linkURL != null) {
      // Strangely, some Android browsers don't seem to register to handle HTTP:// or HTTPS://.
      // Lower-case these as it should always be OK to lower-case these schemes.
      if (linkURL.startsWith("HTTP://")) {
        linkURL = "http" + linkURL.substring(4);
      } else if (linkURL.startsWith("HTTPS://")) {
        linkURL = "https" + linkURL.substring(5);
      }
      content.setSpan(new URLSpan(linkURL), linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    newContents.add(content);
    newHistories.add(new String[] {itemID, newText});
  }
  
  static void maybeAddText(String text, Collection<String> texts) {
    if (text != null && !text.isEmpty()) {
      texts.add(text);
    }
  }
  
  static void maybeAddTextSeries(Collection<String> textSeries, Collection<String> texts) {
    if (textSeries != null && !textSeries.isEmpty()) {
      boolean first = true;
      StringBuilder authorsText = new StringBuilder();
      for (String author : textSeries) {
        if (first) {
          first = false;
        } else {
          authorsText.append(", ");
        }
        authorsText.append(author);
      }
      texts.add(authorsText.toString());
    }
  }

}
