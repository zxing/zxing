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
namespace com.google.zxing
{
	
	/// <summary> These are a set of hints that you may pass to Writers to specify their behavior.
	/// 
	/// </summary>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>

	public sealed class EncodeHintType
	{
		
		/// <summary> Specifies what degree of error correction to use, for example in QR Codes (type Integer).</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'ERROR_CORRECTION '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly EncodeHintType ERROR_CORRECTION = new EncodeHintType();
		
		/// <summary> Specifies what character encoding to use where applicable (type String)</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'CHARACTER_SET '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly EncodeHintType CHARACTER_SET = new EncodeHintType();
		
		private EncodeHintType()
		{
		}
	}
}