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
import java.util.Date;

/**
 * Handles calendar entries encoded in QR Codes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CalendarResultHandler extends ResultHandler {

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
                       calendarResult.isStartAllDay(),
                       calendarResult.getEnd(),
                       calendarResult.getLocation(),
                       calendarResult.getDescription());
    }
  }

  @Override
  public CharSequence getDisplayContents() {

    CalendarParsedResult calResult = (CalendarParsedResult) getResult();
    StringBuilder result = new StringBuilder(100);

    ParsedResult.maybeAppend(calResult.getSummary(), result);

    Date start = calResult.getStart();
    ParsedResult.maybeAppend(format(calResult.isStartAllDay(), start), result);

    Date end = calResult.getEnd();
    if (end != null) {
      if (calResult.isEndAllDay() && !start.equals(end)) {
        // Show only year/month/day
        // if it's all-day and this is the end date, it's exclusive, so show the user
        // that it ends on the day before to make more intuitive sense.
        // But don't do it if the event already (incorrectly?) specifies the same start/end
        end = new Date(end.getTime() - 24 * 60 * 60 * 1000);
      }
      ParsedResult.maybeAppend(format(calResult.isEndAllDay(), end), result);
    }

    ParsedResult.maybeAppend(calResult.getLocation(), result);
    ParsedResult.maybeAppend(calResult.getAttendee(), result);
    ParsedResult.maybeAppend(calResult.getDescription(), result);
    return result.toString();
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

  @Override
  public int getDisplayTitle() {
    return R.string.result_calendar;
  }
}
