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
	
	/// <summary> Parses contact information formatted according to the VCard (2.1) format. This is not a complete
	/// implementation but should parse information as commonly encoded in 2D barcodes.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class VCardResultParser:ResultParser
	{
		
		private VCardResultParser()
		{
		}
		
		public static AddressBookParsedResult parse(Result result)
		{
			// Although we should insist on the raw text ending with "END:VCARD", there's no reason
			// to throw out everything else we parsed just because this was omitted. In fact, Eclair
			// is doing just that, and we can't parse its contacts without this leniency.
			System.String rawText = result.Text;
			if (rawText == null || !rawText.StartsWith("BEGIN:VCARD"))
			{
				return null;
			}
			System.String[] names = matchVCardPrefixedField("FN", rawText, true);
			if (names == null)
			{
				// If no display names found, look for regular name fields and format them
				names = matchVCardPrefixedField("N", rawText, true);
				formatNames(names);
			}
			System.String[] phoneNumbers = matchVCardPrefixedField("TEL", rawText, true);
			System.String[] emails = matchVCardPrefixedField("EMAIL", rawText, true);
			System.String note = matchSingleVCardPrefixedField("NOTE", rawText, false);
			System.String[] addresses = matchVCardPrefixedField("ADR", rawText, true);
			if (addresses != null)
			{
				for (int i = 0; i < addresses.Length; i++)
				{
					addresses[i] = formatAddress(addresses[i]);
				}
			}
			System.String org = matchSingleVCardPrefixedField("ORG", rawText, true);
			System.String birthday = matchSingleVCardPrefixedField("BDAY", rawText, true);
			if (!isLikeVCardDate(birthday))
			{
				birthday = null;
			}
			System.String title = matchSingleVCardPrefixedField("TITLE", rawText, true);
			System.String url = matchSingleVCardPrefixedField("URL", rawText, true);
			return new AddressBookParsedResult(names, null, phoneNumbers, emails, note, addresses, org, birthday, title, url);
		}
		
		private static System.String[] matchVCardPrefixedField(System.String prefix, System.String rawText, bool trim)
		{
			System.Collections.ArrayList matches = null;
			int i = 0;
			int max = rawText.Length;
			while (i < max)
			{
				//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
				i = rawText.IndexOf(prefix, i);
				if (i < 0)
				{
					break;
				}
				if (i > 0 && rawText[i - 1] != '\n')
				{
					// then this didn't start a new token, we matched in the middle of something
					i++;
					continue;
				}
				i += prefix.Length; // Skip past this prefix we found to start
				if (rawText[i] != ':' && rawText[i] != ';')
				{
					continue;
				}
				while (rawText[i] != ':')
				{
					// Skip until a colon
					i++;
				}
				i++; // skip colon
				int start = i; // Found the start of a match here
				//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
				i = rawText.IndexOf('\n', i); // Really, ends in \r\n
				if (i < 0)
				{
					// No terminating end character? uh, done. Set i such that loop terminates and break
					i = max;
				}
				else if (i > start)
				{
					// found a match
					if (matches == null)
					{
						matches = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(3)); // lazy init
					}
					System.String element = rawText.Substring(start, (i) - (start));
					if (trim)
					{
						element = element.Trim();
					}
					matches.Add(element);
					i++;
				}
				else
				{
					i++;
				}
			}
			if (matches == null || (matches.Count == 0))
			{
				return null;
			}
			return toStringArray(matches);
		}
		
		internal static System.String matchSingleVCardPrefixedField(System.String prefix, System.String rawText, bool trim)
		{
			System.String[] values = matchVCardPrefixedField(prefix, rawText, trim);
			return values == null?null:values[0];
		}
		
		private static bool isLikeVCardDate(System.String value_Renamed)
		{
			if (value_Renamed == null)
			{
				return true;
			}
			// Not really sure this is true but matches practice
			// Mach YYYYMMDD
			if (isStringOfDigits(value_Renamed, 8))
			{
				return true;
			}
			// or YYYY-MM-DD
			return value_Renamed.Length == 10 && value_Renamed[4] == '-' && value_Renamed[7] == '-' && isSubstringOfDigits(value_Renamed, 0, 4) && isSubstringOfDigits(value_Renamed, 5, 2) && isSubstringOfDigits(value_Renamed, 8, 2);
		}
		
		private static System.String formatAddress(System.String address)
		{
			if (address == null)
			{
				return null;
			}
			int length = address.Length;
			System.Text.StringBuilder newAddress = new System.Text.StringBuilder(length);
			for (int j = 0; j < length; j++)
			{
				char c = address[j];
				if (c == ';')
				{
					newAddress.Append(' ');
				}
				else
				{
					newAddress.Append(c);
				}
			}
			return newAddress.ToString().Trim();
		}
		
		/// <summary> Formats name fields of the form "Public;John;Q.;Reverend;III" into a form like
		/// "Reverend John Q. Public III".
		/// 
		/// </summary>
		/// <param name="names">name values to format, in place
		/// </param>
		private static void  formatNames(System.String[] names)
		{
			if (names != null)
			{
				for (int i = 0; i < names.Length; i++)
				{
					System.String name = names[i];
					System.String[] components = new System.String[5];
					int start = 0;
					int end;
					int componentIndex = 0;
					//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
					while ((end = name.IndexOf(';', start)) > 0)
					{
						components[componentIndex] = name.Substring(start, (end) - (start));
						componentIndex++;
						start = end + 1;
					}
					components[componentIndex] = name.Substring(start);
					System.Text.StringBuilder newName = new System.Text.StringBuilder(100);
					maybeAppendComponent(components, 3, newName);
					maybeAppendComponent(components, 1, newName);
					maybeAppendComponent(components, 2, newName);
					maybeAppendComponent(components, 0, newName);
					maybeAppendComponent(components, 4, newName);
					names[i] = newName.ToString().Trim();
				}
			}
		}
		
		private static void  maybeAppendComponent(System.String[] components, int i, System.Text.StringBuilder newName)
		{
			if (components[i] != null)
			{
				newName.Append(' ');
				newName.Append(components[i]);
			}
		}
	}
}