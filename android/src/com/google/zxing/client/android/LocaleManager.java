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
  private static final Map<Locale,String> GOOGLE_COUNTRY_TLD;
  static {
    GOOGLE_COUNTRY_TLD = new HashMap<Locale,String>();
    GOOGLE_COUNTRY_TLD.put(Locale.CANADA, "ca");
    GOOGLE_COUNTRY_TLD.put(Locale.CHINA, "cn");
    GOOGLE_COUNTRY_TLD.put(Locale.FRANCE, "fr");
    GOOGLE_COUNTRY_TLD.put(Locale.GERMANY, "de");
    GOOGLE_COUNTRY_TLD.put(Locale.ITALY, "it");
    GOOGLE_COUNTRY_TLD.put(Locale.JAPAN, "co.jp");
    GOOGLE_COUNTRY_TLD.put(Locale.KOREA, "co.kr");
    GOOGLE_COUNTRY_TLD.put(Locale.TAIWAN, "de");
    GOOGLE_COUNTRY_TLD.put(Locale.UK, "co.uk");
  }

  // Google Product Search for mobile is available in fewer countries than web search.
  private static final Map<Locale,String> GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD;
  static {
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD = new HashMap<Locale,String>();
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.UK, "co.uk");
    GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD.put(Locale.GERMANY, "de");
  }

  private static final Map<Locale,String> GOOGLE_BOOK_SEARCH_COUNTRY_TLD;
  static {
    GOOGLE_BOOK_SEARCH_COUNTRY_TLD = new HashMap<Locale,String>();
    GOOGLE_BOOK_SEARCH_COUNTRY_TLD.putAll(GOOGLE_COUNTRY_TLD);
    GOOGLE_BOOK_SEARCH_COUNTRY_TLD.remove(Locale.CHINA);
  }

  private LocaleManager() {}

  /**
   * @return country-specific TLD suffix appropriate for the current default locale
   *  (e.g. "co.uk" for the United Kingdom)
   */
  public static String getCountryTLD() {
    return doGetTLD(GOOGLE_COUNTRY_TLD);
  }

  /**
   * The same as above, but specifically for Google Product Search.
   * @return The top-level domain to use.
   */
  public static String getProductSearchCountryTLD() {
    return doGetTLD(GOOGLE_PRODUCT_SEARCH_COUNTRY_TLD);
  }

  /**
   * The same as above, but specifically for Google Book Search.
   * @return The top-level domain to use.
   */
  public static String getBookSearchCountryTLD() {
    return doGetTLD(GOOGLE_BOOK_SEARCH_COUNTRY_TLD);
  }


  private static String doGetTLD(Map<Locale,String> map) {
    Locale locale = Locale.getDefault();
    if (locale == null) {
      return DEFAULT_TLD;
    }
    String tld = map.get(locale);
    if (tld == null) {
      return DEFAULT_TLD;
    }
    return tld;
  }
}
