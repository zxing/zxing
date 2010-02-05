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
	public sealed class AddressBookParsedResult:ParsedResult
	{
		public System.String[] Names
		{
			get
			{
				return names;
			}
			
		}
		/// <summary> In Japanese, the name is written in kanji, which can have multiple readings. Therefore a hint
		/// is often provided, called furigana, which spells the name phonetically.
		/// 
		/// </summary>
		/// <returns> The pronunciation of the getNames() field, often in hiragana or katakana.
		/// </returns>
		public System.String Pronunciation
		{
			get
			{
				return pronunciation;
			}
			
		}
		public System.String[] PhoneNumbers
		{
			get
			{
				return phoneNumbers;
			}
			
		}
		public System.String[] Emails
		{
			get
			{
				return emails;
			}
			
		}
		public System.String Note
		{
			get
			{
				return note;
			}
			
		}
		public System.String[] Addresses
		{
			get
			{
				return addresses;
			}
			
		}
		public System.String Title
		{
			get
			{
				return title;
			}
			
		}
		public System.String Org
		{
			get
			{
				return org;
			}
			
		}
		public System.String URL
		{
			get
			{
				return url;
			}
			
		}
		/// <returns> birthday formatted as yyyyMMdd (e.g. 19780917)
		/// </returns>
		public System.String Birthday
		{
			get
			{
				return birthday;
			}
			
		}
		override public System.String DisplayResult
		{
			get
			{
				System.Text.StringBuilder result = new System.Text.StringBuilder(100);
				maybeAppend(names, result);
				maybeAppend(pronunciation, result);
				maybeAppend(title, result);
				maybeAppend(org, result);
				maybeAppend(addresses, result);
				maybeAppend(phoneNumbers, result);
				maybeAppend(emails, result);
				maybeAppend(url, result);
				maybeAppend(birthday, result);
				maybeAppend(note, result);
				return result.ToString();
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'names '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String[] names;
		//UPGRADE_NOTE: Final was removed from the declaration of 'pronunciation '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String pronunciation;
		//UPGRADE_NOTE: Final was removed from the declaration of 'phoneNumbers '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String[] phoneNumbers;
		//UPGRADE_NOTE: Final was removed from the declaration of 'emails '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String[] emails;
		//UPGRADE_NOTE: Final was removed from the declaration of 'note '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String note;
		//UPGRADE_NOTE: Final was removed from the declaration of 'addresses '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String[] addresses;
		//UPGRADE_NOTE: Final was removed from the declaration of 'org '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String org;
		//UPGRADE_NOTE: Final was removed from the declaration of 'birthday '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String birthday;
		//UPGRADE_NOTE: Final was removed from the declaration of 'title '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String title;
		//UPGRADE_NOTE: Final was removed from the declaration of 'url '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String url;
		
		public AddressBookParsedResult(System.String[] names, System.String pronunciation, System.String[] phoneNumbers, System.String[] emails, System.String note, System.String[] addresses, System.String org, System.String birthday, System.String title, System.String url):base(ParsedResultType.ADDRESSBOOK)
		{
			this.names = names;
			this.pronunciation = pronunciation;
			this.phoneNumbers = phoneNumbers;
			this.emails = emails;
			this.note = note;
			this.addresses = addresses;
			this.org = org;
			this.birthday = birthday;
			this.title = title;
			this.url = url;
		}
	}
}