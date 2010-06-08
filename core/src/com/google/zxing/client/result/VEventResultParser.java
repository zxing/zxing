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

/**
 * Partially implements the iCalendar format's "VEVENT" format for specifying a
 * calendar event. See RFC 2445. This supports SUMMARY, LOCATION, DTSTART and DTEND fields.
 *
 * @author Sean Owen
 */
final class VEventResultParser extends ResultParser {

  private VEventResultParser() {
  }

  public static CalendarParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null) {
      return null;
    }
    int vEventStart = rawText.indexOf("BEGIN:VEVENT");
    if (vEventStart < 0) {
      return null;
    }

    String summary = VCardResultParser.matchSingleVCardPrefixedField("SUMMARY", rawText, true);
    String start = VCardResultParser.matchSingleVCardPrefixedField("DTSTART", rawText, true);
    String end = VCardResultParser.matchSingleVCardPrefixedField("DTEND", rawText, true);
    String location = VCardResultParser.matchSingleVCardPrefixedField("LOCATION", rawText, true);
    String description = VCardResultParser.matchSingleVCardPrefixedField("DESCRIPTION", rawText, true);
    try {
      return new CalendarParsedResult(summary, start, end, location, null, description);
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

}