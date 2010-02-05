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
	
	/// <summary> Enumerates barcode formats known to this package.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>

	public sealed class BarcodeFormat
	{
		public System.String Name
		{
			get
			{
				return name;
			}
			
		}
		
		// No, we can't use an enum here. J2ME doesn't support it.
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'VALUES '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly System.Collections.Hashtable VALUES = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable());
		
		/// <summary>QR Code 2D barcode format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'QR_CODE '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat QR_CODE = new BarcodeFormat("QR_CODE");
		
		/// <summary>DataMatrix 2D barcode format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'DATAMATRIX '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat DATAMATRIX = new BarcodeFormat("DATAMATRIX");
		
		/// <summary>UPC-E 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'UPC_E '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat UPC_E = new BarcodeFormat("UPC_E");
		
		/// <summary>UPC-A 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'UPC_A '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat UPC_A = new BarcodeFormat("UPC_A");
		
		/// <summary>EAN-8 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'EAN_8 '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat EAN_8 = new BarcodeFormat("EAN_8");
		
		/// <summary>EAN-13 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'EAN_13 '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat EAN_13 = new BarcodeFormat("EAN_13");
		
		/// <summary>Code 128 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'CODE_128 '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat CODE_128 = new BarcodeFormat("CODE_128");
		
		/// <summary>Code 39 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'CODE_39 '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat CODE_39 = new BarcodeFormat("CODE_39");
		
		/// <summary>ITF (Interleaved Two of Five) 1D format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'ITF '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat ITF = new BarcodeFormat("ITF");
		
		/// <summary>PDF417 format. </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'PDF417 '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly BarcodeFormat PDF417 = new BarcodeFormat("PDF417");
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'name '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String name;
		
		private BarcodeFormat(System.String name)
		{
			this.name = name;
			VALUES[name] = this;
		}
		
		public override System.String ToString()
		{
			return name;
		}
		
		public static BarcodeFormat valueOf(System.String name)
		{
			BarcodeFormat format = (BarcodeFormat) VALUES[name];
			if (format == null)
			{
				throw new System.ArgumentException();
			}
			return format;
		}
	}
}