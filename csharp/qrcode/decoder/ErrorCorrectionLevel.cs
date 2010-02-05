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
namespace com.google.zxing.qrcode.decoder
{
	
	/// <summary> <p>See ISO 18004:2006, 6.5.1. This enum encapsulates the four error correction levels
	/// defined by the QR code standard.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ErrorCorrectionLevel
	{
		public int Bits
		{
			get
			{
				return bits;
			}
			
		}
		public System.String Name
		{
			get
			{
				return name;
			}
			
		}
		
		// No, we can't use an enum here. J2ME doesn't support it.
		
		/// <summary> L = ~7% correction</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'L '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ErrorCorrectionLevel L = new ErrorCorrectionLevel(0, 0x01, "L");
		/// <summary> M = ~15% correction</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'M '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ErrorCorrectionLevel M = new ErrorCorrectionLevel(1, 0x00, "M");
		/// <summary> Q = ~25% correction</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'Q '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ErrorCorrectionLevel Q = new ErrorCorrectionLevel(2, 0x03, "Q");
		/// <summary> H = ~30% correction</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'H '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ErrorCorrectionLevel H = new ErrorCorrectionLevel(3, 0x02, "H");
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'FOR_BITS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly ErrorCorrectionLevel[] FOR_BITS = new ErrorCorrectionLevel[]{M, L, H, Q};
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'ordinal '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int ordinal_Renamed_Field;
		//UPGRADE_NOTE: Final was removed from the declaration of 'bits '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private int bits;
		//UPGRADE_NOTE: Final was removed from the declaration of 'name '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String name;
		
		private ErrorCorrectionLevel(int ordinal, int bits, System.String name)
		{
			this.ordinal_Renamed_Field = ordinal;
			this.bits = bits;
			this.name = name;
		}
		
		public int ordinal()
		{
			return ordinal_Renamed_Field;
		}
		
		public override System.String ToString()
		{
			return name;
		}
		
		/// <param name="bits">int containing the two bits encoding a QR Code's error correction level
		/// </param>
		/// <returns> {@link ErrorCorrectionLevel} representing the encoded error correction level
		/// </returns>
		public static ErrorCorrectionLevel forBits(int bits)
		{
			if (bits < 0 || bits >= FOR_BITS.Length)
			{
				throw new System.ArgumentException();
			}
			return FOR_BITS[bits];
		}
	}
}