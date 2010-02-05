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
	
	/// <summary> Represents some type of metadata about the result of the decoding that the decoder
	/// wishes to communicate back to the caller.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>

	public sealed class ResultMetadataType
	{
		
		// No, we can't use an enum here. J2ME doesn't support it.
		
		/// <summary> Unspecified, application-specific metadata. Maps to an unspecified {@link Object}.</summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'OTHER '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ResultMetadataType OTHER = new ResultMetadataType();
		
		/// <summary> Denotes the likely approximate orientation of the barcode in the image. This value
		/// is given as degrees rotated clockwise from the normal, upright orientation.
		/// For example a 1D barcode which was found by reading top-to-bottom would be
		/// said to have orientation "90". This key maps to an {@link Integer} whose
		/// value is in the range [0,360).
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'ORIENTATION '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ResultMetadataType ORIENTATION = new ResultMetadataType();
		
		/// <summary> <p>2D barcode formats typically encode text, but allow for a sort of 'byte mode'
		/// which is sometimes used to encode binary data. While {@link Result} makes available
		/// the complete raw bytes in the barcode for these formats, it does not offer the bytes
		/// from the byte segments alone.</p>
		/// 
		/// <p>This maps to a {@link java.util.Vector} of byte arrays corresponding to the
		/// raw bytes in the byte segments in the barcode, in order.</p>
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'BYTE_SEGMENTS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ResultMetadataType BYTE_SEGMENTS = new ResultMetadataType();
		
		/// <summary> Error correction level used, if applicable. The value type depends on the
		/// format, but is typically a String.
		/// </summary>
		//UPGRADE_NOTE: Final was removed from the declaration of 'ERROR_CORRECTION_LEVEL '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public static readonly ResultMetadataType ERROR_CORRECTION_LEVEL = new ResultMetadataType();
		
		private ResultMetadataType()
		{
		}
	}
}