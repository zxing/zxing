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
namespace com.google.zxing
{
	
	/// <summary> <p>Encapsulates the result of decoding a barcode within an image.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>

	public sealed class Result
	{
		/// <returns> raw text encoded by the barcode, if applicable, otherwise <code>null</code>
		/// </returns>
		public System.String Text
		{
			get
			{
				return text;
			}
			
		}
		/// <returns> raw bytes encoded by the barcode, if applicable, otherwise <code>null</code>
		/// </returns>
		public sbyte[] RawBytes
		{
			get
			{
				return rawBytes;
			}
			
		}
		/// <returns> points related to the barcode in the image. These are typically points
		/// identifying finder patterns or the corners of the barcode. The exact meaning is
		/// specific to the type of barcode that was decoded.
		/// </returns>
		public ResultPoint[] ResultPoints
		{
			get
			{
				return resultPoints;
			}
			
		}
		/// <returns> {@link BarcodeFormat} representing the format of the barcode that was decoded
		/// </returns>
		public BarcodeFormat BarcodeFormat
		{
			get
			{
				return format;
			}
			
		}
		/// <returns> {@link Hashtable} mapping {@link ResultMetadataType} keys to values. May be
		/// <code>null</code>. This contains optional metadata about what was detected about the barcode,
		/// like orientation.
		/// </returns>
		public System.Collections.Hashtable ResultMetadata
		{
			get
			{
				return resultMetadata;
			}
			
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'text '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String text;
		//UPGRADE_NOTE: Final was removed from the declaration of 'rawBytes '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private sbyte[] rawBytes;
		//UPGRADE_NOTE: Final was removed from the declaration of 'resultPoints '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private ResultPoint[] resultPoints;
		//UPGRADE_NOTE: Final was removed from the declaration of 'format '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private BarcodeFormat format;
		private System.Collections.Hashtable resultMetadata;
		
		public Result(System.String text, sbyte[] rawBytes, ResultPoint[] resultPoints, BarcodeFormat format)
		{
			if (text == null && rawBytes == null)
			{
				throw new System.ArgumentException("Text and bytes are null");
			}
			this.text = text;
			this.rawBytes = rawBytes;
			this.resultPoints = resultPoints;
			this.format = format;
			this.resultMetadata = null;
		}
		
		public void  putMetadata(ResultMetadataType type, System.Object value_Renamed)
		{
			if (resultMetadata == null)
			{
				resultMetadata = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable(3));
			}
			resultMetadata[type] = value_Renamed;
		}
		
		public override System.String ToString()
		{
			if (text == null)
			{
				return "[" + rawBytes.Length + " bytes]";
			}
			else
			{
				return text;
			}
		}
	}
}