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
	/// Implements KDDI AU's address book format. See
	/// <a href="http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html">
	/// http://www.au.kddi.com/ezfactory/tec/two_dimensions/index.html</a>.
	/// (Thanks to Yuzo for translating!)
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class AddressBookAUResultParser : ResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		// MEMORY is mandatory; seems like a decent indicator, as does end-of-record separator CR/LF
		if (!rawText.Contains("MEMORY") || !rawText.Contains("\r\n"))
		{
		  return null;
		}

		// NAME1 and NAME2 have specific uses, namely written name and pronunciation, respectively.
		// Therefore we treat them specially instead of as an array of names.
		string name = matchSinglePrefixedField("NAME1:", rawText, '\r', true);
		string pronunciation = matchSinglePrefixedField("NAME2:", rawText, '\r', true);

		string[] phoneNumbers = matchMultipleValuePrefix("TEL", 3, rawText, true);
		string[] emails = matchMultipleValuePrefix("MAIL", 3, rawText, true);
		string note = matchSinglePrefixedField("MEMORY:", rawText, '\r', false);
		string address = matchSinglePrefixedField("ADD:", rawText, '\r', true);
		string[] addresses = address == null ? null : new string[] {address};
		return new AddressBookParsedResult(maybeWrap(name), pronunciation, phoneNumbers, null, emails, null, null, note, addresses, null, null, null, null, null);
	  }

	  private static string[] matchMultipleValuePrefix(string prefix, int max, string rawText, bool trim)
	  {
		List<string> values = null;
		for (int i = 1; i <= max; i++)
		{
		  string value = matchSinglePrefixedField(prefix + i + ':', rawText, '\r', trim);
		  if (value == null)
		  {
			break;
		  }
		  if (values == null)
		  {
			values = new List<string>(max); // lazy init
		  }
		  values.Add(value);
		}
		if (values == null)
		{
		  return null;
		}
		return values.ToArray();
	  }

	}

}