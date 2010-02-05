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
namespace com.google.zxing.client.result
{
	
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class URIParsedResult:ParsedResult
	{
		public System.String URI
		{
			get
			{
				return uri;
			}
			
		}
		public System.String Title
		{
			get
			{
				return title;
			}
			
		}
		/// <returns> true if the URI contains suspicious patterns that may suggest it intends to
		/// mislead the user about its true nature. At the moment this looks for the presence
		/// of user/password syntax in the host/authority portion of a URI which may be used
		/// in attempts to make the URI's host appear to be other than it is. Example:
		/// http://yourbank.com@phisher.com  This URI connects to phisher.com but may appear
		/// to connect to yourbank.com at first glance.
		/// </returns>
		public bool PossiblyMaliciousURI
		{
			get
			{
				return containsUser();
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				System.Text.StringBuilder result = new System.Text.StringBuilder(30);
				maybeAppend(title, result);
				maybeAppend(uri, result);
				return result.ToString();
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'uri '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String uri;
		//UPGRADE_NOTE: Final was removed from the declaration of 'title '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String title;
		
		public URIParsedResult(System.String uri, System.String title):base(ParsedResultType.URI)
		{
			this.uri = massageURI(uri);
			this.title = title;
		}
		
		private bool containsUser()
		{
			// This method is likely not 100% RFC compliant yet
			int hostStart = uri.IndexOf(':'); // we should always have scheme at this point
			hostStart++;
			// Skip slashes preceding host
			int uriLength = uri.Length;
			while (hostStart < uriLength && uri[hostStart] == '/')
			{
				hostStart++;
			}
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			int hostEnd = uri.IndexOf('/', hostStart);
			if (hostEnd < 0)
			{
				hostEnd = uriLength;
			}
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			int at = uri.IndexOf('@', hostStart);
			return at >= hostStart && at < hostEnd;
		}
		
		/// <summary> Transforms a string that represents a URI into something more proper, by adding or canonicalizing
		/// the protocol.
		/// </summary>
		private static System.String massageURI(System.String uri)
		{
			int protocolEnd = uri.IndexOf(':');
			if (protocolEnd < 0)
			{
				// No protocol, assume http
				uri = "http://" + uri;
			}
			else if (isColonFollowedByPortNumber(uri, protocolEnd))
			{
				// Found a colon, but it looks like it is after the host, so the protocol is still missing
				uri = "http://" + uri;
			}
			else
			{
				// Lowercase protocol to avoid problems
				uri = uri.Substring(0, (protocolEnd) - (0)).ToLower() + uri.Substring(protocolEnd);
			}
			return uri;
		}
		
		private static bool isColonFollowedByPortNumber(System.String uri, int protocolEnd)
		{
			//UPGRADE_WARNING: Method 'java.lang.String.indexOf' was converted to 'System.String.IndexOf' which may throw an exception. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1101'"
			int nextSlash = uri.IndexOf('/', protocolEnd + 1);
			if (nextSlash < 0)
			{
				nextSlash = uri.Length;
			}
			if (nextSlash <= protocolEnd + 1)
			{
				return false;
			}
			for (int x = protocolEnd + 1; x < nextSlash; x++)
			{
				if (uri[x] < '0' || uri[x] > '9')
				{
					return false;
				}
			}
			return true;
		}
	}
}