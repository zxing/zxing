/*
 * Copyright 2007 ZXing authors
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tests {@link CalendarParsedResult}.
 *
 * @author Sean Owen
 */
public final class CalendarParsedResultTestCase extends Assert {

  private static final double EPSILON = 1.0E-10;

  private static DateFormat makeGMTFormat() {
    DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ENGLISH);
    format.setTimeZone(TimeZone.getTimeZone("GMT"));
    return format;
  }

  @Before
  public void setUp() {
    Locale.setDefault(Locale.ENGLISH);
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  }

  @Test
  public void testStartEnd() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        DTEND:20080505T234555Z
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", "20080505T234555Z");
  }

  @Test
  public void testNoVCalendar() {
    doTest(
        """
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        DTEND:20080505T234555Z
        END:VEVENT""",
        null, null, null, "20080504T123456Z", "20080505T234555Z");
  }

  @Test
  public void testStart() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", null);
  }

  @Test
  public void testDuration() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        DURATION:P1D
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", "20080505T123456Z");
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        DURATION:P1DT2H3M4S
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", "20080505T143800Z");
  }

  @Test
  public void testSummary() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        SUMMARY:foo
        DTSTART:20080504T123456Z
        END:VEVENT
        END:VCALENDAR""",
        null, "foo", null, "20080504T123456Z", null);
  }

  @Test
  public void testLocation() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        LOCATION:Miami
        DTSTART:20080504T123456Z
        END:VEVENT
        END:VCALENDAR""",
        null, null, "Miami", "20080504T123456Z", null);
  }

  @Test
  public void testDescription() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        DESCRIPTION:This is a test
        END:VEVENT
        END:VCALENDAR""",
        "This is a test", null, null, "20080504T123456Z", null);
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        DESCRIPTION:This is a test
               with a continuation
        END:VEVENT
        END:VCALENDAR""",
        "This is a test with a continuation", null, null, "20080504T123456Z", null);
  }

  @Test
  public void testGeo() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        GEO:-12.345;-45.678
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", null, null, null, -12.345, -45.678);
  }

  @Test
  public void testBadGeo() {
    // Not parsed as VEVENT
    Result fakeResult = new Result("""
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        GEO:-12.345
        END:VEVENT
        END:VCALENDAR""", null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.TEXT, result.getType());
  }

  @Test
  public void testOrganizer() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        ORGANIZER:mailto:bob@example.org
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", null, "bob@example.org", null, Double.NaN, Double.NaN);
  }

  @Test
  public void testAttendees() {
    doTest(
        """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        DTSTART:20080504T123456Z
        ATTENDEE:mailto:bob@example.org
        ATTENDEE:mailto:alice@example.org
        END:VEVENT
        END:VCALENDAR""",
        null, null, null, "20080504T123456Z", null, null,
        new String[] {"bob@example.org", "alice@example.org"}, Double.NaN, Double.NaN);
  }

  @Test
  public void testVEventEscapes() {
    doTest("""
           BEGIN:VEVENT
           CREATED:20111109T110351Z
           LAST-MODIFIED:20111109T170034Z
           DTSTAMP:20111109T170034Z
           UID:0f6d14ef-6cb7-4484-9080-61447ccdf9c2
           SUMMARY:Summary line
           CATEGORIES:Private
           DTSTART;TZID=Europe/Vienna:20111110T110000
           DTEND;TZID=Europe/Vienna:20111110T120000
           LOCATION:Location\\, with\\, escaped\\, commas
           DESCRIPTION:Meeting with a friend\\nlook at homepage first\\n\\n
             \\n
           SEQUENCE:1
           X-MOZ-GENERATION:1
           END:VEVENT""",
           "Meeting with a friend\nlook at homepage first\n\n\n  \n",
           "Summary line",
           "Location, with, escaped, commas",
           "20111110T110000Z",
           "20111110T120000Z");
  }

  @Test
  public void testAllDayValueDate() {
    doTest("""
           BEGIN:VEVENT
           DTSTART;VALUE=DATE:20111110
           DTEND;VALUE=DATE:20111110
           END:VEVENT""",
           null, null, null, "20111110T000000Z", "20111110T000000Z");
  }

  private static void doTest(String contents,
                             String description,
                             String summary,
                             String location,
                             String startString,
                             String endString) {
    doTest(contents, description, summary, location, startString, endString, null, null, Double.NaN, Double.NaN);
  }

  private static void doTest(String contents,
                             String description,
                             String summary,
                             String location,
                             String startString,
                             String endString,
                             String organizer,
                             String[] attendees,
                             double latitude,
                             double longitude) {
    Result fakeResult = new Result(contents, null, null, BarcodeFormat.QR_CODE);
    ParsedResult result = ResultParser.parseResult(fakeResult);
    assertSame(ParsedResultType.CALENDAR, result.getType());
    CalendarParsedResult calResult = (CalendarParsedResult) result;
    assertEquals(description, calResult.getDescription());
    assertEquals(summary, calResult.getSummary());
    assertEquals(location, calResult.getLocation());
    DateFormat dateFormat = makeGMTFormat();
    assertEquals(startString, dateFormat.format(calResult.getStartTimestamp()));
    assertEquals(endString, calResult.getEndTimestamp() < 0L ? null : dateFormat.format(calResult.getEndTimestamp()));
    assertEquals(organizer, calResult.getOrganizer());
    assertArrayEquals(attendees, calResult.getAttendees());
    assertEqualOrNaN(latitude, calResult.getLatitude());
    assertEqualOrNaN(longitude, calResult.getLongitude());
  }

  private static void assertEqualOrNaN(double expected, double actual) {
    if (Double.isNaN(expected)) {
      assertTrue(Double.isNaN(actual));
    } else {
      assertEquals(expected, actual, EPSILON);
    }
  }

}
