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
	
	/// <summary> Parses the "URLTO" result format, which is of the form "URLTO:[title]:[url]".
	/// This seems to be used sometimes, but I am not able to find documentation
	/// on its origin or official format?
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class URLTOResultParser
	{
		
		private URLTOResultParser()
		{
		}
		
		public static URIParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			if (rawText == null || (!rawText.StartsWith("urlto:") && !rawText.StartsWith("URLTO:")))
			{
				return null;
			}
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			int titleEnd = rawText.IndexOf(':', 6);
			if (titleEnd < 0)
			{
				return null;
			}
			System.String title = titleEnd <= 6?null:rawText.Substring(6, (titleEnd) - (6));
			System.String uri = rawText.Substring(titleEnd + 1);
			return new URIParsedResult(uri, title);
		}
	}
}