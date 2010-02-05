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
using ErrorCorrectionLevel = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
namespace com.google.zxing.common
{
	
	/// <summary> <p>Encapsulates the result of decoding a matrix of bits. This typically
	/// applies to 2D barcode formats. For now it contains the raw bytes obtained,
	/// as well as a String interpretation of those bytes, if applicable.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class DecoderResult
	{
		public sbyte[] RawBytes
		{
			get
			{
				return rawBytes;
			}
			
		}
		public System.String Text
		{
			get
			{
				return text;
			}
			
		}
		public System.Collections.ArrayList ByteSegments
		{
			get
			{
				return byteSegments;
			}
			
		}
		public ErrorCorrectionLevel ECLevel
		{
			get
			{
				return ecLevel;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'rawBytes '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private sbyte[] rawBytes;
		//UPGRADE_NOTE: Final was removed from the declaration of 'text '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String text;
		//UPGRADE_NOTE: Final was removed from the declaration of 'byteSegments '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.Collections.ArrayList byteSegments;
		//UPGRADE_NOTE: Final was removed from the declaration of 'ecLevel '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ErrorCorrectionLevel ecLevel;
		
		public DecoderResult(sbyte[] rawBytes, System.String text, System.Collections.ArrayList byteSegments, ErrorCorrectionLevel ecLevel)
		{
			if (rawBytes == null && text == null)
			{
				throw new System.ArgumentException();
			}
			this.rawBytes = rawBytes;
			this.text = text;
			this.byteSegments = byteSegments;
			this.ecLevel = ecLevel;
		}
	}
}