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

package com.google.zxing.client.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles any locale-specific logic for the client.
 *
 * @author Sean Owen
 */
public final class LocaleManager {

  private static final String DEFAULT_TLD = "com";
  private static final String DEFAULT_COUNTRY = "US";
  private static final String DEFAULT_LANGUAGE = "en";

  /**
   * Locales (well, countries) where Google web search is available.
   * These should be kept in sync with our translations.
   */
  private static final Map<String,String> GOOGLE_COUNTRY_TLD;
  static {
    GOOGLE_COUNTRY_TLD = new HashMap<>();
    GOOGLE_COUNTRY_TLD.put("AR", "com.ar"); // ARGENTINA
    GOOGLE_COUNTRY_TLD.put("AU", "com.au"); // AUSTRALIA
    GOOGLE_COUNTRY_TLD.put("BR", "com.br"); // BRAZIL
    GOOGLE_COUNTRY_TLD.put("BG", "bg"); // BULGARIA
    GOOGLE_COUNTRY_TLD.put(Locale.CANADA.getCountry(), "ca");
    GOOGLE_COUNTRY_TLD.put(Locale.CHINA.getCountry(), "cn");
    GOOGLE_COUNTRY_TLD.put("CZ", "cz"); // CZECH REPUBLIC
    GOOGLE_COUNTRY_TLD.put("DK", "dk"); // DENMARK
    GOOGLE_COUNTRY_TLD.put("FI", "fi"); // FINLAND
    GOOGLE_COUNTRY_TLD.put(Locale.FRANCE.getCountry(), "fr");
    GOOGLE_COUNTRY_TLD.put(Locale.GERMANY.getCountry(), "de");
    GOOGLE_COUNTRY_TLD.put("GR", "gr"); // GREECE
    GOOGLE_COUNTRY_TLD.put("HU", "hu"); // HUNGARY
    GOOGLE_COUNTRY_TLD.put("ID", "co.id"); // INDONESIA
    GOOGLE_COUNTRY_TLD.put("IL", "co.il"); // ISRAEL
    GOOGLE_COUNTRY_TLD.put(Locale.ITALY.getCountry(), "it");
    GOOGLE_COUNTRY_TLD.put(Locale.JAPAN.getCountry(), "co.jp");
    GOOGLE_COUNTRY_TLD.put(Locale.KOREA.getCountry(), "co.kr");
    GOOGLE_COUNTRY_TLD.put("NL", "nl"); // NETHERLANDS
    GOOGLE_COUNTRY_TLD.put("PL", "pl"); // POLAND
    GOOGLE_COUNTRY_TLD.put("PT", "pt"); // PORTUGAL
    GOOGLE_COUNTRY_TLD.put("RO", "ro"); // ROMANIA    
    GOOGLE_COUNTRY_TLD.put("RU", "ru"); // RUSSIA
    GOOGLE_COUNTRY_TLD.put("SK", "sk"); // SLOVAK REPUBLIC
    GOOGLE_COUNTRY_TLD.put("SI", "si"); // SLOVENIA
    GOOGLE_COUNTRY_TLD.put("ES", "es"); // SPAIN
    GOOGLE_COUNTRY_TLD.put("SE", "se"); // SWEDEN
    GOOGLE_COUNTRY_TLD.put("CH", "ch"); // SWITZERLAND    
    GOOGLE_COUNTRY_TLD.put(Locale.TAIWAN.getCountry(), "tw");
    GOOGLE_COUNTRY_TLD.put("TR", "com.tr"); // TURKEY
    GOOGLE_COUNTRY_TLD.put("UA", "com.ua"); // UKRAINE
    GOOGLE_COUNTRY_TLD.put(Locale.UK.getCountry(), "co.uk");
    GOOGLE_COUNTRY_TLD.put(Locale.US.getCountry(), "com");
  }

  /**
   * Google Product Search for mobile is available in fewer countries than web search. See here:
   * http://support.google.com/merchants/bin/answer.py?hl=en-GB&answer=160619
   */
  private static final Map<String,String> GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD;
  static {
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD = new HashMap<>();
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("AU", "com.au"); // AUSTRALIA
    //GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.CHINA.getCountry(), "cn");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.FRANCE.getCountry(), "fr");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.GERMANY.getCountry(), "de");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.ITALY.getCountry(), "it");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.JAPAN.getCountry(), "co.jp");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("NL", "nl"); // NETHERLANDS
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("ES", "es"); // SPAIN
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put("CH", "ch"); // SWITZERLAND
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.UK.getCountry(), "co.uk");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.US.getCountry(), "com");
  }

  /**
   * Book search is offered everywhere that web search is available.
   */
  private static final Map<String,String> GOOGLE_BOOK_SEARCH_COUNTRY_TLD = GOOGLE_COUNTRY_TLD;

  private static final Collection<String> TRANSLATED_HELP_ASSET_LANGUAGES =
      Arrays.asList("de", "en", "es", "fr", "it", "ja", "ko", "nl", "pt", "ru", "uk", "zh-rCN", "zh-rTW", "zh-rHK");

  private LocaleManager() {}

  /**
   * @param context application's {@link Context}
   * @return country-specific TLD suffix appropriate for the current default locale
   *  (e.g. "co.uk" for the United Kingdom)
   */
  public static String getCountryTLD(Context context) {
    return doGetTLD(GOOGLE_COUNTRY_TLD, context);
  }

  /**
   * The same as above, but specifically for Google Product Search.
   *
   * @param context application's {@link Context}
   * @return The top-level domain to use.
   */
  public static String getProductSearchCountryTLD(Context context) {
    return doGetTLD(GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD, context);
  }

  /**
   * The same as above, but specifically for Google Book Search.
   *
   * @param context application's {@link Context}
   * @return The top-level domain to use.
   */
  public static String getBookSearchCountryTLD(Context context) {
    return doGetTLD(GOOGLE_BOOK_SEARCH_COUNTRY_TLD, context);
  }

  /**
   * Does a given URL point to Google Book Search, regardless of domain.
   *
   * @param url The address to check.
   * @return True if this is a Book Search URL.
   */
  public static boolean isBookSearchUrl(String url) {
    return url.startsWith("http://google.com/books") || url.startsWith("http://books.google.");
  }

  private static String getSystemCountry() {
    Locale locale = Locale.getDefault();
    return locale == null ? DEFAULT_COUNTRY : locale.getCountry();
  }

  private static String getSystemLanguage() {
    Locale locale = Locale.getDefault();
    if (locale == null) {
      return DEFAULT_LANGUAGE;
    }
    String language = locale.getLanguage();
    // Special case Chinese
    if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(language)) {
      return language + "-r" + getSystemCountry();
    }
    return language;
  }

  public static String getTranslatedAssetLanguage() {
    String language = getSystemLanguage();
    return TRANSLATED_HELP_ASSET_LANGUAGES.contains(language) ? language : DEFAULT_LANGUAGE;
  }

  private static String doGetTLD(Map<String,String> map, Context context) {
    String tld = map.get(getCountry(context));
    return tld == null ? DEFAULT_TLD : tld;
  }

  public static String getCountry(Context context) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String countryOverride = prefs.getString(PreferencesActivity.KEY_SEARCH_COUNTRY, "-");
    if (countryOverride != null && !countryOverride.isEmpty() && !"-".equals(countryOverride)) {
      return countryOverride;
    }
    return getSystemCountry();
  }

}
