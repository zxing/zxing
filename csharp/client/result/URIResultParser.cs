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

using System.Text.RegularExpressions;

namespace com.google.zxing.client.result
{

	using Result = com.google.zxing.Result;


	/// <summary>
	/// Tries to parse results that are a URI of some kind.
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class URIResultParser : ResultParser
	{

	  private const string ALPHANUM_PART = "[a-zA-Z0-9\\-]";
	  private static readonly string URL_WITH_PROTOCOL_PATTERN = "[a-zA-Z0-9]{2,}:";
	  private static readonly string URL_WITHOUT_PROTOCOL_PATTERN = '(' + ALPHANUM_PART + "+\\.)+" + ALPHANUM_PART + "{2,}" + "(:\\d{1,5})?" + "(/|\\?|$)"; // query, path or nothing -  maybe port -  host name elements

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		// We specifically handle the odd "URL" scheme here for simplicity and add "URI" for fun
		// Assume anything starting this way really means to be a URI
		if (rawText.StartsWith("URL:") || rawText.StartsWith("URI:"))
		{
		  return new URIParsedResult(rawText.Substring(4).Trim(), null);
		}
		rawText = rawText.Trim();
		return isBasicallyValidURI(rawText) ? new URIParsedResult(rawText, null) : null;
	  }

	  internal static bool isBasicallyValidURI(string uri)
	  {
		Match m = Regex.Match(uri,URL_WITH_PROTOCOL_PATTERN) ;
		if (m.Success && m.Index == 0) // match at start only
		{
		  return true;
		}
		m = Regex.Match(uri,URL_WITHOUT_PROTOCOL_PATTERN);
		return m.Success && m.Index == 0;
	  }

	}
}