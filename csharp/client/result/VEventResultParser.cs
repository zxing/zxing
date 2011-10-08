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
using System;
using Result = com.google.zxing.Result;
namespace com.google.zxing.client.result
{
	
	/// <summary> Partially implements the iCalendar format's "VEVENT" format for specifying a
	/// calendar event. See RFC 2445. This supports SUMMARY, DTSTART and DTEND fields.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class VEventResultParser:ResultParser
	{
		
		private VEventResultParser()
		{
		}
		
		public static CalendarParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			if (rawText == null)
			{
				return null;
			}
			int vEventStart = rawText.IndexOf("BEGIN:VEVENT");
			if (vEventStart < 0)
			{
				return null;
			}
			int vEventEnd = rawText.IndexOf("END:VEVENT");
			if (vEventEnd < 0)
			{
				return null;
			}
			
			System.String summary = VCardResultParser.matchSingleVCardPrefixedField("SUMMARY", rawText, true);
			System.String start = VCardResultParser.matchSingleVCardPrefixedField("DTSTART", rawText, true);
			System.String end = VCardResultParser.matchSingleVCardPrefixedField("DTEND", rawText, true);
			try
			{
				return new CalendarParsedResult(summary, start, end, null, null, null);
			}
			catch (System.ArgumentException)
			{
				return null;
			}
		}
	}
}
