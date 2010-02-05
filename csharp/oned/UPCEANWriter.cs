/*
* Copyright 2009 ZXing authors
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
using BarcodeFormat = com.google.zxing.BarcodeFormat;
using Writer = com.google.zxing.Writer;
using WriterException = com.google.zxing.WriterException;
using ByteMatrix = com.google.zxing.common.ByteMatrix;
namespace com.google.zxing.oned
{
	
	/// <summary> <p>Encapsulates functionality and implementation that is common to UPC and EAN families
	/// of one-dimensional barcodes.</p>
	/// 
	/// </summary>
	/// <author>  aripollak@gmail.com (Ari Pollak)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public abstract class UPCEANWriter : Writer
	{
		
		public virtual ByteMatrix encode(System.String contents, BarcodeFormat format, int width, int height)
		{
			return encode(contents, format, width, height, null);
		}
		
		public virtual ByteMatrix encode(System.String contents, BarcodeFormat format, int width, int height, System.Collections.Hashtable hints)
		{
			if (contents == null || contents.Length == 0)
			{
				throw new System.ArgumentException("Found empty contents");
			}
			
			if (width < 0 || height < 0)
			{
				throw new System.ArgumentException("Requested dimensions are too small: " + width + 'x' + height);
			}
			
			sbyte[] code = encode(contents);
			return renderResult(code, width, height);
		}
		
		/// <returns> a byte array of horizontal pixels (0 = white, 1 = black) 
		/// </returns>
		private static ByteMatrix renderResult(sbyte[] code, int width, int height)
		{
			int inputWidth = code.Length;
			// Add quiet zone on both sides
			int fullWidth = inputWidth + (UPCEANReader.START_END_PATTERN.Length << 1);
			int outputWidth = System.Math.Max(width, fullWidth);
			int outputHeight = System.Math.Max(1, height);
			
			int multiple = outputWidth / fullWidth;
			int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
			
			ByteMatrix output = new ByteMatrix(outputWidth, outputHeight);
			sbyte[][] outputArray = output.Array;
			
			sbyte[] row = new sbyte[outputWidth];
			
			// a. Write the white pixels at the left of each row
			for (int x = 0; x < leftPadding; x++)
			{
				row[x] = (sbyte) SupportClass.Identity(255);
			}
			
			// b. Write the contents of this row of the barcode
			int offset = leftPadding;
			for (int x = 0; x < inputWidth; x++)
			{
                // Redivivus.in Java to c# Porting update
                // 30/01/2010 
                // type cased 0 with sbyte
                sbyte value_Renamed = (code[x] == 1) ? (sbyte)0 : (sbyte)SupportClass.Identity(255);
				for (int z = 0; z < multiple; z++)
				{
					row[offset + z] = value_Renamed;
				}
				offset += multiple;
			}
			
			// c. Write the white pixels at the right of each row
			offset = leftPadding + (inputWidth * multiple);
			for (int x = offset; x < outputWidth; x++)
			{
				row[x] = (sbyte) SupportClass.Identity(255);
			}
			
			// d. Write the completed row multiple times
			for (int z = 0; z < outputHeight; z++)
			{
				Array.Copy(row, 0, outputArray[z], 0, outputWidth);
			}
			
			return output;
		}
		
		
		/// <summary> Appends the given pattern to the target array starting at pos.
		/// 
		/// </summary>
		/// <param name="startColor">starting color - 0 for white, 1 for black
		/// </param>
		/// <returns> the number of elements added to target.
		/// </returns>
		protected internal static int appendPattern(sbyte[] target, int pos, int[] pattern, int startColor)
		{
			if (startColor != 0 && startColor != 1)
			{
				throw new System.ArgumentException("startColor must be either 0 or 1, but got: " + startColor);
			}
			
			sbyte color = (sbyte) startColor;
			int numAdded = 0;
			for (int i = 0; i < pattern.Length; i++)
			{
				for (int j = 0; j < pattern[i]; j++)
				{
					target[pos] = color;
					pos += 1;
					numAdded += 1;
				}
				color ^= 1; // flip color after each segment
			}
			return numAdded;
		}
		
		/// <returns> a byte array of horizontal pixels (0 = white, 1 = black) 
		/// </returns>
		public abstract sbyte[] encode(System.String contents);
	}
}