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

	/// <summary>
	/// <p>See ISO 18004:2006, 6.5.1. This enum encapsulates the four error correction levels
	/// defined by the QR code standard.</p>
	/// 
	/// @author Sean Owen
	/// </summary>
	public sealed class ErrorCorrectionLevel
	{

        /// <summary>L = ~7% correction </summary>
        public static readonly ErrorCorrectionLevel L = new ErrorCorrectionLevel(0, 0x01, "L");
        /// <summary>M = ~15% correction </summary>
        public static readonly ErrorCorrectionLevel M = new ErrorCorrectionLevel(1, 0x00, "M");
        /// <summary>Q = ~25% correction </summary>
        public static readonly ErrorCorrectionLevel Q = new ErrorCorrectionLevel(2, 0x03, "Q");
        /// <summary>H = ~30% correction </summary>
        public static readonly ErrorCorrectionLevel H = new ErrorCorrectionLevel(3, 0x02, "H");

	    private static readonly ErrorCorrectionLevel[] FOR_BITS = {M, L, H, Q};

        private int ordinal_Renamed_Field;
        private int bits;
        private System.String name;

        ErrorCorrectionLevel(int ordinal, int bits, System.String name)
        {
            this.ordinal_Renamed_Field = ordinal;
            this.bits = bits;
            this.name = name;
        }

        public int Bits
        {
            get {return bits;}
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
        /// <returns> ErrorCorrectionLevel representing the encoded error correction level
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