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
	/// Parses the "URLTO" result format, which is of the form "URLTO:[title]:[url]".
	/// This seems to be used sometimes, but I am not able to find documentation
	/// on its origin or official format?
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class URLTOResultParser : ResultParser
	{

	  public override ParsedResult parse(Result result)
	  {
		string rawText = getMassagedText(result);
		if (!rawText.StartsWith("urlto:") && !rawText.StartsWith("URLTO:"))
		{
		  return null;
		}
		int titleEnd = rawText.IndexOf(':', 6);
		if (titleEnd < 0)
		{
		  return null;
		}
		string title = titleEnd <= 6 ? null : rawText.Substring(6, titleEnd - 6);
		string uri = rawText.Substring(titleEnd + 1);
		return new URIParsedResult(uri, title);
	  }

	}
}