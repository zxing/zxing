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
	
	/// <summary> Tries to parse results that are a URI of some kind.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	sealed class URIResultParser:ResultParser
	{
		
		private URIResultParser()
		{
		}
		
		public static URIParsedResult parse(Result result)
		{
			System.String rawText = result.Text;
			// We specifically handle the odd "URL" scheme here for simplicity
			if (rawText != null && rawText.StartsWith("URL:"))
			{
				rawText = rawText.Substring(4);
			}
			if (!isBasicallyValidURI(rawText))
			{
				return null;
			}
			return new URIParsedResult(rawText, null);
		}
		
		/// <summary> Determines whether a string is not obviously not a URI. This implements crude checks; this class does not
		/// intend to strictly check URIs as its only function is to represent what is in a barcode, but, it does
		/// need to know when a string is obviously not a URI.
		/// </summary>
		internal static bool isBasicallyValidURI(System.String uri)
		{
			if (uri == null || uri.IndexOf(' ') >= 0 || uri.IndexOf('\n') >= 0)
			{
				return false;
			}
			// Look for period in a domain but followed by at least a two-char TLD
			// Forget strings that don't have a valid-looking protocol
			int period = uri.IndexOf('.');
			if (period >= uri.Length - 2)
			{
				return false;
			}
			int colon = uri.IndexOf(':');
			if (period < 0 && colon < 0)
			{
				return false;
			}
			if (colon >= 0)
			{
				if (period < 0 || period > colon)
				{
					// colon ends the protocol
					for (int i = 0; i < colon; i++)
					{
						char c = uri[i];
						if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z'))
						{
							return false;
						}
					}
				}
				else
				{
					// colon starts the port; crudely look for at least two numbers
					if (colon >= uri.Length - 2)
					{
						return false;
					}
					for (int i = colon + 1; i < colon + 3; i++)
					{
						char c = uri[i];
						if (c < '0' || c > '9')
						{
							return false;
						}
					}
				}
			}
			return true;
		}
	}
}