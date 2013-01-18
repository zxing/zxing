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
	/// @author Sean Owen
	/// </summary>
	public sealed class BookmarkDoCoMoResultParser : AbstractDoCoMoResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = result.Text;
		if (!rawText.StartsWith("MEBKM:"))
		{
		  return null;
		}
		string title = matchSingleDoCoMoPrefixedField("TITLE:", rawText, true);
		string[] rawUri = matchDoCoMoPrefixedField("URL:", rawText, true);
		if (rawUri == null)
		{
		  return null;
		}
		string uri = rawUri[0];
		return URIResultParser.isBasicallyValidURI(uri) ? new URIParsedResult(uri, title) : null;
	  }

	}
}