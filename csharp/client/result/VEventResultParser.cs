using System;
using System.Collections.Generic;

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

	using Result = com.google.zxing.Result;


	/// <summary>
	/// Partially implements the iCalendar format's "VEVENT" format for specifying a
	/// calendar event. See RFC 2445. This supports SUMMARY, LOCATION, GEO, DTSTART and DTEND fields.
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class VEventResultParser : ResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		int vEventStart = rawText.IndexOf("BEGIN:VEVENT");
		if (vEventStart < 0)
		{
		  return null;
		}

		string summary = matchSingleVCardPrefixedField("SUMMARY", rawText, true);
		string start = matchSingleVCardPrefixedField("DTSTART", rawText, true);
		if (start == null)
		{
		  return null;
		}
		string end = matchSingleVCardPrefixedField("DTEND", rawText, true);
		string duration = matchSingleVCardPrefixedField("DURATION", rawText, true);
		string location = matchSingleVCardPrefixedField("LOCATION", rawText, true);
		string organizer = stripMailto(matchSingleVCardPrefixedField("ORGANIZER", rawText, true));

		string[] attendees = matchVCardPrefixedField("ATTENDEE", rawText, true);
		if (attendees != null)
		{
		  for (int i = 0; i < attendees.Length; i++)
		  {
			attendees[i] = stripMailto(attendees[i]);
		  }
		}
		string description = matchSingleVCardPrefixedField("DESCRIPTION", rawText, true);

		string geoString = matchSingleVCardPrefixedField("GEO", rawText, true);
		double latitude;
		double longitude;
		if (geoString == null)
		{
		  latitude = double.NaN;
		  longitude = double.NaN;
		}
		else
		{
		  int semicolon = geoString.IndexOf(';');
		  try
		  {
			latitude = Convert.ToDouble(geoString.Substring(0, semicolon));
			longitude = Convert.ToDouble(geoString.Substring(semicolon + 1));
		  }
		  catch (FormatException nfe)
		  {
			return null;
		  }
		}

		try
		{
		  return new CalendarParsedResult(summary, start, end, duration, location, organizer, attendees, description, latitude, longitude);
		}
		catch (System.ArgumentException iae)
		{
		  return null;
		}
	  }

	  private static string matchSingleVCardPrefixedField(string prefix, string rawText, bool trim)
	  {
		IList<string> values = VCardResultParser.matchSingleVCardPrefixedField(prefix, rawText, trim, false);
		return values == null || values.Count == 0 ? null : values[0];
	  }

	  private static string[] matchVCardPrefixedField(string prefix, string rawText, bool trim)
	  {
		IList<IList<string>> values = VCardResultParser.matchVCardPrefixedField(prefix, rawText, trim, false);
		if (values == null || values.Count == 0)
		{
		  return null;
		}
		int size = values.Count;
		string[] result = new string[size];
		for (int i = 0; i < size; i++)
		{
		  result[i] = values[i][0];
		}
		return result;
	  }

	  private static string stripMailto(string s)
	  {
		if (s != null && (s.StartsWith("mailto:") || s.StartsWith("MAILTO:")))
		{
		  s = s.Substring(7);
		}
		return s;
	  }

	}
}