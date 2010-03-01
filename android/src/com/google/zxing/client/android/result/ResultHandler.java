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

package com.google.zxing.client.android.result;

import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.Intents;
import com.google.zxing.client.android.LocaleManager;
import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.client.android.book.SearchBookContentsActivity;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Contacts;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A base class for the Android-specific barcode handlers. These allow the app to polymorphically
 * suggest the appropriate actions for each data type.
 *
 * This class also contains a bunch of utility methods to take common actions like opening a URL.
 * They could easily be moved into a helper object, but it can't be static because the Activity
 * instance is needed to launch an intent.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public abstract class ResultHandler {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
  private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

  private static final String GOOGLE_SHOPPER_PACKAGE = "com.google.android.apps.shopper";
  private static final String GOOGLE_SHOPPER_ACTIVITY = GOOGLE_SHOPPER_PACKAGE +
      ".results.SearchResultsActivity";
  private static final String MARKET_URI_PREFIX = "market://search?q=pname:";
  private static final String MARKET_REFERRER_SUFFIX =
      "&referrer=utm_source%3Dbarcodescanner%26utm_medium%3Dapps%26utm_campaign%3Dscan";

  public static final int MAX_BUTTON_COUNT = 4;

  private final ParsedResult result;
  private final Activity activity;

  private final DialogInterface.OnClickListener shopperMarketListener =
      new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialogInterface, int which) {
      launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI_PREFIX +
          GOOGLE_SHOPPER_PACKAGE + MARKET_REFERRER_SUFFIX)));
    }
  };

  ResultHandler(Activity activity, ParsedResult result) {
    this.result = result;
    this.activity = activity;
  }

  ParsedResult getResult() {
    return result;
  }

  /**
   * Indicates how many buttons the derived class wants shown.
   *
   * @return The integer button count.
   */
  public abstract int getButtonCount();

  /**
   * The text of the nth action button.
   *
   * @param index From 0 to getButtonCount() - 1
   * @return The button text as a resource ID
   */
  public abstract int getButtonText(int index);


  /**
   * Execute the action which corresponds to the nth button.
   *
   * @param index The button that was clicked.
   */
  public abstract void handleButtonPress(int index);

  /**
   * Create a possibly styled string for the contents of the current barcode.
   *
   * @return The text to be displayed.
   */
  public CharSequence getDisplayContents() {
    String contents = result.getDisplayResult();
    return contents.replace("\r", "");
  }

  /**
   * A string describing the kind of barcode that was found, e.g. "Found contact info".
   *
   * @return The resource ID of the string.
   */
  public abstract int getDisplayTitle();

  /**
   * A convenience method to get the parsed type. Should not be overridden.
   *
   * @return The parsed type, e.g. URI or ISBN
   */
  public final ParsedResultType getType() {
    return result.getType();
  }

  /**
   * Sends an intent to create a new calendar event by prepopulating the Add Event UI. Older
   * versions of the system have a bug where the event title will not be filled out.
   *
   * @param summary A description of the event
   * @param start   The start time as yyyyMMdd or yyyyMMdd'T'HHmmss or yyyyMMdd'T'HHmmss'Z'
   * @param end     The end time as yyyyMMdd or yyyyMMdd'T'HHmmss or yyyyMMdd'T'HHmmss'Z'
   */
  final void addCalendarEvent(String summary, String start, String end) {
    Intent intent = new Intent(Intent.ACTION_EDIT);
    intent.setType("vnd.android.cursor.item/event");
    intent.putExtra("beginTime", calculateMilliseconds(start));
    if (start.length() == 8) {
      intent.putExtra("allDay", true);
    }
    intent.putExtra("endTime", calculateMilliseconds(end));
    intent.putExtra("title", summary);
    launchIntent(intent);
  }

  private static long calculateMilliseconds(String when) {
    if (when.length() == 8) {
      // Only contains year/month/day
      Date date;
      synchronized (DATE_FORMAT) {
        date = DATE_FORMAT.parse(when, new ParsePosition(0));
      }
      return date.getTime();
    } else {
      // The when string can be local time, or UTC if it ends with a Z
      Date date;
      synchronized (DATE_TIME_FORMAT) {
       date = DATE_TIME_FORMAT.parse(when.substring(0, 15), new ParsePosition(0));
      }
      long milliseconds = date.getTime();
      if (when.length() == 16 && when.charAt(15) == 'Z') {
        Calendar calendar = new GregorianCalendar();
        int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        milliseconds += offset;
      }
      return milliseconds;
    }
  }

  final void addContact(String[] names, String[] phoneNumbers, String[] emails, String note,
                         String address, String org, String title) {

    // Only use the first name in the array, if present.
    Intent intent = new Intent(Contacts.Intents.Insert.ACTION, Contacts.People.CONTENT_URI);
    putExtra(intent, Contacts.Intents.Insert.NAME, names != null ? names[0] : null);

    int phoneCount = Math.min((phoneNumbers != null) ? phoneNumbers.length : 0,
        Contents.PHONE_KEYS.length);
    for (int x = 0; x < phoneCount; x++) {
      putExtra(intent, Contents.PHONE_KEYS[x], phoneNumbers[x]);
    }

    int emailCount = Math.min((emails != null) ? emails.length : 0, Contents.EMAIL_KEYS.length);
    for (int x = 0; x < emailCount; x++) {
      putExtra(intent, Contents.EMAIL_KEYS[x], emails[x]);
    }

    putExtra(intent, Contacts.Intents.Insert.NOTES, note);
    putExtra(intent, Contacts.Intents.Insert.POSTAL, address);
    putExtra(intent, Contacts.Intents.Insert.COMPANY, org);
    putExtra(intent, Contacts.Intents.Insert.JOB_TITLE, title);
    launchIntent(intent);
  }

  final void shareByEmail(String contents) {
    sendEmailFromUri("mailto:", activity.getString(R.string.msg_share_subject_line), contents);
  }

  final void sendEmail(String address, String subject, String body) {
    sendEmailFromUri("mailto:" + address, subject, body);
  }

  // Use public Intent fields rather than private GMail app fields to specify subject and body.
  final void sendEmailFromUri(String uri, String subject, String body) {
    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(uri));
    putExtra(intent, Intent.EXTRA_SUBJECT, subject);
    putExtra(intent, Intent.EXTRA_TEXT, body);
    intent.setType("text/plain");
    launchIntent(intent);
  }

  final void shareBySMS(String contents) {
    sendSMSFromUri("smsto:", activity.getString(R.string.msg_share_subject_line) + ":\n" +
        contents);
  }

  final void sendSMS(String phoneNumber, String body) {
    sendSMSFromUri("smsto:" + phoneNumber, body);
  }

  final void sendSMSFromUri(String uri, String body) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
    putExtra(intent, "sms_body", body);
    // Exit the app once the SMS is sent
    intent.putExtra("compose_mode", true);
    launchIntent(intent);
  }

  final void sendMMS(String phoneNumber, String subject, String body) {
    sendMMSFromUri("mmsto:" + phoneNumber, subject, body);
  }

  final void sendMMSFromUri(String uri, String subject, String body) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
    // The Messaging app needs to see a valid subject or else it will treat this an an SMS.
    if (subject == null || subject.length() == 0) {
      putExtra(intent, "subject", activity.getString(R.string.msg_default_mms_subject));
    } else {
      putExtra(intent, "subject", subject);
    }
    putExtra(intent, "sms_body", body);
    intent.putExtra("compose_mode", true);
    launchIntent(intent);
  }

  final void dialPhone(String phoneNumber) {
    launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber)));
  }

  final void dialPhoneFromUri(String uri) {
    launchIntent(new Intent(Intent.ACTION_DIAL, Uri.parse(uri)));
  }

  final void openMap(String geoURI) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)));
  }

  /**
   * Do a geo search using the address as the query.
   *
   * @param address The address to find
   * @param title An optional title, e.g. the name of the business at this address
   */
  final void searchMap(String address, String title) {
    String query = address;
    if (title != null && title.length() > 0) {
      query = query + " (" + title + ')';
    }
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))));
  }

  final void getDirections(double latitude, double longitude) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google." +
        LocaleManager.getCountryTLD() + "/maps?f=d&daddr=" + latitude + ',' + longitude)));
  }

  // Uses the mobile-specific version of Product Search, which is formatted for small screens.
  final void openProductSearch(String upc) {
    Uri uri = Uri.parse("http://www.google." + LocaleManager.getProductSearchCountryTLD() +
        "/m/products?q=" + upc + "&source=zxing");
    launchIntent(new Intent(Intent.ACTION_VIEW, uri));
  }

  final void openBookSearch(String isbn) {
    Uri uri = Uri.parse("http://books.google." + LocaleManager.getBookSearchCountryTLD() +
        "/books?vid=isbn" + isbn);
    launchIntent(new Intent(Intent.ACTION_VIEW, uri));
  }

  final void searchBookContents(String isbn) {
    Intent intent = new Intent(Intents.SearchBookContents.ACTION);
    intent.setClassName(activity, SearchBookContentsActivity.class.getName());
    putExtra(intent, Intents.SearchBookContents.ISBN, isbn);
    launchIntent(intent);
  }

  final void openURL(String url) {
    launchIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
  }

  final void webSearch(String query) {
    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
    intent.putExtra("query", query);
    launchIntent(intent);
  }

  final void openGoogleShopper(String query) {
    try {
      activity.getPackageManager().getPackageInfo(GOOGLE_SHOPPER_PACKAGE, 0);
      // If we didn't throw, Shopper is installed, so launch it.
      Intent intent = new Intent(Intent.ACTION_SEARCH);
      intent.setClassName(GOOGLE_SHOPPER_PACKAGE, GOOGLE_SHOPPER_ACTIVITY);
      intent.putExtra(SearchManager.QUERY, query);
      activity.startActivity(intent);
    } catch (PackageManager.NameNotFoundException e) {
      // Otherwise offer to install it from Market.
      AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder.setTitle(R.string.msg_google_shopper_missing);
      builder.setMessage(R.string.msg_install_google_shopper);
      builder.setIcon(R.drawable.shopper_icon);
      builder.setPositiveButton(R.string.button_ok, shopperMarketListener);
      builder.setNegativeButton(R.string.button_cancel, null);
      builder.show();
    }
  }

  void launchIntent(Intent intent) {
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      try {
        activity.startActivity(intent);
      } catch (ActivityNotFoundException e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.msg_intent_failed);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
      }
    }
  }

  private static void putExtra(Intent intent, String key, String value) {
    if (value != null && value.length() > 0) {
      intent.putExtra(key, value);
    }
  }

  protected void showNotOurResults(int index, AlertDialog.OnClickListener proceedListener) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
    if (prefs.getBoolean(PreferencesActivity.KEY_NOT_OUR_RESULTS_SHOWN, false)) {
      // already seen it, just proceed
      proceedListener.onClick(null, index);
    } else {
      // note the user has seen it
      prefs.edit().putBoolean(PreferencesActivity.KEY_NOT_OUR_RESULTS_SHOWN, true).commit();
      AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder.setMessage(R.string.msg_not_our_results);
      builder.setPositiveButton(R.string.button_ok, proceedListener);
      builder.show();
    }
  }

}
