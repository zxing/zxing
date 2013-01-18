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
	/// <p>Parses an "sms:" URI result, which specifies a number to SMS.
	/// See <a href="http://tools.ietf.org/html/rfc5724"> RFC 5724</a> on this.</p>
	/// 
	/// <p>This class supports "via" syntax for numbers, which is not part of the spec.
	/// For example "+12125551212;via=+12124440101" may appear as a number.
	/// It also supports a "subject" query parameter, which is not mentioned in the spec.
	/// These are included since they were mentioned in earlier IETF drafts and might be
	/// used.</p>
	/// 
	/// <p>This actually also parses URIs starting with "mms:" and treats them all the same way,
	/// and effectively converts them to an "sms:" URI for purposes of forwarding to the platform.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class SMSMMSResultParser : ResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		if (!(rawText.StartsWith("sms:") || rawText.StartsWith("SMS:") || rawText.StartsWith("mms:") || rawText.StartsWith("MMS:")))
		{
		  return null;
		}

		// Check up front if this is a URI syntax string with query arguments
		IDictionary<string, string> nameValuePairs = parseNameValuePairs(rawText);
		string subject = null;
		string body = null;
		bool querySyntax = false;
		if (nameValuePairs != null && nameValuePairs.Count > 0)
		{
		  subject = nameValuePairs["subject"];
		  body = nameValuePairs["body"];
		  querySyntax = true;
		}

		// Drop sms, query portion
		int queryStart = rawText.IndexOf('?', 4);
		string smsURIWithoutQuery;
		// If it's not query syntax, the question mark is part of the subject or message
		if (queryStart < 0 || !querySyntax)
		{
		  smsURIWithoutQuery = rawText.Substring(4);
		}
		else
		{
		  smsURIWithoutQuery = rawText.Substring(4, queryStart - 4);
		}

		int lastComma = -1;
		int comma;
		List<string> numbers = new List<string>(1);
		List<string> vias = new List<string>(1);
		while ((comma = smsURIWithoutQuery.IndexOf(',', lastComma + 1)) > lastComma)
		{
		  string numberPart = smsURIWithoutQuery.Substring(lastComma + 1, comma - (lastComma + 1));
		  addNumberVia(numbers, vias, numberPart);
		  lastComma = comma;
		}
		addNumberVia(numbers, vias, smsURIWithoutQuery.Substring(lastComma + 1));

		return new SMSParsedResult(numbers.ToArray(), vias.ToArray(), subject, body);
	  }

	  private static void addNumberVia(ICollection<string> numbers, ICollection<string> vias, string numberPart)
	  {
		int numberEnd = numberPart.IndexOf(';');
		if (numberEnd < 0)
		{
		  numbers.Add(numberPart);
		  vias.Add(null);
		}
		else
		{
		  numbers.Add(numberPart.Substring(0, numberEnd));
		  string maybeVia = numberPart.Substring(numberEnd + 1);
		  string via;
		  if (maybeVia.StartsWith("via="))
		  {
			via = maybeVia.Substring(4);
		  }
		  else
		  {
			via = null;
		  }
		  vias.Add(via);
		}
	  }

	}
}