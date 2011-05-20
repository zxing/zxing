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

/**
 * @author Sean Owen
 */
public final class CalendarParsedResult extends ParsedResult {

  private final String summary;
  private final String start;
  private final String end;
  private final String location;
  private final String attendee;
  private final String description;
  private final double latitude;
  private final double longitude;

  public CalendarParsedResult(String summary,
                              String start,
                              String end,
                              String location,
                              String attendee,
                              String description) {
    this(summary, start, end, location, attendee, description, Double.NaN, Double.NaN);
  }

  public CalendarParsedResult(String summary,
                              String start,
                              String end,
                              String location,
                              String attendee,
                              String description,
                              double latitude,
                              double longitude) {
    super(ParsedResultType.CALENDAR);
    // Start is required, end is not
    if (start == null) {
      throw new IllegalArgumentException();
    }
    validateDate(start);
    if (end == null) {
      end = start;
    } else {
      validateDate(end);
    }
    this.summary = summary;
    this.start = start;
    this.end = end;
    this.location = location;
    this.attendee = attendee;
    this.description = description;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getSummary() {
    return summary;
  }

  /**
   * <p>We would return the start and end date as a {@link java.util.Date} except that this code
   * needs to work under JavaME / MIDP and there is no date parsing library available there, such
   * as <code>java.text.SimpleDateFormat</code>.</p> See validateDate() for the return format.
   *
   * @return start time formatted as a RFC 2445 DATE or DATE-TIME.</p>
   */
  public String getStart() {
    return start;
  }

  /**
   * @see #getStart(). May return null if the event has no duration.
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

  public String getDescription() {
    return description;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public String getDisplayResult() {
    StringBuffer result = new StringBuffer(100);
    maybeAppend(summary, result);
    maybeAppend(start, result);
    maybeAppend(end, result);
    maybeAppend(location, result);
    maybeAppend(attendee, result);
    maybeAppend(description, result);
    return result.toString();
  }

  /**
   * RFC 2445 allows the start and end fields to be of type DATE (e.g. 20081021) or DATE-TIME
   * (e.g. 20081021T123000 for local time, or 20081021T123000Z for UTC).
   *
   * @param date The string to validate
   */
  private static void validateDate(String date) {
    if (date != null) {
      int length = date.length();
      if (length != 8 && length != 15 && length != 16) {
        throw new IllegalArgumentException();
      }
      for (int i = 0; i < 8; i++) {
        if (!Character.isDigit(date.charAt(i))) {
          throw new IllegalArgumentException();
        }
      }
      if (length > 8) {
        if (date.charAt(8) != 'T') {
          throw new IllegalArgumentException();
        }
        for (int i = 9; i < 15; i++) {
          if (!Character.isDigit(date.charAt(i))) {
            throw new IllegalArgumentException();
          }
        }
        if (length == 16 && date.charAt(15) != 'Z') {
          throw new IllegalArgumentException();
        }
      }
    }
  }

}
