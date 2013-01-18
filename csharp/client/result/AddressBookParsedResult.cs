using System.Text;

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

	/// <summary>
	/// @author Sean Owen
	/// </summary>
	public sealed class AddressBookParsedResult : ParsedResult
	{

	  private readonly string[] names;
	  private readonly string pronunciation;
	  private readonly string[] phoneNumbers;
	  private readonly string[] phoneTypes;
	  private readonly string[] emails;
	  private readonly string[] emailTypes;
	  private readonly string instantMessenger;
	  private readonly string note;
	  private readonly string[] addresses;
	  private readonly string[] addressTypes;
	  private readonly string org;
	  private readonly string birthday;
	  private readonly string title;
	  private readonly string url;

	  public AddressBookParsedResult(string[] names, string pronunciation, string[] phoneNumbers, string[] phoneTypes, string[] emails, string[] emailTypes, string instantMessenger, string note, string[] addresses, string[] addressTypes, string org, string birthday, string title, string url) : base(ParsedResultType.ADDRESSBOOK)
	  {
		this.names = names;
		this.pronunciation = pronunciation;
		this.phoneNumbers = phoneNumbers;
		this.phoneTypes = phoneTypes;
		this.emails = emails;
		this.emailTypes = emailTypes;
		this.instantMessenger = instantMessenger;
		this.note = note;
		this.addresses = addresses;
		this.addressTypes = addressTypes;
		this.org = org;
		this.birthday = birthday;
		this.title = title;
		this.url = url;
	  }

	  public string[] Names
	  {
		  get
		  {
			return names;
		  }
	  }

	  /// <summary>
	  /// In Japanese, the name is written in kanji, which can have multiple readings. Therefore a hint
	  /// is often provided, called furigana, which spells the name phonetically.
	  /// </summary>
	  /// <returns> The pronunciation of the getNames() field, often in hiragana or katakana. </returns>
	  public string Pronunciation
	  {
		  get
		  {
			return pronunciation;
		  }
	  }

	  public string[] PhoneNumbers
	  {
		  get
		  {
			return phoneNumbers;
		  }
	  }

	  /// <returns> optional descriptions of the type of each phone number. It could be like "HOME", but,
	  ///  there is no guaranteed or standard format. </returns>
	  public string[] PhoneTypes
	  {
		  get
		  {
			return phoneTypes;
		  }
	  }

	  public string[] Emails
	  {
		  get
		  {
			return emails;
		  }
	  }

	  /// <returns> optional descriptions of the type of each e-mail. It could be like "WORK", but,
	  ///  there is no guaranteed or standard format. </returns>
	  public string[] EmailTypes
	  {
		  get
		  {
			return emailTypes;
		  }
	  }

	  public string InstantMessenger
	  {
		  get
		  {
			return instantMessenger;
		  }
	  }

	  public string Note
	  {
		  get
		  {
			return note;
		  }
	  }

	  public string[] Addresses
	  {
		  get
		  {
			return addresses;
		  }
	  }

	  /// <returns> optional descriptions of the type of each e-mail. It could be like "WORK", but,
	  ///  there is no guaranteed or standard format. </returns>
	  public string[] AddressTypes
	  {
		  get
		  {
			return addressTypes;
		  }
	  }

	  public string Title
	  {
		  get
		  {
			return title;
		  }
	  }

	  public string Org
	  {
		  get
		  {
			return org;
		  }
	  }

	  public string URL
	  {
		  get
		  {
			return url;
		  }
	  }

	  /// <returns> birthday formatted as yyyyMMdd (e.g. 19780917) </returns>
	  public string Birthday
	  {
		  get
		  {
			return birthday;
		  }
	  }

	  public override string DisplayResult
	  {
		  get
		  {
			StringBuilder result = new StringBuilder(100);
			maybeAppend(names, result);
			maybeAppend(pronunciation, result);
			maybeAppend(title, result);
			maybeAppend(org, result);
			maybeAppend(addresses, result);
			maybeAppend(phoneNumbers, result);
			maybeAppend(emails, result);
			maybeAppend(instantMessenger, result);
			maybeAppend(url, result);
			maybeAppend(birthday, result);
			maybeAppend(note, result);
			return result.ToString();
		  }
	  }

	}

}