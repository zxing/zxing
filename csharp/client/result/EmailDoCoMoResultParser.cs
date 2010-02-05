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
using System;
using Result = com.google.zxing.Result;
namespace com.google.zxing.client.result
{
	
	/// <summary> Implements the "MATMSG" email message entry format.
	/// 
	/// Supported keys: TO, SUB, BODY
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class EmailDoCoMoResultParser:AbstractDoCoMoResultParser
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'ATEXT_SYMBOLS'. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly char[] ATEXT_SYMBOLS = new char[]{'@', '.', '!', '#', '$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~'};
		
		public static EmailAddressParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			if (rawText == null || !rawText.StartsWith("MATMSG:"))
			{
				return null;
			}
			System.String[] rawTo = matchDoCoMoPrefixedField("TO:", rawText, true);
			if (rawTo == null)
			{
				return null;
			}
			System.String to = rawTo[0];
			if (!isBasicallyValidEmailAddress(to))
			{
				return null;
			}
			System.String subject = matchSingleDoCoMoPrefixedField("SUB:", rawText, false);
			System.String body = matchSingleDoCoMoPrefixedField("BODY:", rawText, false);
			return new EmailAddressParsedResult(to, subject, body, "mailto:" + to);
		}
		
		/// <summary> This implements only the most basic checking for an email address's validity -- that it contains
		/// an '@' contains no characters disallowed by RFC 2822. This is an overly lenient definition of
		/// validity. We want to generally be lenient here since this class is only intended to encapsulate what's
		/// in a barcode, not "judge" it.
		/// </summary>
		internal static bool isBasicallyValidEmailAddress(System.String email)
		{
			if (email == null)
			{
				return false;
			}
			bool atFound = false;
			for (int i = 0; i < email.Length; i++)
			{
				char c = email[i];
				if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && !isAtextSymbol(c))
				{
					return false;
				}
				if (c == '@')
				{
					if (atFound)
					{
						return false;
					}
					atFound = true;
				}
			}
			return atFound;
		}
		
		private static bool isAtextSymbol(char c)
		{
			for (int i = 0; i < ATEXT_SYMBOLS.Length; i++)
			{
				if (c == ATEXT_SYMBOLS[i])
				{
					return true;
				}
			}
			return false;
		}
	}
}