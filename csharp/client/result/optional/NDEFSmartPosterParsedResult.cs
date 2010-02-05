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
using ParsedResult = com.google.zxing.client.result.ParsedResult;
using ParsedResultType = com.google.zxing.client.result.ParsedResultType;
namespace com.google.zxing.client.result.optional
{
	
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class NDEFSmartPosterParsedResult:ParsedResult
	{
		public System.String Title
		{
			get
			{
				return title;
			}
			
		}
		public System.String URI
		{
			get
			{
				return uri;
			}
			
		}
		public int Action
		{
			get
			{
				return action;
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				if (title == null)
				{
					return uri;
				}
				else
				{
					return title + '\n' + uri;
				}
			}
			
		}
		
		public const int ACTION_UNSPECIFIED = - 1;
		public const int ACTION_DO = 0;
		public const int ACTION_SAVE = 1;
		public const int ACTION_OPEN = 2;
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'title '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String title;
		//UPGRADE_NOTE: Final was removed from the declaration of 'uri '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String uri;
		//UPGRADE_NOTE: Final was removed from the declaration of 'action '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int action;
		
		internal NDEFSmartPosterParsedResult(int action, System.String uri, System.String title):base(ParsedResultType.NDEF_SMART_POSTER)
		{
			this.action = action;
			this.uri = uri;
			this.title = title;
		}
	}
}