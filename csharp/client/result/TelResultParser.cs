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
	/// Parses a "tel:" URI result, which specifies a phone number.
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class TelResultParser : ResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		if (!rawText.StartsWith("tel:") && !rawText.StartsWith("TEL:"))
		{
		  return null;
		}
		// Normalize "TEL:" to "tel:"
		string telURI = rawText.StartsWith("TEL:") ? "tel:" + rawText.Substring(4) : rawText;
		// Drop tel, query portion
		int queryStart = rawText.IndexOf('?', 4);
		string number = queryStart < 0 ? rawText.Substring(4) : rawText.Substring(4, queryStart - 4);
		return new TelParsedResult(number, telURI, null);
	  }

	}
}