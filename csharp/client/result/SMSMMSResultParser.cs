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
	
	/// <summary> <p>Parses an "sms:" URI result, which specifies a number to SMS and optional
	/// "via" number. See <a href="http://gbiv.com/protocols/uri/drafts/draft-antti-gsm-sms-url-04.txt">
	/// the IETF draft</a> on this.</p>
	/// 
	/// <p>This actually also parses URIs starting with "mms:", "smsto:", "mmsto:", "SMSTO:", and
	/// "MMSTO:", and treats them all the same way, and effectively converts them to an "sms:" URI
	/// for purposes of forwarding to the platform.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class SMSMMSResultParser:ResultParser
	{
		
		private SMSMMSResultParser()
		{
		}
		
		public static SMSParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			if (rawText == null)
			{
				return null;
			}
			int prefixLength;
			if (rawText.StartsWith("sms:") || rawText.StartsWith("SMS:") || rawText.StartsWith("mms:") || rawText.StartsWith("MMS:"))
			{
				prefixLength = 4;
			}
			else if (rawText.StartsWith("smsto:") || rawText.StartsWith("SMSTO:") || rawText.StartsWith("mmsto:") || rawText.StartsWith("MMSTO:"))
			{
				prefixLength = 6;
			}
			else
			{
				return null;
			}
			
			// Check up front if this is a URI syntax string with query arguments
			System.Collections.Hashtable nameValuePairs = parseNameValuePairs(rawText);
			System.String subject = null;
			System.String body = null;
			bool querySyntax = false;
			if (nameValuePairs != null && !(nameValuePairs.Count == 0))
			{
				subject = ((System.String) nameValuePairs["subject"]);
				body = ((System.String) nameValuePairs["body"]);
				querySyntax = true;
			}
			
			// Drop sms, query portion
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			int queryStart = rawText.IndexOf('?', prefixLength);
			System.String smsURIWithoutQuery;
			// If it's not query syntax, the question mark is part of the subject or message
			if (queryStart < 0 || !querySyntax)
			{
				smsURIWithoutQuery = rawText.Substring(prefixLength);
			}
			else
			{
				smsURIWithoutQuery = rawText.Substring(prefixLength, (queryStart) - (prefixLength));
			}
			int numberEnd = smsURIWithoutQuery.IndexOf(';');
			System.String number;
			System.String via;
			if (numberEnd < 0)
			{
				number = smsURIWithoutQuery;
				via = null;
			}
			else
			{
				number = smsURIWithoutQuery.Substring(0, (numberEnd) - (0));
				System.String maybeVia = smsURIWithoutQuery.Substring(numberEnd + 1);
				if (maybeVia.StartsWith("via="))
				{
					via = maybeVia.Substring(4);
				}
				else
				{
					via = null;
				}
			}
			
			// Thanks to dominik.wild for suggesting this enhancement to support
			// smsto:number:body URIs
			if (body == null)
			{
				int bodyStart = number.IndexOf(':');
				if (bodyStart >= 0)
				{
					body = number.Substring(bodyStart + 1);
					number = number.Substring(0, (bodyStart) - (0));
				}
			}
			return new SMSParsedResult("sms:" + number, number, via, subject, body, null);
		}
	}
}