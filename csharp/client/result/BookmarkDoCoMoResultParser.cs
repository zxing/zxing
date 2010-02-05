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
	
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class BookmarkDoCoMoResultParser:AbstractDoCoMoResultParser
	{
		
		private BookmarkDoCoMoResultParser()
		{
		}
		
		public static URIParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			if (rawText == null || !rawText.StartsWith("MEBKM:"))
			{
				return null;
			}
			System.String title = matchSingleDoCoMoPrefixedField("TITLE:", rawText, true);
			System.String[] rawUri = matchDoCoMoPrefixedField("URL:", rawText, true);
			if (rawUri == null)
			{
				return null;
			}
			System.String uri = rawUri[0];
			if (!URIResultParser.isBasicallyValidURI(uri))
			{
				return null;
			}
			return new URIParsedResult(uri, title);
		}
	}
}