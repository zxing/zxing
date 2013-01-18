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

	/// <summary>
	/// Implements the "MECARD" address book entry format.
	/// 
	/// Supported keys: N, SOUND, TEL, EMAIL, NOTE, ADR, BDAY, URL, plus ORG
	/// Unsupported keys: TEL-AV, NICKNAME
	/// 
	/// Except for TEL, multiple values for keys are also not supported;
	/// the first one found takes precedence.
	/// 
	/// Our understanding of the MECARD format is based on this document:
	/// 
	/// http://www.mobicode.org.tw/files/OMIA%20Mobile%20Bar%20Code%20Standard%20v3.2.1.doc 
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class AddressBookDoCoMoResultParser : AbstractDoCoMoResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		if (!rawText.StartsWith("MECARD:"))
		{
		  return null;
		}
		string[] rawName = matchDoCoMoPrefixedField("N:", rawText, true);
		if (rawName == null)
		{
		  return null;
		}
		string name = parseName(rawName[0]);
		string pronunciation = matchSingleDoCoMoPrefixedField("SOUND:", rawText, true);
		string[] phoneNumbers = matchDoCoMoPrefixedField("TEL:", rawText, true);
		string[] emails = matchDoCoMoPrefixedField("EMAIL:", rawText, true);
		string note = matchSingleDoCoMoPrefixedField("NOTE:", rawText, false);
		string[] addresses = matchDoCoMoPrefixedField("ADR:", rawText, true);
		string birthday = matchSingleDoCoMoPrefixedField("BDAY:", rawText, true);
		if (birthday != null && !isStringOfDigits(birthday, 8))
		{
		  // No reason to throw out the whole card because the birthday is formatted wrong.
		  birthday = null;
		}
		string url = matchSingleDoCoMoPrefixedField("URL:", rawText, true);

		// Although ORG may not be strictly legal in MECARD, it does exist in VCARD and we might as well
		// honor it when found in the wild.
		string org = matchSingleDoCoMoPrefixedField("ORG:", rawText, true);

		return new AddressBookParsedResult(maybeWrap(name), pronunciation, phoneNumbers, null, emails, null, null, note, addresses, null, org, birthday, null, url);
	  }

	  private static string parseName(string name)
	  {
		int comma =(int) name.IndexOf(',');
		if (comma >= 0)
		{
		  // Format may be last,first; switch it around
		  return name.Substring(comma + 1) + ' ' + name.Substring(0, comma);
		}
		return name;
	  }

	}

}