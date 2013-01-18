using System;
using System.Text;
using System.Text.RegularExpressions;

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

namespace com.google.zxing.client.result
{


	/// <summary>
	/// @author Sean Owen
	/// </summary>
	public sealed class CalendarParsedResult : ParsedResult
	{

	  private static readonly string RFC2445_DURATION = "P(?:(\\d+)W)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?";
	  private static readonly long[] RFC2445_DURATION_FIELD_UNITS = {7 * 24 * 60 * 60 * 1000L, 24 * 60 * 60 * 1000L, 60 * 60 * 1000L, 60 * 1000L, 1000L};

	  private static readonly string DATE_TIME = "[0-9]{8}(T[0-9]{6}Z?)?";

	  private static readonly string DATE_FORMAT = "yyyyMMdd";
	  static CalendarParsedResult()
	  {
		// For dates without a time, for purposes of interacting with Android, the resulting timestamp
		// needs to be midnight of that day in GMT. See:
		// http://code.google.com/p/android/issues/detail?id=8330
		string DATE_FORMAT_TimeZone = "GMT";
	  }
	  private static readonly string DATE_TIME_FORMAT = "yyyyMMdd'T'HHmmss";

	  private readonly string summary;
	  private readonly DateTime start;
	  private readonly bool startAllDay;
	  private readonly DateTime? end;
	  private readonly bool endAllDay;
	  private readonly string location;
	  private readonly string organizer;
	  private readonly string[] attendees;
	  private readonly string description;
	  private readonly double latitude;
	  private readonly double longitude;

	  public CalendarParsedResult(string summary, string startString, string endString, string durationString, string location, string organizer, string[] attendees, string description, double latitude, double longitude) : base(ParsedResultType.CALENDAR)
	  {
		this.summary = summary;

		try
		{
		  this.start = parseDate(startString);
		}
		catch (FormatException pe)
		{
		  throw new System.ArgumentException(pe.ToString());
		}

		if (endString == null)
		{
		  long durationMS = parseDurationMS(durationString);
		  end = durationMS < 0L ? null :(DateTime?) start.AddMilliseconds( durationMS);
		}
		else
		{
		  try
		  {
			this.end = parseDate(endString);
		  }
		  catch (FormatException pe)
		  {
			throw new System.ArgumentException(pe.ToString());
		  }
		}

		this.startAllDay = startString.Length == 8;
		this.endAllDay = endString != null && endString.Length == 8;

		this.location = location;
		this.organizer = organizer;
		this.attendees = attendees;
		this.description = description;
		this.latitude = latitude;
		this.longitude = longitude;
	  }

	  public string Summary
	  {
		  get
		  {
			return summary;
		  }
	  }

	  /// <returns> start time </returns>
	  public DateTime Start
	  {
		  get
		  {
			return start;
		  }
	  }

	  /// <returns> true if start time was specified as a whole day </returns>
	  public bool StartAllDay
	  {
		  get
		  {
			return startAllDay;
		  }
	  }

	  /// <summary>
	  /// May return null if the event has no duration. </summary>
	  /// <seealso cref= #getStart() </seealso>
	  public DateTime? End
	  {
		  get
		  {
			return end;
		  }
	  }

	  /// <returns> true if end time was specified as a whole day </returns>
	  public bool EndAllDay
	  {
		  get
		  {
			return endAllDay;
		  }
	  }

	  public string Location
	  {
		  get
		  {
			return location;
		  }
	  }

	  public string Organizer
	  {
		  get
		  {
			return organizer;
		  }
	  }

	  public string[] Attendees
	  {
		  get
		  {
			return attendees;
		  }
	  }

	  public string Description
	  {
		  get
		  {
			return description;
		  }
	  }

	  public double Latitude
	  {
		  get
		  {
			return latitude;
		  }
	  }

	  public double Longitude
	  {
		  get
		  {
			return longitude;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			StringBuilder result = new StringBuilder(100);
			maybeAppend(summary, result);
			maybeAppend(format(startAllDay, start), result);
			maybeAppend(format(endAllDay, end), result);
			maybeAppend(location, result);
			maybeAppend(organizer, result);
			maybeAppend(attendees, result);
			maybeAppend(description, result);
			return result.ToString();
		  }
	  }

	  /// <summary>
	  /// Parses a string as a date. RFC 2445 allows the start and end fields to be of type DATE (e.g. 20081021)
	  /// or DATE-TIME (e.g. 20081021T123000 for local time, or 20081021T123000Z for UTC).
	  /// </summary>
	  /// <param name="when"> The string to parse </param>
	  /// <exception cref="FormatException"> if not able to parse as a date </exception>
//JAVA TO C# CONVERTER WARNING: Method 'throws' clauses are not available in .NET:
//ORIGINAL LINE: private static java.util.Date parseDate(String when) throws java.text.ParseException
	  private static DateTime parseDate(string when)
	  {
		if (!(new Regex (DATE_TIME).IsMatch(when)))
		{
            throw FormatException.FormatInstance;
		}
		if (when.Length == 8)
		{
		  // Show only year/month/day
		  return DateTime.Parse(when);
		}
		else
		{
		  // The when string can be local time, or UTC if it ends with a Z
		  DateTime date;
		  if (when.Length == 16 && when[15] == 'Z')
		  {
			date = DateTime.Parse(when.Substring(0, 15));
            //DateTime calendar = new GregorianCalendar();
            //  System.Globalization.GregorianCalendar calendar = new System.Globalization.GregorianCalendar()
            //long milliseconds = date;
            //// Account for time zone difference
            //milliseconds += calendar.get(DateTime.ZONE_OFFSET);
            //// Might need to correct for daylight savings time, but use target time since
            //// now might be in DST but not then, or vice versa
            //calendar = new DateTime(milliseconds);
            //milliseconds += calendar.get(DateTime.DST_OFFSET);
            TimeSpan offset = TimeZone.CurrentTimeZone.GetUtcOffset(date);

            date = date + offset;
		  }
		  else
		  {
			date = DateTime.Parse(when);
		  }
		  return date;
		}
	  }

      private static string format(bool allDay, DateTime? date)
      {
          if (date == null)
          {
              return null;
          }
          //DateFormat format = allDay ? DateFormat.getDateInstance(DateFormat.MEDIUM) : DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
          //return format.format(date);
          if (allDay)
          {
              return String.Format("dd-MMM-yyyy",date);
          }
          else
          {
              return String.Format("dd-MMM-yyyy hh:mm:ss tt",date);
          }
      }

	  private static long parseDurationMS(string durationString)
	  {
		if (durationString == null)
		{
		  return -1L;
		}
        Regex m = new Regex(RFC2445_DURATION);
        Match match = m.Match(durationString);
		if (!match.Success)
		{
		  return -1L;
		}
		long durationMS = 0L;
		for (int i = 0; i < RFC2445_DURATION_FIELD_UNITS.Length; i++)
		{
		  string fieldValue = match.Groups[i + 1].Value;
		  if (fieldValue != null)
		  {
			durationMS += RFC2445_DURATION_FIELD_UNITS[i] * Convert.ToInt32(fieldValue);
		  }
		}
		return durationMS;
	  }

	}

}