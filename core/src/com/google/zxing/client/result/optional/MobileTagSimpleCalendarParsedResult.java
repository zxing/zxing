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
import com.google.zxing.client.result.ParsedReaderResultType;

/**
 * <p>Represents a "simple calendar" result encoded according to section 4.9 of the
 * MobileTag Reader International Specification.</p>
 *
 * @author srowen@google.com (Sean Owen)
 */
public final class MobileTagSimpleCalendarParsedResult extends AbstractMobileTagParsedResult {

  public static final String SERVICE_TYPE = "07";

  private final String summary;
  private final String start;
  private final String end;
  private final String location;
  private final String attendee;
  private final String title;

  private MobileTagSimpleCalendarParsedResult(String summary,
                                              String start,
                                              String end,
                                              String location,
                                              String attendee,
                                              String title) {
    super(ParsedReaderResultType.MOBILETAG_SIMPLE_CALENDAR);
    this.summary = summary;
    this.start = start;
    this.end = end;
    this.location = location;
    this.attendee = attendee;
    this.title = title;
  }

  public static MobileTagSimpleCalendarParsedResult parse(Result result) {
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

    return new MobileTagSimpleCalendarParsedResult(summary, start, end, location, attendee, title);
  }

  public String getSummary() {
    return summary;
  }

  /**
   * <p>We would return the start and end date as a {@link java.util.Date} except that this code
   * needs to work under JavaME / MIDP and there is no date parsing library available there, such
   * as <code>java.text.SimpleDateFormat</code>.</p>
   *
   * <p>However we do translate the date from its encoded format of, say, "0602212156" to its full
   * text representation of "20060221T215600Z", per the specification.</p>
   */
  public String getStart() {
    return start;
  }

  /**
   * @see #getStart()
   */
  public String getEnd() {
    return end;
  }

  public String getLocation() {
    return location;
  }

  public String getAttendee() {
    return attendee;
  }

  public String getTitle() {
    return title;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(summary);
    maybeAppend(start, result);
    maybeAppend(end, result);
    maybeAppend(location, result);
    maybeAppend(attendee, result);
    maybeAppend(title, result);
    return result.toString();
  }

  private static String expandDateString(String date) {
    if (date == null) {
      return null;
    }
    // Input is of form YYMMddHHmmss, and needs to be YYYYMMdd'T'HHmmss'Z'
    return "20" + date.substring(0, 6) + 'T' + date.substring(6) + "00Z";
  }

}