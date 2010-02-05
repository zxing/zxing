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
	
	/// <summary> <p>Abstract class representing the result of decoding a barcode, as more than
	/// a String -- as some type of structured data. This might be a subclass which represents
	/// a URL, or an e-mail address. {@link #parseResult(com.google.zxing.Result)} will turn a raw
	/// decoded string into the most appropriate type of structured representation.</p>
	/// 
	/// <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
	/// on exception-based mechanisms during parsing.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public abstract class ResultParser
	{
		
		public static ParsedResult parseResult(Result theResult)
		{
			// This is a bit messy, but given limited options in MIDP / CLDC, this may well be the simplest
			// way to go about this. For example, we have no reflection available, really.
			// Order is important here.
			ParsedResult result;
			if ((result = BookmarkDoCoMoResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = AddressBookDoCoMoResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = EmailDoCoMoResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = AddressBookAUResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = VCardResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = BizcardResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = VEventResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = EmailAddressResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = TelResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = SMSMMSResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = GeoResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = URLTOResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = URIResultParser.parse(theResult)) != null)
			{
				return result;
			}
			else if ((result = ISBNResultParser.parse(theResult)) != null)
			{
				// We depend on ISBN parsing coming before UPC, as it is a subset.
				return result;
			}
			else if ((result = ProductResultParser.parse(theResult)) != null)
			{
				return result;
			}
			return new TextParsedResult(theResult.Text, null);
		}
		
		protected internal static void  maybeAppend(System.String value_Renamed, System.Text.StringBuilder result)
		{
			if (value_Renamed != null)
			{
				result.Append('\n');
				result.Append(value_Renamed);
			}
		}
		
		protected internal static void  maybeAppend(System.String[] value_Renamed, System.Text.StringBuilder result)
		{
			if (value_Renamed != null)
			{
				for (int i = 0; i < value_Renamed.Length; i++)
				{
					result.Append('\n');
					result.Append(value_Renamed[i]);
				}
			}
		}
		
		protected internal static System.String[] maybeWrap(System.String value_Renamed)
		{
			return value_Renamed == null?null:new System.String[]{value_Renamed};
		}
		
		protected internal static System.String unescapeBackslash(System.String escaped)
		{
			if (escaped != null)
			{
				int backslash = escaped.IndexOf('\\');
				if (backslash >= 0)
				{
					int max = escaped.Length;
					System.Text.StringBuilder unescaped = new System.Text.StringBuilder(max - 1);
					unescaped.Append(escaped.ToCharArray(), 0, backslash);
					bool nextIsEscaped = false;
					for (int i = backslash; i < max; i++)
					{
						char c = escaped[i];
						if (nextIsEscaped || c != '\\')
						{
							unescaped.Append(c);
							nextIsEscaped = false;
						}
						else
						{
							nextIsEscaped = true;
						}
					}
					return unescaped.ToString();
				}
			}
			return escaped;
		}
		
		private static System.String urlDecode(System.String escaped)
		{
			
			// No we can't use java.net.URLDecoder here. JavaME doesn't have it.
			if (escaped == null)
			{
				return null;
			}
			char[] escapedArray = escaped.ToCharArray();
			
			int first = findFirstEscape(escapedArray);
			if (first < 0)
			{
				return escaped;
			}
			
			int max = escapedArray.Length;
			// final length is at most 2 less than original due to at least 1 unescaping
			System.Text.StringBuilder unescaped = new System.Text.StringBuilder(max - 2);
			// Can append everything up to first escape character
			unescaped.Append(escapedArray, 0, first);
			
			for (int i = first; i < max; i++)
			{
				char c = escapedArray[i];
				if (c == '+')
				{
					// + is translated directly into a space
					unescaped.Append(' ');
				}
				else if (c == '%')
				{
					// Are there even two more chars? if not we will just copy the escaped sequence and be done
					if (i >= max - 2)
					{
						unescaped.Append('%'); // append that % and move on
					}
					else
					{
						int firstDigitValue = parseHexDigit(escapedArray[++i]);
						int secondDigitValue = parseHexDigit(escapedArray[++i]);
						if (firstDigitValue < 0 || secondDigitValue < 0)
						{
							// bad digit, just move on
							unescaped.Append('%');
							unescaped.Append(escapedArray[i - 1]);
							unescaped.Append(escapedArray[i]);
						}
						unescaped.Append((char) ((firstDigitValue << 4) + secondDigitValue));
					}
				}
				else
				{
					unescaped.Append(c);
				}
			}
			return unescaped.ToString();
		}
		
		private static int findFirstEscape(char[] escapedArray)
		{
			int max = escapedArray.Length;
			for (int i = 0; i < max; i++)
			{
				char c = escapedArray[i];
				if (c == '+' || c == '%')
				{
					return i;
				}
			}
			return - 1;
		}
		
		private static int parseHexDigit(char c)
		{
			if (c >= 'a')
			{
				if (c <= 'f')
				{
					return 10 + (c - 'a');
				}
			}
			else if (c >= 'A')
			{
				if (c <= 'F')
				{
					return 10 + (c - 'A');
				}
			}
			else if (c >= '0')
			{
				if (c <= '9')
				{
					return c - '0';
				}
			}
			return - 1;
		}
		
		protected internal static bool isStringOfDigits(System.String value_Renamed, int length)
		{
			if (value_Renamed == null)
			{
				return false;
			}
			int stringLength = value_Renamed.Length;
			if (length != stringLength)
			{
				return false;
			}
			for (int i = 0; i < length; i++)
			{
				char c = value_Renamed[i];
				if (c < '0' || c > '9')
				{
					return false;
				}
			}
			return true;
		}
		
		protected internal static bool isSubstringOfDigits(System.String value_Renamed, int offset, int length)
		{
			if (value_Renamed == null)
			{
				return false;
			}
			int stringLength = value_Renamed.Length;
			int max = offset + length;
			if (stringLength < max)
			{
				return false;
			}
			for (int i = offset; i < max; i++)
			{
				char c = value_Renamed[i];
				if (c < '0' || c > '9')
				{
					return false;
				}
			}
			return true;
		}
		
		internal static System.Collections.Hashtable parseNameValuePairs(System.String uri)
		{
			int paramStart = uri.IndexOf('?');
			if (paramStart < 0)
			{
				return null;
			}
			System.Collections.Hashtable result = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable(3));
			paramStart++;
			int paramEnd;
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			while ((paramEnd = uri.IndexOf('&', paramStart)) >= 0)
			{
				appendKeyValue(uri, paramStart, paramEnd, result);
				paramStart = paramEnd + 1;
			}
			appendKeyValue(uri, paramStart, uri.Length, result);
			return result;
		}
		
		private static void  appendKeyValue(System.String uri, int paramStart, int paramEnd, System.Collections.Hashtable result)
		{
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			int separator = uri.IndexOf('=', paramStart);
			if (separator >= 0)
			{
				// key = value
				System.String key = uri.Substring(paramStart, (separator) - (paramStart));
				System.String value_Renamed = uri.Substring(separator + 1, (paramEnd) - (separator + 1));
				value_Renamed = urlDecode(value_Renamed);
				result[key] = value_Renamed;
			}
			// Can't put key, null into a hashtable
		}
		
		internal static System.String[] matchPrefixedField(System.String prefix, System.String rawText, char endChar, bool trim)
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
				i += prefix.Length; // Skip past this prefix we found to start
				int start = i; // Found the start of a match here
				bool done = false;
				while (!done)
				{
					//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
					i = rawText.IndexOf((System.Char) endChar, i);
					if (i < 0)
					{
						// No terminating end character? uh, done. Set i such that loop terminates and break
						i = rawText.Length;
						done = true;
					}
					else if (rawText[i - 1] == '\\')
					{
						// semicolon was escaped so continue
						i++;
					}
					else
					{
						// found a match
						if (matches == null)
						{
							matches = System.Collections.ArrayList.Synchronized(new System.Collections.ArrayList(3)); // lazy init
						}
						System.String element = unescapeBackslash(rawText.Substring(start, (i) - (start)));
						if (trim)
						{
							element = element.Trim();
						}
						matches.Add(element);
						i++;
						done = true;
					}
				}
			}
			if (matches == null || (matches.Count == 0))
			{
				return null;
			}
			return toStringArray(matches);
		}
		
		internal static System.String matchSinglePrefixedField(System.String prefix, System.String rawText, char endChar, bool trim)
		{
			System.String[] matches = matchPrefixedField(prefix, rawText, endChar, trim);
			return matches == null?null:matches[0];
		}
		
		internal static System.String[] toStringArray(System.Collections.ArrayList strings)
		{
			int size = strings.Count;
			System.String[] result = new System.String[size];
			for (int j = 0; j < size; j++)
			{
				result[j] = ((System.String) strings[j]);
			}
			return result;
		}
	}
}