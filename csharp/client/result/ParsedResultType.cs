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
	
	/// <summary> Represents the type of data encoded by a barcode -- from plain text, to a
	/// URI, to an e-mail address, etc.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ParsedResultType
	{
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'ADDRESSBOOK '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType ADDRESSBOOK = new ParsedResultType("ADDRESSBOOK");
		//UPGRADE_NOTE: Final was removed from the declaration of 'EMAIL_ADDRESS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType EMAIL_ADDRESS = new ParsedResultType("EMAIL_ADDRESS");
		//UPGRADE_NOTE: Final was removed from the declaration of 'PRODUCT '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType PRODUCT = new ParsedResultType("PRODUCT");
		//UPGRADE_NOTE: Final was removed from the declaration of 'URI '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType URI = new ParsedResultType("URI");
		//UPGRADE_NOTE: Final was removed from the declaration of 'TEXT '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType TEXT = new ParsedResultType("TEXT");
		//UPGRADE_NOTE: Final was removed from the declaration of 'ANDROID_INTENT '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType ANDROID_INTENT = new ParsedResultType("ANDROID_INTENT");
		//UPGRADE_NOTE: Final was removed from the declaration of 'GEO '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType GEO = new ParsedResultType("GEO");
		//UPGRADE_NOTE: Final was removed from the declaration of 'TEL '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType TEL = new ParsedResultType("TEL");
		//UPGRADE_NOTE: Final was removed from the declaration of 'SMS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType SMS = new ParsedResultType("SMS");
		//UPGRADE_NOTE: Final was removed from the declaration of 'CALENDAR '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType CALENDAR = new ParsedResultType("CALENDAR");
		// "optional" types
		//UPGRADE_NOTE: Final was removed from the declaration of 'NDEF_SMART_POSTER '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType NDEF_SMART_POSTER = new ParsedResultType("NDEF_SMART_POSTER");
		//UPGRADE_NOTE: Final was removed from the declaration of 'MOBILETAG_RICH_WEB '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType MOBILETAG_RICH_WEB = new ParsedResultType("MOBILETAG_RICH_WEB");
		//UPGRADE_NOTE: Final was removed from the declaration of 'ISBN '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ParsedResultType ISBN = new ParsedResultType("ISBN");
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'name '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String name;
		
		private ParsedResultType(System.String name)
		{
			this.name = name;
		}
		
		public override System.String ToString()
		{
			return name;
		}
	}
}