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
 * @author srowen@google.com (Sean Owen)
 */
public final class CalendarParsedResult extends ParsedResult {

  private final String summary;
  private final String start;
  private final String end;
  private final String location;
  private final String attendee;
  private final String title;

  public CalendarParsedResult(String summary,
                              String start,
                              String end,
                              String location,
                              String attendee,
                              String title) {
    super(ParsedResultType.CALENDAR);
    this.summary = summary;
    this.start = start;
    this.end = end;
    this.location = location;
    this.attendee = attendee;
    this.title = title;
  }

  public String getSummary() {
    return summary;
  }

  /**
   * <p>We would return the start and end date as a {@link java.util.Date} except that this code
   * needs to work under JavaME / MIDP and there is no date parsing library available there, such
   * as <code>java.text.SimpleDateFormat</code>.</p>
   *
   * <p>Instead this is a String formatted as YYYYMMdd'T'HHmmss'Z'.</p>
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

}