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

import android.content.ActivityNotFoundException;
import android.util.Log;
import com.google.zxing.client.android.R;
import com.google.zxing.client.result.CalendarParsedResult;
import com.google.zxing.client.result.ParsedResult;

import android.app.Activity;
import android.content.Intent;

import java.text.DateFormat;

/**
 * Handles calendar entries encoded in QR Codes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CalendarResultHandler extends ResultHandler {

  private static final String TAG = CalendarResultHandler.class.getSimpleName();

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
    if (index == 0) {
      CalendarParsedResult calendarResult = (CalendarParsedResult) getResult();

      String description = calendarResult.getDescription();
      String organizer = calendarResult.getOrganizer();
      if (organizer != null) { // No separate Intent key, put in description
        if (description == null) {
          description = organizer;
        } else {
          description = description + '\n' + organizer;
        }
      }

      addCalendarEvent(calendarResult.getSummary(),
                       calendarResult.getStartTimestamp(),
                       calendarResult.isStartAllDay(),
                       calendarResult.getEndTimestamp(),
                       calendarResult.getLocation(),
                       description,
                       calendarResult.getAttendees());
    }
  }

  /**
   * Sends an intent to create a new calendar event by prepopulating the Add Event UI. Older
   * versions of the system have a bug where the event title will not be filled out.
   *
   * @param summary A description of the event
   * @param start   The start time
   * @param allDay  if true, event is considered to be all day starting from start time
   * @param end     The end time (optional; can be < 0 if not specified)
   * @param location a text description of the event location
   * @param description a text description of the event itself
   * @param attendees attendees to invite
   */
  private void addCalendarEvent(String summary,
                                long start,
                                boolean allDay,
                                long end,
                                String location,
                                String description,
                                String[] attendees) {
    Intent intent = new Intent(Intent.ACTION_INSERT);
    intent.setType("vnd.android.cursor.item/event");
    intent.putExtra("beginTime", start);
    if (allDay) {
      intent.putExtra("allDay", true);
    }
    if (end < 0L) {
      if (allDay) {
        // + 1 day
        end = start + 24 * 60 * 60 * 1000;
      } else {
        end = start;
      }
    }
    intent.putExtra("endTime", end);
    intent.putExtra("title", summary);
    intent.putExtra("eventLocation", location);
    intent.putExtra("description", description);
    if (attendees != null) {
      intent.putExtra(Intent.EXTRA_EMAIL, attendees);
      // Documentation says this is either a String[] or comma-separated String, which is right?
    }

    try {
      // Do this manually at first
      rawLaunchIntent(intent);
    } catch (ActivityNotFoundException anfe) {
      Log.w(TAG, "No calendar app available that responds to " + Intent.ACTION_INSERT);
      // For calendar apps that don't like "INSERT":
      intent.setAction(Intent.ACTION_EDIT);
      launchIntent(intent); // Fail here for real if nothing can handle it
    }
  }


  @Override
  public CharSequence getDisplayContents() {

    CalendarParsedResult calResult = (CalendarParsedResult) getResult();
    StringBuilder result = new StringBuilder(100);

    ParsedResult.maybeAppend(calResult.getSummary(), result);

    long start = calResult.getStartTimestamp();
    ParsedResult.maybeAppend(format(calResult.isStartAllDay(), start), result);

    long end = calResult.getEndTimestamp();
    if (end >= 0L) {
      if (calResult.isEndAllDay() && start != end) {
        // Show only year/month/day
        // if it's all-day and this is the end date, it's exclusive, so show the user
        // that it ends on the day before to make more intuitive sense.
        // But don't do it if the event already (incorrectly?) specifies the same start/end
        end -= 24 * 60 * 60 * 1000;
      }
      ParsedResult.maybeAppend(format(calResult.isEndAllDay(), end), result);
    }

    ParsedResult.maybeAppend(calResult.getLocation(), result);
    ParsedResult.maybeAppend(calResult.getOrganizer(), result);
    ParsedResult.maybeAppend(calResult.getAttendees(), result);
    ParsedResult.maybeAppend(calResult.getDescription(), result);
    return result.toString();
  }

  private static String format(boolean allDay, long date) {
    if (date < 0L) {
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
