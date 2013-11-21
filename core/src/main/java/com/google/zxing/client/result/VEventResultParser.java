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

package com.google.zxing.client.result;

import com.google.zxing.Result;

import java.util.List;

/**
 * Partially implements the iCalendar format's "VEVENT" format for specifying a
 * calendar event. See RFC 2445. This supports SUMMARY, LOCATION, GEO, DTSTART and DTEND fields.
 *
 * @author Sean Owen
 */
public final class VEventResultParser extends ResultParser {

  @Override
  public CalendarParsedResult parse(Result result) {
    String rawText = getMassagedText(result);
    int vEventStart = rawText.indexOf("BEGIN:VEVENT");
    if (vEventStart < 0) {
      return null;
    }

    String summary = matchSingleVCardPrefixedField("SUMMARY", rawText, true);
    String start = matchSingleVCardPrefixedField("DTSTART", rawText, true);
    if (start == null) {
      return null;
    }
    String end = matchSingleVCardPrefixedField("DTEND", rawText, true);
    String duration = matchSingleVCardPrefixedField("DURATION", rawText, true);
    String location = matchSingleVCardPrefixedField("LOCATION", rawText, true);
    String organizer = stripMailto(matchSingleVCardPrefixedField("ORGANIZER", rawText, true));

    String[] attendees = matchVCardPrefixedField("ATTENDEE", rawText, true);
    if (attendees != null) {
      for (int i = 0; i < attendees.length; i++) {
        attendees[i] = stripMailto(attendees[i]);
      }
    }
    String description = matchSingleVCardPrefixedField("DESCRIPTION", rawText, true);

    String geoString = matchSingleVCardPrefixedField("GEO", rawText, true);
    double latitude;
    double longitude;
    if (geoString == null) {
      latitude = Double.NaN;
      longitude = Double.NaN;
    } else {
      int semicolon = geoString.indexOf(';');
      try {
        latitude = Double.parseDouble(geoString.substring(0, semicolon));
        longitude = Double.parseDouble(geoString.substring(semicolon + 1));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }

    try {
      return new CalendarParsedResult(summary,
                                      start,
                                      end,
                                      duration,
                                      location,
                                      organizer,
                                      attendees,
                                      description,
                                      latitude,
                                      longitude);
    } catch (IllegalArgumentException ignored) {
      return null;
    }
  }

  private static String matchSingleVCardPrefixedField(CharSequence prefix,
                                                      String rawText,
                                                      boolean trim) {
    List<String> values = VCardResultParser.matchSingleVCardPrefixedField(prefix, rawText, trim, false);
    return values == null || values.isEmpty() ? null : values.get(0);
  }

  private static String[] matchVCardPrefixedField(CharSequence prefix, String rawText, boolean trim) {
    List<List<String>> values = VCardResultParser.matchVCardPrefixedField(prefix, rawText, trim, false);
    if (values == null || values.isEmpty()) {
      return null;
    }
    int size = values.size();
    String[] result = new String[size];
    for (int i = 0; i < size; i++) {
      result[i] = values.get(i).get(0);
    }
    return result;
  }

  private static String stripMailto(String s) {
    if (s != null && (s.startsWith("mailto:") || s.startsWith("MAILTO:"))) {
      s = s.substring(7);
    }
    return s;
  }

}