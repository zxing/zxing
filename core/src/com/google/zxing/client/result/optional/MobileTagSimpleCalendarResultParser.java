/*
 * Copyright 2008 ZXing authors
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

package com.google.zxing.client.result.optional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.result.CalendarParsedResult;

/**
 * <p>Represents a "simple calendar" result encoded according to section 4.9 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author Sean Owen
 */
final class MobileTagSimpleCalendarResultParser extends AbstractMobileTagResultParser {

  public static final String SERVICE_TYPE = "07";

  public static CalendarParsedResult parse(Result result) {
    if (!result.getBarcodeFormat().equals(BarcodeFormat.DATAMATRIX)) {
      return null;
    }
    String rawText = result.getText();
    if (!rawText.startsWith(SERVICE_TYPE)) {
      return null;
    }

    String[] matches = matchDelimitedFields(rawText.substring(2), 6);
    if (matches == null || !isDigits(matches[1], 10) || !isDigits(matches[2], 10)) {
      return null;
    }
    String summary = matches[0];
    String start = expandDateString(matches[1]);
    String end = expandDateString(matches[2]);
    String location = matches[3];
    String attendee = matches[4];
    String title = matches[5];

    try {
      return new CalendarParsedResult(summary, start, end, location, attendee, title);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  private static String expandDateString(String date) {
    if (date == null) {
      return null;
    }
    // Input is of form YYMMddHHmmss, and needs to be YYYYMMdd'T'HHmmss'Z'
    return "20" + date.substring(0, 6) + 'T' + date.substring(6) + "00Z";
  }

}