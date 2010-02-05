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
	
	/// <summary> A simple result type encapsulating a string that has no further
	/// interpretation.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class TextParsedResult:ParsedResult
	{
		public System.String Text
		{
			get
			{
				return text;
			}
			
		}
		public System.String Language
		{
			get
			{
				return language;
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				return text;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'text '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String text;
		//UPGRADE_NOTE: Final was removed from the declaration of 'language '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String language;
		
		public TextParsedResult(System.String text, System.String language):base(ParsedResultType.TEXT)
		{
			this.text = text;
			this.language = language;
		}
	}
}