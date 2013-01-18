using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using com.google.zxing.common;

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
	/// Parses contact information formatted according to the VCard (2.1) format. This is not a complete
	/// implementation but should parse information as commonly encoded in 2D barcodes.
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class VCardResultParser : ResultParser
	{

	  private static readonly string BEGIN_VCARD = "BEGIN:VCARD";
	  private static readonly string VCARD_LIKE_DATE = "\\d{4}-?\\d{2}-?\\d{2}";
	  private static readonly string CR_LF_SPACE_TAB = "\r\n[ \t]";
	  private static readonly string NEWLINE_ESCAPE = "\\\\[nN]";
	  private static readonly string VCARD_ESCAPES = "\\\\([,;\\\\])";
	  private static readonly string EQUALS = "=";
	  private static readonly string SEMICOLON = ";";
	  private static readonly string UNESCAPED_SEMICOLONS = "(?<!\\\\);+";

	  public override ParsedResult parse(Result result)
	  {
		// Although we should insist on the raw text ending with "END:VCARD", there's no reason
		// to throw out everything else we parsed just because this was omitted. In fact, Eclair
		// is doing just that, and we can't parse its contacts without this leniency.
		string rawText = getMassagedText(result);
	      Match m = Regex.Match(rawText, BEGIN_VCARD, RegexOptions.IgnoreCase);
		if (!m.Success || m.Index != 0)
		{
		  return null;
		}
		IList<IList<string>> names = matchVCardPrefixedField("FN", rawText, true, false);
		if (names == null)
		{
		  // If no display names found, look for regular name fields and format them
		  names = matchVCardPrefixedField("N", rawText, true, false);
		  formatNames(names);
		}
		IList<IList<string>> phoneNumbers = matchVCardPrefixedField("TEL", rawText, true, false);
		IList<IList<string>> emails = matchVCardPrefixedField("EMAIL", rawText, true, false);
		IList<string> note = matchSingleVCardPrefixedField("NOTE", rawText, false, false);
		IList<IList<string>> addresses = matchVCardPrefixedField("ADR", rawText, true, true);
		IList<string> org = matchSingleVCardPrefixedField("ORG", rawText, true, true);
		IList<string> birthday = matchSingleVCardPrefixedField("BDAY", rawText, true, false);
		if (birthday != null && !isLikeVCardDate(birthday[0]))
		{
		  birthday = null;
		}
		IList<string> title = matchSingleVCardPrefixedField("TITLE", rawText, true, false);
		IList<string> url = matchSingleVCardPrefixedField("URL", rawText, true, false);
		IList<string> instantMessenger = matchSingleVCardPrefixedField("IMPP", rawText, true, false);
		return new AddressBookParsedResult(toPrimaryValues(names), null, toPrimaryValues(phoneNumbers), toTypes(phoneNumbers), toPrimaryValues(emails), toTypes(emails), toPrimaryValue(instantMessenger), toPrimaryValue(note), toPrimaryValues(addresses), toTypes(addresses), toPrimaryValue(org), toPrimaryValue(birthday), toPrimaryValue(title), toPrimaryValue(url));
	  }

	  internal static IList<IList<string>> matchVCardPrefixedField(string prefix, string rawText, bool trim, bool parseFieldDivider)
	  {
		IList<IList<string>> matches = null;
		int i = 0;
		int max = rawText.Length;

		while (i < max)
		{

		  // At start or after newline, match prefix, followed by optional metadata 
		  // (led by ;) ultimately ending in colon
            //Matcher matcher = Pattern.compile("(?:^|\n)" + prefix + "(?:;([^:]*))?:", Pattern.CASE_INSENSITIVE).matcher(rawText);
            Regex matcher = new Regex("(?:^|\n)" + prefix + "(?:;([^:]*))?:", RegexOptions.IgnoreCase);
            //(rawText);
            if (i > 0)
		  {
			i--; // Find from i-1 not i since looking at the preceding character
		  }
            //if (!matcher.find(i))
            Match match = matcher.Match(rawText);
            if (!match.Success)
            {
			break;
		  }
          //i = matcher.end(0); // group 0 = whole pattern; end(0) is past final colon

		  i = match.Groups[0].Index + match.Groups[0].Length; // group 0 = whole pattern; end(0) is past final colon

		  string metadataString = match.Groups[1].Value; // group 1 = metadata substring
		  IList<string> metadata = null;
		  bool quotedPrintable = false;
		  string quotedPrintableCharset = null;
		  if (metadataString != null)
		  {
			foreach (string metadatum in SEMICOLON.Split(new string[] {metadataString},StringSplitOptions.None))
			{
			  if (metadata == null)
			  {
				metadata = new List<string>(1);
			  }
			  metadata.Add(metadatum);
              //string[] metadatumTokens = EQUALS.Split(metadatum, 2);
              string[] metadatumTokens =  EQUALS.Split(new string[] {metadatum},2,StringSplitOptions.None);
              if (metadatumTokens.Length > 1)
			  {
				string key = metadatumTokens[0];
				string value = metadatumTokens[1];
				if ("ENCODING"== key.ToUpper() && "QUOTED-PRINTABLE"==value.ToUpper())
				{
				  quotedPrintable = true;
				}
				else if ("CHARSET" == key.ToUpper())
				{
				  quotedPrintableCharset = value;
				}
			  }
			}
		  }

		  int matchStart = i; // Found the start of a match here

		  while ((i =(int) rawText.IndexOf( '\n', i)) >= 0) // Really, end in \r\n
		  {
			if (i < rawText.Length - 1 && (rawText[i + 1] == ' ' || rawText[i + 1] == '\t')) // this is only a continuation -  But if followed by tab or space,
			{
			  i += 2; // Skip \n and continutation whitespace
			} // If preceded by = in quoted printable
			else if (quotedPrintable && ((i >= 1 && rawText[i - 1] == '=') || (i >= 2 && rawText[i - 2] == '='))) // this is a continuation
			{
			  i++; // Skip \n
			}
			else
			{
			  break;
			}
		  }

		  if (i < 0)
		  {
			// No terminating end character? uh, done. Set i such that loop terminates and break
			i = max;
		  }
		  else if (i > matchStart)
		  {
			// found a match
			if (matches == null)
			{
			  matches = new List<IList<string>>(1); // lazy init
			}
			if (i >= 1 && rawText[i - 1] == '\r')
			{
			  i--; // Back up over \r, which really should be there
			}
			string element = rawText.Substring(matchStart, i - matchStart);
			if (trim)
			{
			  element = element.Trim();
			}
			if (quotedPrintable)
			{
			  element = decodeQuotedPrintable(element, quotedPrintableCharset);
			  if (parseFieldDivider)
			  {
                  //element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").Trim();
                  element = new Regex(UNESCAPED_SEMICOLONS).Replace(element,("\n")).Trim();
              }
			}
			else
			{
			  if (parseFieldDivider)
			  {
                  //element = UNESCAPED_SEMICOLONS.matcher(element).replaceAll("\n").Trim();
                  element = new Regex(UNESCAPED_SEMICOLONS).Replace(element,"\n").Trim();
              }
              //element = CR_LF_SPACE_TAB.matcher(element).replaceAll("");
              element = new Regex(CR_LF_SPACE_TAB).Replace(element,"");
              //element = NEWLINE_ESCAPE.matcher(element).replaceAll("\n");
              element = new Regex(NEWLINE_ESCAPE).Replace(element,"\n");
              //element = VCARD_ESCAPES.matcher(element).replaceAll("$1");
              element = new Regex(VCARD_ESCAPES).Replace(element,"$1");
            }
			if (metadata == null)
			{
			  IList<string> matchList = new List<string>(1);
			  matchList.Add(element);
			  matches.Add(matchList);
			}
			else
			{
			  metadata.Insert(0, element);
			  matches.Add(metadata);
			}
			i++;
		  }
		  else
		  {
			i++;
		  }

		}

		return matches;
	  }

	  private static string decodeQuotedPrintable(string value, string charset)
	  {
		int length = value.Length;
		StringBuilder result = new StringBuilder(length);
		MemoryStream fragmentBuffer = new MemoryStream();
		for (int i = 0; i < length; i++)
		{
		  char c = value[i];
		  switch (c)
		  {
			case '\r':
			case '\n':
			  break;
			case '=':
			  if (i < length - 2)
			  {
				char nextChar = value[i + 1];
				if (nextChar != '\r' && nextChar != '\n')
				{
				  char nextNextChar = value[i + 2];
				  int firstDigit = parseHexDigit(nextChar);
				  int secondDigit = parseHexDigit(nextNextChar);
				  if (firstDigit >= 0 && secondDigit >= 0)
				  {
					fragmentBuffer.WriteByte((byte)((firstDigit << 4) + secondDigit));
				  } // else ignore it, assume it was incorrectly encoded
				  i += 2;
				}
			  }
			  break;
			default:
			  maybeAppendFragment(fragmentBuffer, charset, result);
			  result.Append(c);
		  break;
		  }
		}
		maybeAppendFragment(fragmentBuffer, charset, result);
		return result.ToString();
	  }

	  private static void maybeAppendFragment(MemoryStream fragmentBuffer, string charset, StringBuilder result)
	  {
	      Encoding en = Encoding.GetEncoding(charset);
	      Encoding enFallback = Encoding.UTF8;
		if (fragmentBuffer.Length > 0)
		{
		  sbyte[] fragmentBytes = fragmentBuffer.ToArray().ToSBytes();
		  string fragment;
		  if (charset == null)
		  {
              fragment = en.GetString((byte[])(Array)fragmentBytes); ;
		  }
		  else
		  {
			try
			{
                fragment = en.GetString((byte[])(Array)fragmentBytes); ;
			}
			catch (ArgumentException e)
			{
			  // Yikes, well try anyway:
              //fragment = new string(fragmentBytes);
			    fragment = enFallback.GetString((byte[]) (Array) fragmentBytes);
			}
		  }
		  fragmentBuffer.Position=0;
		  result.Append(fragment);
		}
	  }

	  internal static IList<string> matchSingleVCardPrefixedField(string prefix, string rawText, bool trim, bool parseFieldDivider)
	  {
		IList<IList<string>> values = matchVCardPrefixedField(prefix, rawText, trim, parseFieldDivider);
		return values == null || values.Count == 0 ? null : values[0];
	  }

	  private static string toPrimaryValue(IList<string> list)
	  {
		return list == null || list.Count == 0 ? null : list[0];
	  }

	  private static string[] toPrimaryValues(ICollection<IList<string>> lists)
	  {
		if (lists == null || lists.Count == 0)
		{
		  return null;
		}
		List<string> result = new List<string>(lists.Count);
		foreach (IList<string> list in lists)
		{
		  result.Add(list[0]);
		}
		return result.ToArray();
	  }

	  private static string[] toTypes(ICollection<IList<string>> lists)
	  {
		if (lists == null || lists.Count == 0)
		{
		  return null;
		}
		List<string> result = new List<string>(lists.Count);
		foreach (IList<string> list in lists)
		{
		  string type = null;
		  for (int i = 1; i < list.Count; i++)
		  {
			string metadatum = list[i];
			int equals = metadatum.IndexOf('=');
			if (equals < 0)
			{
			  // take the whole thing as a usable label
			  type = metadatum;
			  break;
			}
			if ("TYPE" == (metadatum.Substring(0, equals)).ToUpper())
			{
			  type = metadatum.Substring(equals + 1);
			  break;
			}
		  }
		  result.Add(type);
		}
		return result.ToArray();
	  }

	  private static bool isLikeVCardDate(string value)
	  {
          //return value == null || VCARD_LIKE_DATE.matcher(value).matches();
          return value == null || new Regex(VCARD_LIKE_DATE).IsMatch(value);
      }

	  /// <summary>
	  /// Formats name fields of the form "Public;John;Q.;Reverend;III" into a form like
	  /// "Reverend John Q. Public III".
	  /// </summary>
	  /// <param name="names"> name values to format, in place </param>
	  private static void formatNames(IEnumerable<IList<string>> names)
	  {
		if (names != null)
		{
		  foreach (IList<string> list in names)
		  {
			string name = list[0];
			string[] components = new string[5];
			int start = 0;
			int end;
			int componentIndex = 0;
			while (componentIndex < components.Length - 1 && (end = name.IndexOf(';', start)) > 0)
			{
			  components[componentIndex] = name.Substring(start, end - start);
			  componentIndex++;
			  start = end + 1;
			}
			components[componentIndex] = name.Substring(start);
			StringBuilder newName = new StringBuilder(100);
			maybeAppendComponent(components, 3, newName);
			maybeAppendComponent(components, 1, newName);
			maybeAppendComponent(components, 2, newName);
			maybeAppendComponent(components, 0, newName);
			maybeAppendComponent(components, 4, newName);
			list[0] = newName.ToString().Trim();
		  }
		}
	  }

	  private static void maybeAppendComponent(string[] components, int i, StringBuilder newName)
	  {
		if (components[i] != null)
		{
		  newName.Append(' ');
		  newName.Append(components[i]);
		}
	  }

	}

}