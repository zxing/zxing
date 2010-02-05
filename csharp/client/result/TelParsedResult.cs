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
using System;
namespace com.google.zxing.client.result
{
	
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class TelParsedResult:ParsedResult
	{
		public System.String Number
		{
			get
			{
				return number;
			}
			
		}
		public System.String TelURI
		{
			get
			{
				return telURI;
			}
			
		}
		public System.String Title
		{
			get
			{
				return title;
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				System.Text.StringBuilder result = new System.Text.StringBuilder(20);
				maybeAppend(number, result);
				maybeAppend(title, result);
				return result.ToString();
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'number '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String number;
		//UPGRADE_NOTE: Final was removed from the declaration of 'telURI '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String telURI;
		//UPGRADE_NOTE: Final was removed from the declaration of 'title '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String title;
		
		public TelParsedResult(System.String number, System.String telURI, System.String title):base(ParsedResultType.TEL)
		{
			this.number = number;
			this.telURI = telURI;
			this.title = title;
		}
	}
}