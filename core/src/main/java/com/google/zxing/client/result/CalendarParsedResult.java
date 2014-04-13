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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sean Owen
 */
public final class CalendarParsedResult extends ParsedResult {

  private static final Pattern RFC2445_DURATION =
      Pattern.compile("P(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?");
  private static final long[] RFC2445_DURATION_FIELD_UNITS = {
      7 * 24 * 60 * 60 * 1000L, // 1 week
      24 * 60 * 60 * 1000L, // 1 day
      60 * 60 * 1000L, // 1 hour
      60 * 1000L, // 1 minute
      1000L, // 1 second
  };

  private static final Pattern DATE_TIME = Pattern.compile("[0-9]{8}(T[0-9]{6}Z?)?");

  private final String summary;
  private final Date start;
  private final boolean startAllDay;
  private final Date end;
  private final boolean endAllDay;
  private final String location;
  private final String organizer;
  private final String[] attendees;
  private final String description;
  private final double latitude;
  private final double longitude;

  public CalendarParsedResult(String summary,
                              String startString,
                              String endString,
                              String durationString,
                              String location,
                              String organizer,
                              String[] attendees,
                              String description,
                              double latitude,
                              double longitude) {
    super(ParsedResultType.CALENDAR);
    this.summary = summary;

    try {
      this.start = parseDate(startString);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(pe.toString());
    }

    if (endString == null) {
      long durationMS = parseDurationMS(durationString);
      end = durationMS < 0L ? null : new Date(start.getTime() + durationMS);
    } else {
      try {
        this.end = parseDate(endString);
      } catch (ParseException pe) {
        throw new IllegalArgumentException(pe.toString());
      }
    }

    this.startAllDay = startString.length() == 8;
    this.endAllDay = endString != null && endString.length() == 8;

    this.location = location;
    this.organizer = organizer;
    this.attendees = attendees;
    this.description = description;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getSummary() {
    return summary;
  }

  /**
   * @return start time
   */
  public Date getStart() {
    return start;
  }

  /**
   * @return true if start time was specified as a whole day
   */
  public boolean isStartAllDay() {
    return startAllDay;
  }

  /**
   * @return event end {@link Date}, or {@code null} if event has no duration
   * @see #getStart()
   */
  public Date getEnd() {
    return end;
  }

  /**
   * @return true if end time was specified as a whole day
   */
  public boolean isEndAllDay() {
    return endAllDay;
  }

  public String getLocation() {
    return location;
  }

  public String getOrganizer() {
    return organizer;
  }

  public String[] getAttendees() {
    return attendees;
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

  @Override
  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(100);
    maybeAppend(summary, result);
    maybeAppend(format(startAllDay, start), result);
    maybeAppend(format(endAllDay, end), result);
    maybeAppend(location, result);
    maybeAppend(organizer, result);
    maybeAppend(attendees, result);
    maybeAppend(description, result);
    return result.toString();
  }

  /**
   * Parses a string as a date. RFC 2445 allows the start and end fields to be of type DATE (e.g. 20081021)
   * or DATE-TIME (e.g. 20081021T123000 for local time, or 20081021T123000Z for UTC).
   *
   * @param when The string to parse
   * @throws ParseException if not able to parse as a date
   */
  private static Date parseDate(String when) throws ParseException {
    if (!DATE_TIME.matcher(when).matches()) {
      throw new ParseException(when, 0);
    }
    if (when.length() == 8) {
      // Show only year/month/day
      return buildDateFormat().parse(when);
    } else {
      // The when string can be local time, or UTC if it ends with a Z
      Date date;
      if (when.length() == 16 && when.charAt(15) == 'Z') {
        date = buildDateTimeFormat().parse(when.substring(0, 15));
        Calendar calendar = new GregorianCalendar();
        long milliseconds = date.getTime();
        // Account for time zone difference
        milliseconds += calendar.get(Calendar.ZONE_OFFSET);
        // Might need to correct for daylight savings time, but use target time since
        // now might be in DST but not then, or vice versa
        calendar.setTime(new Date(milliseconds));
        milliseconds += calendar.get(Calendar.DST_OFFSET);
        date = new Date(milliseconds);
      } else {
        date = buildDateTimeFormat().parse(when);
      }
      return date;
    }
  }

  private static String format(boolean allDay, Date date) {
    if (date == null) {
      return null;
    }
    DateFormat format = allDay
        ? DateFormat.getDateInstance(DateFormat.MEDIUM)
        : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    return format.format(date);
  }

  private static long parseDurationMS(CharSequence durationString) {
    if (durationString == null) {
      return -1L;
    }
    Matcher m = RFC2445_DURATION.matcher(durationString);
    if (!m.matches()) {
      return -1L;
    }
    long durationMS = 0L;
    for (int i = 0; i < RFC2445_DURATION_FIELD_UNITS.length; i++) {
      String fieldValue = m.group(i + 1);
      if (fieldValue != null) {
        durationMS += RFC2445_DURATION_FIELD_UNITS[i] * Integer.parseInt(fieldValue);
      }
    }
    return durationMS;
  }

  private static DateFormat buildDateFormat() {
    DateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    // For dates without a time, for purposes of interacting with Android, the resulting timestamp
    // needs to be midnight of that day in GMT. See:
    // http://code.google.com/p/android/issues/detail?id=8330
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    return format;
  }

  private static DateFormat buildDateTimeFormat() {
    return new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.ENGLISH);
  }

}
