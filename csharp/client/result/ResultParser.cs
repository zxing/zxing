using System;
using System.Collections.Generic;
using System.Text;

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

namespace com.google.zxing.client.result
{

	using Result = com.google.zxing.Result;
    using System.Text.RegularExpressions;


	/// <summary>
	/// <p>Abstract class representing the result of decoding a barcode, as more than
	/// a String -- as some type of structured data. This might be a subclass which represents
	/// a URL, or an e-mail address. <seealso cref="#parseResult(Result)"/> will turn a raw
	/// decoded string into the most appropriate type of structured representation.</p>
	/// 
	/// <p>Thanks to Jeff Griffin for proposing rewrite of these classes that relies less
	/// on exception-based mechanisms during parsing.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public abstract class ResultParser
	{

	  private static readonly ResultParser[] PARSERS = {new BookmarkDoCoMoResultParser(), new AddressBookDoCoMoResultParser(), new EmailDoCoMoResultParser(), new AddressBookAUResultParser(), new VCardResultParser(), new BizcardResultParser(), new VEventResultParser(), new EmailAddressResultParser(), new SMTPResultParser(), new TelResultParser(), new SMSMMSResultParser(), new SMSTOMMSTOResultParser(), new GeoResultParser(), new WifiResultParser(), new URLTOResultParser(), new URIResultParser(), new ISBNResultParser(), new ProductResultParser(), new ExpandedProductResultParser()};

	  private static readonly string DIGITS = "\\d*";
	  private static readonly string ALPHANUM = "[a-zA-Z0-9]*";
	  private static readonly string AMPERSAND = "&";
	  private static readonly string EQUALS = "=";
	  private const string BYTE_ORDER_MARK = "\ufeff";

	  /// <summary>
	  /// Attempts to parse the raw <seealso cref="Result"/>'s contents as a particular type
	  /// of information (email, URL, etc.) and return a <seealso cref="ParsedResult"/> encapsulating
	  /// the result of parsing.
	  /// </summary>
	  public abstract ParsedResult parse(Result theResult);

	  protected internal static string getMassagedText(Result result)
	  {
		string text = result.Text;
		if (text.StartsWith(BYTE_ORDER_MARK))
		{
		  text = text.Substring(1);
		}
		return text;
	  }

	  public static ParsedResult parseResult(Result theResult)
	  {
		foreach (ResultParser parser in PARSERS)
		{
		  ParsedResult result = parser.parse(theResult);
		  if (result != null)
		  {
			return result;
		  }
		}
		return new TextParsedResult(theResult.Text, null);
	  }

	  protected internal static void maybeAppend(string value, StringBuilder result)
	  {
		if (value != null)
		{
		  result.Append('\n');
		  result.Append(value);
		}
	  }

	  protected internal static void maybeAppend(string[] value, StringBuilder result)
	  {
		if (value != null)
		{
		  foreach (string s in value)
		  {
			result.Append('\n');
			result.Append(s);
		  }
		}
	  }

	  protected internal static string[] maybeWrap(string value)
	  {
		return value == null ? null : new string[] {value};
	  }

	  protected internal static string unescapeBackslash(string escaped)
	  {
		int backslash = (int) escaped.IndexOf('\\');
		if (backslash < 0)
		{
		  return escaped;
		}
		int max = escaped.Length;
		StringBuilder unescaped = new StringBuilder(max - 1);
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

	  protected internal static int parseHexDigit(char c)
	  {
		if (c >= '0' && c <= '9')
		{
		  return c - '0';
		}
		if (c >= 'a' && c <= 'f')
		{
		  return 10 + (c - 'a');
		}
		if (c >= 'A' && c <= 'F')
		{
		  return 10 + (c - 'A');
		}
		return -1;
	  }

	  protected internal static bool isStringOfDigits(string value, int length)
	  {
        //return value != null && length == value.Length && DIGITS.matcher(value).matches();

		return value != null && length == value.Length && new Regex(DIGITS).IsMatch(value);
	  }

	  protected internal static bool isSubstringOfDigits(string value, int offset, int length)
	  {
		if (value == null)
		{
		  return false;
		}
		int max = offset + length;
        string s = value.Substring(offset, length);
        return isStringOfDigits(s, length);
          //return  value.Length >= max && DIGITS.matcher(value.subSequence(offset, max)).matches();
	  }

	  protected internal static bool isSubstringOfAlphaNumeric(string value, int offset, int length)
	  {
		if (value == null)
		{
		  return false;
		}
		int max = offset + length;
        string s = value.Substring(offset, length);
        return length == s.Length && new Regex(ALPHANUM).IsMatch(s);
        //return value.Length >= max && ALPHANUM.matcher(value.subSequence(offset, max)).matches();
      }

	  internal static IDictionary<string, string> parseNameValuePairs(string uri)
	  {
		int paramStart = uri.IndexOf('?');
		if (paramStart < 0)
		{
		  return null;
		}
		IDictionary<string, string> result = new Dictionary<string, string>(3);
		foreach (string keyValue in AMPERSAND.Split(new string[] {uri.Substring(paramStart + 1)},StringSplitOptions.None))
		{
		  appendKeyValue(keyValue, result);
		}
		return result;
	  }

	  private static void appendKeyValue(string keyValue, IDictionary<string, string> result)
	  {
		string[] keyValueTokens = EQUALS.Split(new string[] {keyValue}, 2,StringSplitOptions.None);
		if (keyValueTokens.Length == 2)
		{
		  string key = keyValueTokens[0];
		  string value = keyValueTokens[1];
		  try
		  {
              //value = URLDecoder.decode(value, "UTF-8");
              value = System.Web.HttpUtility.UrlDecode(value,Encoding.UTF8);
              result[key] = value;
		  }
          //catch (UnsupportedEncodingException uee)
          //{
          //  throw new IllegalStateException(uee); // can't happen
          //}
		  catch (System.ArgumentException iae)
		  {
			// continue; invalid data such as an escape like %0t
		  }
		}
	  }

	  internal static string[] matchPrefixedField(string prefix, string rawText, char endChar, bool trim)
	  {
		List<string> matches = null;
		int i = 0;
		int max = rawText.Length;
		while (i < max)
		{
		  i = rawText.IndexOf(prefix, i);
		  if (i < 0)
		  {
			break;
		  }
		  i += prefix.Length; // Skip past this prefix we found to start
		  int start = i; // Found the start of a match here
		  bool more = true;
		  while (more)
		  {
			i = (int)rawText.IndexOf( endChar, i);
			if (i < 0)
			{
			  // No terminating end character? uh, done. Set i such that loop terminates and break
			  i = rawText.Length;
			  more = false;
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
				matches = new List<string>(3); // lazy init
			  }
			  string element = unescapeBackslash(rawText.Substring(start, i - start));
			  if (trim)
			  {
				element = element.Trim();
			  }
			  matches.Add(element);
			  i++;
			  more = false;
			}
		  }
		}
		if (matches == null || matches.Count == 0)
		{
		  return null;
		}
		return matches.ToArray();
	  }

	  internal static string matchSinglePrefixedField(string prefix, string rawText, char endChar, bool trim)
	  {
		string[] matches = matchPrefixedField(prefix, rawText, endChar, trim);
		return matches == null ? null : matches[0];
	  }

	}

}