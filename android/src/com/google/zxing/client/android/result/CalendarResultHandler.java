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

import com.google.zxing.client.android.R;
import com.google.zxing.client.result.CalendarParsedResult;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Handles calendar entries encoded in QR Codes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CalendarResultHandler extends ResultHandler {

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
  private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

  private static final int[] buttons = {
      R.string.button_add_calendar
  };

  public CalendarResultHandler(Activity activity, ParsedResult result) {
    super(activity, result);
  }

  @Override
  public int getButtonCount() {
    return buttons.length;
  }

  @Override
  public int getButtonText(int index) {
    return buttons[index];
  }

  @Override
  public void handleButtonPress(int index) {
    CalendarParsedResult calendarResult = (CalendarParsedResult) getResult();
    if (index == 0) {
      addCalendarEvent(calendarResult.getSummary(),
                       calendarResult.getStart(),
                       calendarResult.getEnd(),
                       calendarResult.getLocation(),
                       calendarResult.getDescription());
    }
  }

  @Override
  public CharSequence getDisplayContents() {
    CalendarParsedResult calResult = (CalendarParsedResult) getResult();
    StringBuffer result = new StringBuffer(100);
    ParsedResult.maybeAppend(calResult.getSummary(), result);
    appendTime(calResult.getStart(), result);

    // The end can be null if the event has no duration, so use the start time.
    String endString = calResult.getEnd();
    if (endString == null) {
      endString = calResult.getStart();
    }
    appendTime(endString, result);

    ParsedResult.maybeAppend(calResult.getLocation(), result);
    ParsedResult.maybeAppend(calResult.getAttendee(), result);
    ParsedResult.maybeAppend(calResult.getDescription(), result);
    return result.toString();
  }

  private static void appendTime(String when, StringBuffer result) {
    if (when.length() == 8) {
      // Show only year/month/day
      Date date;
      synchronized (DATE_FORMAT) {
        date = DATE_FORMAT.parse(when, new ParsePosition(0));
      }
      ParsedResult.maybeAppend(DateFormat.getDateInstance().format(date.getTime()), result);
    } else {
      // The when string can be local time, or UTC if it ends with a Z
      Date date;
      synchronized (DATE_TIME_FORMAT) {
       date = DATE_TIME_FORMAT.parse(when.substring(0, 15), new ParsePosition(0));
      }
      long milliseconds = date.getTime();
      if (when.length() == 16 && when.charAt(15) == 'Z') {
        Calendar calendar = new GregorianCalendar();
        int offset = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));
        milliseconds += offset;
      }
      ParsedResult.maybeAppend(DateFormat.getDateTimeInstance().format(milliseconds), result);
    }
  }

  @Override
  public int getDisplayTitle() {
    return R.string.result_calendar;
  }
}
