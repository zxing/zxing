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

package com.google.zxing.web.generator.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;

import java.util.Date;

/**
 * A Generator for calendar events.
 * 
 * @author Yohann Coppel
 */
public final class CalendarEventGenerator implements GeneratorSource {

  private static final String[] FULL_DAY_ONLY_IDS =
      { "fullDayOnlyInfo1", "fullDayOnlyInfo2", "fullDayOnlyInfo3", "fullDayOnlyInfo4" };
  private static final long ONE_HOUR = 60L * 60 * 1000;

  private Grid table;
  private final TextBox eventName = new TextBox();
  private final CheckBox fullDay = new CheckBox();
  private final DatePicker datePicker1 = new DatePicker();
  private final DatePicker datePicker2 = new DatePicker();
  private final TextBox timePicker1 = new TextBox();
  private final TextBox timePicker2 = new TextBox();
  private final CheckBox summerTime = new CheckBox();
  private final ListBox timeZones = new ListBox();
  private Date timePicker1PreviousDate;
  private final TextBox location = new TextBox();
  private final TextBox description = new TextBox();

  public CalendarEventGenerator(final ChangeHandler handler, KeyPressHandler keyListener) {
    eventName.addStyleName(StylesDefs.INPUT_FIELD_REQUIRED);
    eventName.addChangeHandler(handler);
    eventName.addKeyPressHandler(keyListener);
    setDateToTextBox(timePicker1, new Date());
    try {
      setDateToTextBox(timePicker2, addMilliseconds(getDateFromTextBox(timePicker1), ONE_HOUR));
      timePicker1PreviousDate = getDateFromTextBox(timePicker1);
    } catch (GeneratorException ge) {
      throw new IllegalStateException(ge);
    }

    buildTimeZoneList();
    timeZones.setSelectedIndex(25);
    timeZones.addKeyPressHandler(keyListener);
    timePicker1.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> valueChangeEvent) {
        Date time = timePicker1PreviousDate;
        Date time1;
        Date time2;
        try {
          time1 = getDateFromTextBox(timePicker1);
          time2 = getDateFromTextBox(timePicker2);
        } catch (GeneratorException e) {
          return;
        }
        if (time2.after(time)) {
          // keep the same time difference if the interval is valid.
          long diff = time2.getTime() - time.getTime();
          setDateToTextBox(timePicker2, addMilliseconds(time1, diff));
        } else {
          // otherwise erase the end date and set it to startdate + one hour.
          setDateToTextBox(timePicker2, addMilliseconds(time1, ONE_HOUR));
        }
        // no need to call onChange for timePicker1, since it will be called
        // for timePicker2 when changes are made.
        // listener.onChange(timePicker1);
        timePicker1PreviousDate = time1;
      }
    });
    timePicker2.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> valueChangeEvent) {
        // Hack to stitch together these old and new APIs
        ChangeEvent event = new ChangeEvent() {
          @Override
          public Object getSource() {
            return timePicker2;
          }
        };
        handler.onChange(event);
      }
    });
  }

  private void buildTimeZoneList() {
    for (TimeZoneInfo info : TimeZoneList.TIMEZONES) {
      timeZones.addItem(info.getGmtRelative() + ' ' + info.getAbbreviation(),
                        String.valueOf(info.getGmtDiff()));
    }
  }

  @Override
  public String getName() {
    return "Calendar event";
  }

  @Override
  public Grid getWidget() {
    if (table != null) {
      return table;
    }
    datePicker1.setValue(new Date());
    datePicker2.setValue(new Date());
    table = new Grid(10, 2);

    table.setText(0, 0, "All day event");
    table.setWidget(0, 1, fullDay);

    table.setText(1, 0, "Event title");
    table.setWidget(1, 1, eventName);

    table.setText(2, 0, "Start date");
    table.setWidget(2, 1, datePicker1);

    table.setText(3, 0, "Time");
    table.setWidget(3, 1, timePicker1);

    table.setText(4, 0, "End date");
    table.setWidget(4, 1, datePicker2);

    table.setText(5, 0, "Time");
    table.setWidget(5, 1, timePicker2);

    table.setText(6, 0, "Time zone");
    table.setWidget(6, 1, timeZones);

    table.setText(7, 0, "Daylight savings");
    table.setWidget(7, 1, summerTime);

    table.setText(8, 0, "Location");
    table.setWidget(8, 1, location);

    table.setText(9, 0, "Description");
    table.setWidget(9, 1, description);

    table.getRowFormatter().getElement(3).setId(FULL_DAY_ONLY_IDS[0]);
    table.getRowFormatter().getElement(5).setId(FULL_DAY_ONLY_IDS[1]);
    table.getRowFormatter().getElement(6).setId(FULL_DAY_ONLY_IDS[2]);
    table.getRowFormatter().getElement(7).setId(FULL_DAY_ONLY_IDS[3]);

    fullDay.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        CheckBox cb = (CheckBox) event.getSource();
        for (String s : FULL_DAY_ONLY_IDS) {
          Element element = DOM.getElementById(s);
          String style = cb.getValue() ? "none" : "";
          element.getStyle().setProperty("display", style);
        }
      }
    });

    return table;
  }

  @Override
  public String getText() throws GeneratorException {
    String eventName = getEventNameField();
    String dates = getDateTimeFields();
    String location = getLocationField();
    String description = getDescriptionField();
    return "BEGIN:VEVENT\r\n" + eventName + dates + location + description + "END:VEVENT\r\n";
  }

  private String getEventNameField() throws GeneratorException {
    String inputName = eventName.getText();
    if (inputName.isEmpty()) {
      throw new GeneratorException("Event name must be at least 1 character.");
    }
    if (inputName.contains("\n")) {
      throw new GeneratorException("Event name should not contain \\n characters.");
    }
    return "SUMMARY:" + inputName + "\r\n";
  }

  private String getDateTimeFields() throws GeneratorException {
    if (fullDay.getValue()) {
      return getFullDayDateFields();
    }
    return getDateTimeValues();
  }

  private String getLocationField() throws GeneratorException {
    String locationString = location.getText();
    if (locationString.isEmpty()) {
      return "";
    }
    if (locationString.contains("\n")) {
      throw new GeneratorException(
          "Location should not contain \\n characters.");
    }
    return "LOCATION:" + locationString + "\r\n";
  }

  private String getDescriptionField() throws GeneratorException {
    String descriptionString = description.getText();
    if (descriptionString.isEmpty()) {
      return "";
    }
    if (descriptionString.contains("\n")) {
      throw new GeneratorException(
          "Description should not contain \\n characters.");
    }
    return "DESCRIPTION:" + descriptionString + "\r\n";
  }

  private String getFullDayDateFields() throws GeneratorException {
    Date date1 = datePicker1.getValue();
    Date date2 = datePicker2.getValue();
    if (date1 == null || date2 == null) {
      throw new GeneratorException("Start and end dates must be set.");
    }
    if (date1.after(date2)) {
      throw new GeneratorException("End date cannot be before start date.");
    }
    // Specify end date as +1 day since it's exclusive
    Date date2PlusDay = new Date(date2.getTime() + 24 * 60 * 60 * 1000);
    DateTimeFormat isoFormatter = DateTimeFormat.getFormat("yyyyMMdd");
    return "DTSTART;VALUE=DATE:" + isoFormatter.format(date1) + "\r\n" +
        "DTEND;VALUE=DATE:" + isoFormatter.format(date2PlusDay) + "\r\n";
  }

  private String getDateTimeValues() throws GeneratorException {
    Date date1 = datePicker1.getValue();
    Date date2 = datePicker2.getValue();
    Date time1 = getDateFromTextBox(timePicker1);
    Date time2 = getDateFromTextBox(timePicker2);
    if (date1 == null || date2 == null || time1 == null || time2 == null) {
      throw new GeneratorException("Start and end dates/times must be set.");
    }
    String timezoneDelta = timeZones.getValue(timeZones.getSelectedIndex());
    long diffTimeZone = Long.parseLong(timezoneDelta);
    if (summerTime.getValue()) {
      diffTimeZone += ONE_HOUR;
    }
    Date dateTime1 = addMilliseconds(mergeDateAndTime(date1, time1), -diffTimeZone);
    Date dateTime2 = addMilliseconds(mergeDateAndTime(date2, time2), -diffTimeZone);
    if (dateTime1.after(dateTime2)) {
      throw new GeneratorException("Ending date/time cannot be before starting date/time.");
    }
    DateTimeFormat isoFormatter = DateTimeFormat.getFormat("yyyyMMdd'T'HHmmss'Z'");
    return "DTSTART:" + isoFormatter.format(dateTime1) + "\r\n" +
        "DTEND:" + isoFormatter.format(dateTime2) + "\r\n";
  }

  private static Date mergeDateAndTime(Date date, Date time) {
    // Is that the only ugly way to do with GWT ? given that we don't
    // have java.util.Calendar for instance
    DateTimeFormat extractDate = DateTimeFormat.getFormat("yyyyMMdd");
    DateTimeFormat extractTime = DateTimeFormat.getFormat("HHmm");
    DateTimeFormat merger = DateTimeFormat.getFormat("yyyyMMddHHmmss");
    String d = extractDate.format(date);
    String t = extractTime.format(time) + "00";
    return merger.parse(d + t);
  }

  @Override
  public void validate(Widget widget) throws GeneratorException {
    if (widget == eventName) {
      getEventNameField();
    } else if (widget == datePicker1 || widget == timePicker1 || widget == datePicker2 || widget == timePicker2) {
      getDateTimeFields();
    }
  }

  private static Date addMilliseconds(Date time1, long milliseconds) {
    return new Date(time1.getTime() + milliseconds);
  }

  private static Date getDateFromTextBox(HasText textBox) throws GeneratorException {
    DateTimeFormat extractTime = DateTimeFormat.getFormat("HHmm");
    try {
      return extractTime.parseStrict(textBox.getText());
    } catch (IllegalArgumentException iae) {
      throw new GeneratorException("Invalid time");
    }
  }

  private static void setDateToTextBox(HasText textBox, Date date) {
    DateTimeFormat extractTime = DateTimeFormat.getFormat("HHmm");
    textBox.setText(extractTime.format(date));
  }

  @Override
  public void setFocus() {
    eventName.setFocus(true);
  }
}
