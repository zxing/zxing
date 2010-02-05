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
using ByteMatrix = com.google.zxing.common.ByteMatrix;
using ErrorCorrectionLevel = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
using Mode = com.google.zxing.qrcode.decoder.Mode;
namespace com.google.zxing.qrcode.encoder
{
	
	/// <author>  satorux@google.com (Satoru Takabayashi) - creator
	/// </author>
	/// <author>  dswitkin@google.com (Daniel Switkin) - ported from C++
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class QRCode
	{
		public Mode Mode
		{
			// Mode of the QR Code.
			
			get
			{
				return mode;
			}
			
			set
			{
				mode = value;
			}
			
		}
		public ErrorCorrectionLevel ECLevel
		{
			// Error correction level of the QR Code.
			
			get
			{
				return ecLevel;
			}
			
			set
			{
				ecLevel = value;
			}
			
		}
		public int Version
		{
			// Version of the QR Code.  The bigger size, the bigger version.
			
			get
			{
				return version;
			}
			
			set
			{
				version = value;
			}
			
		}
		public int MatrixWidth
		{
			// ByteMatrix width of the QR Code.
			
			get
			{
				return matrixWidth;
			}
			
			set
			{
				matrixWidth = value;
			}
			
		}
		public int MaskPattern
		{
			// Mask pattern of the QR Code.
			
			get
			{
				return maskPattern;
			}
			
			set
			{
				maskPattern = value;
			}
			
		}
		public int NumTotalBytes
		{
			// Number of total bytes in the QR Code.
			
			get
			{
				return numTotalBytes;
			}
			
			set
			{
				numTotalBytes = value;
			}
			
		}
		public int NumDataBytes
		{
			// Number of data bytes in the QR Code.
			
			get
			{
				return numDataBytes;
			}
			
			set
			{
				numDataBytes = value;
			}
			
		}
		public int NumECBytes
		{
			// Number of error correction bytes in the QR Code.
			
			get
			{
				return numECBytes;
			}
			
			set
			{
				numECBytes = value;
			}
			
		}
		public int NumRSBlocks
		{
			// Number of Reedsolomon blocks in the QR Code.
			
			get
			{
				return numRSBlocks;
			}
			
			set
			{
				numRSBlocks = value;
			}
			
		}
		public ByteMatrix Matrix
		{
			// ByteMatrix data of the QR Code.
			
			get
			{
				return matrix;
			}
			
			// This takes ownership of the 2D array.
			
			set
			{
				matrix = value;
			}
			
		}
		public bool Valid
		{
			// Checks all the member variables are set properly. Returns true on success. Otherwise, returns
			// false.
			
			get
			{
				return mode != null && ecLevel != null && version != - 1 && matrixWidth != - 1 && maskPattern != - 1 && numTotalBytes != - 1 && numDataBytes != - 1 && numECBytes != - 1 && numRSBlocks != - 1 && isValidMaskPattern(maskPattern) && numTotalBytes == numDataBytes + numECBytes && matrix != null && matrixWidth == matrix.Width && matrix.Width == matrix.Height; // Must be square.
			}
			
		}
		
		public const int NUM_MASK_PATTERNS = 8;
		
		private Mode mode;
		private ErrorCorrectionLevel ecLevel;
		private int version;
		private int matrixWidth;
		private int maskPattern;
		private int numTotalBytes;
		private int numDataBytes;
		private int numECBytes;
		private int numRSBlocks;
		private ByteMatrix matrix;
		
		public QRCode()
		{
			mode = null;
			ecLevel = null;
			version = - 1;
			matrixWidth = - 1;
			maskPattern = - 1;
			numTotalBytes = - 1;
			numDataBytes = - 1;
			numECBytes = - 1;
			numRSBlocks = - 1;
			matrix = null;
		}
		
		
		// Return the value of the module (cell) pointed by "x" and "y" in the matrix of the QR Code. They
		// call cells in the matrix "modules". 1 represents a black cell, and 0 represents a white cell.
		public int at(int x, int y)
		{
			// The value must be zero or one.
			int value_Renamed = matrix.get_Renamed(x, y);
			if (!(value_Renamed == 0 || value_Renamed == 1))
			{
				// this is really like an assert... not sure what better exception to use?
				throw new System.SystemException("Bad value");
			}
			return value_Renamed;
		}
		
		// Return debug String.
		public override System.String ToString()
		{
			System.Text.StringBuilder result = new System.Text.StringBuilder(200);
			result.Append("<<\n");
			result.Append(" mode: ");
			result.Append(mode);
			result.Append("\n ecLevel: ");
			result.Append(ecLevel);
			result.Append("\n version: ");
			result.Append(version);
			result.Append("\n matrixWidth: ");
			result.Append(matrixWidth);
			result.Append("\n maskPattern: ");
			result.Append(maskPattern);
			result.Append("\n numTotalBytes: ");
			result.Append(numTotalBytes);
			result.Append("\n numDataBytes: ");
			result.Append(numDataBytes);
			result.Append("\n numECBytes: ");
			result.Append(numECBytes);
			result.Append("\n numRSBlocks: ");
			result.Append(numRSBlocks);
			if (matrix == null)
			{
				result.Append("\n matrix: null\n");
			}
			else
			{
				result.Append("\n matrix:\n");
				result.Append(matrix.ToString());
			}
			result.Append(">>\n");
			return result.ToString();
		}
		
		// Check if "mask_pattern" is valid.
		public static bool isValidMaskPattern(int maskPattern)
		{
			return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
		}
		
		// Return true if the all values in the matrix are binary numbers.
		//
		// JAVAPORT: This is going to be super expensive and unnecessary, we should not call this in
		// production. I'm leaving it because it may be useful for testing. It should be removed entirely
		// if ByteMatrix is changed never to contain a -1.
		/*
		private static boolean EverythingIsBinary(final ByteMatrix matrix) {
		for (int y = 0; y < matrix.height(); ++y) {
		for (int x = 0; x < matrix.width(); ++x) {
		int value = matrix.get(y, x);
		if (!(value == 0 || value == 1)) {
		// Found non zero/one value.
		return false;
		}
		}
		}
		return true;
		}
		*/
	}
}