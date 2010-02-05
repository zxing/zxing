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
namespace com.google.zxing.common
{
	
	/// <summary> <p>Represents a 2D matrix of bits. In function arguments below, and throughout the common
	/// module, x is the column position, and y is the row position. The ordering is always x, y.
	/// The origin is at the top-left.</p>
	/// 
	/// <p>Internally the bits are represented in a 1-D array of 32-bit ints. However, each row begins
	/// with a new int. This is done intentionally so that we can copy out a row into a BitArray very
	/// efficiently.</p>
	/// 
	/// <p>The ordering of bits is row-major. Within each int, the least significant bits are used first,
	/// meaning they represent lower x values. This is compatible with BitArray's implementation.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class BitMatrix
	{
		/// <returns> The width of the matrix
		/// </returns>
		public int Width
		{
			get
			{
				return width;
			}
			
		}
		/// <returns> The height of the matrix
		/// </returns>
		public int Height
		{
			get
			{
				return height;
			}
			
		}
		/// <summary> This method is for compatibility with older code. It's only logical to call if the matrix
		/// is square, so I'm throwing if that's not the case.
		/// 
		/// </summary>
		/// <returns> row/column dimension of this matrix
		/// </returns>
		public int Dimension
		{
			get
			{
				if (width != height)
				{
					throw new System.SystemException("Can't call getDimension() on a non-square matrix");
				}
				return width;
			}
			
		}
		
		// TODO: Just like BitArray, these need to be public so ProGuard can inline them.
		//UPGRADE_NOTE: Final was removed from the declaration of 'width '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public int width;
		//UPGRADE_NOTE: Final was removed from the declaration of 'height '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public int height;
		//UPGRADE_NOTE: Final was removed from the declaration of 'rowSize '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public int rowSize;
		//UPGRADE_NOTE: Final was removed from the declaration of 'bits '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public int[] bits;
		
		// A helper to construct a square matrix.
		public BitMatrix(int dimension):this(dimension, dimension)
		{
		}
		
		public BitMatrix(int width, int height)
		{
			if (width < 1 || height < 1)
			{
				throw new System.ArgumentException("Both dimensions must be greater than 0");
			}
			this.width = width;
			this.height = height;
			int rowSize = width >> 5;
			if ((width & 0x1f) != 0)
			{
				rowSize++;
			}
			this.rowSize = rowSize;
			bits = new int[rowSize * height];
		}
		
		/// <summary> <p>Gets the requested bit, where true means black.</p>
		/// 
		/// </summary>
		/// <param name="x">The horizontal component (i.e. which column)
		/// </param>
		/// <param name="y">The vertical component (i.e. which row)
		/// </param>
		/// <returns> value of given bit in matrix
		/// </returns>
		public bool get_Renamed(int x, int y)
		{
			int offset = y * rowSize + (x >> 5);
			return ((SupportClass.URShift(bits[offset], (x & 0x1f))) & 1) != 0;
		}
		
		/// <summary> <p>Sets the given bit to true.</p>
		/// 
		/// </summary>
		/// <param name="x">The horizontal component (i.e. which column)
		/// </param>
		/// <param name="y">The vertical component (i.e. which row)
		/// </param>
		public void  set_Renamed(int x, int y)
		{
			int offset = y * rowSize + (x >> 5);
			bits[offset] |= 1 << (x & 0x1f);
		}
		
		/// <summary> <p>Flips the given bit.</p>
		/// 
		/// </summary>
		/// <param name="x">The horizontal component (i.e. which column)
		/// </param>
		/// <param name="y">The vertical component (i.e. which row)
		/// </param>
		public void  flip(int x, int y)
		{
			int offset = y * rowSize + (x >> 5);
			bits[offset] ^= 1 << (x & 0x1f);
		}
		
		/// <summary> Clears all bits (sets to false).</summary>
		public void  clear()
		{
			int max = bits.Length;
			for (int i = 0; i < max; i++)
			{
				bits[i] = 0;
			}
		}
		
		/// <summary> <p>Sets a square region of the bit matrix to true.</p>
		/// 
		/// </summary>
		/// <param name="left">The horizontal position to begin at (inclusive)
		/// </param>
		/// <param name="top">The vertical position to begin at (inclusive)
		/// </param>
		/// <param name="width">The width of the region
		/// </param>
		/// <param name="height">The height of the region
		/// </param>
		public void  setRegion(int left, int top, int width, int height)
		{
			if (top < 0 || left < 0)
			{
				throw new System.ArgumentException("Left and top must be nonnegative");
			}
			if (height < 1 || width < 1)
			{
				throw new System.ArgumentException("Height and width must be at least 1");
			}
			int right = left + width;
			int bottom = top + height;
			if (bottom > this.height || right > this.width)
			{
				throw new System.ArgumentException("The region must fit inside the matrix");
			}
			for (int y = top; y < bottom; y++)
			{
				int offset = y * rowSize;
				for (int x = left; x < right; x++)
				{
					bits[offset + (x >> 5)] |= 1 << (x & 0x1f);
				}
			}
		}
		
		/// <summary> A fast method to retrieve one row of data from the matrix as a BitArray.
		/// 
		/// </summary>
		/// <param name="y">The row to retrieve
		/// </param>
		/// <param name="row">An optional caller-allocated BitArray, will be allocated if null or too small
		/// </param>
		/// <returns> The resulting BitArray - this reference should always be used even when passing
		/// your own row
		/// </returns>
		public BitArray getRow(int y, BitArray row)
		{
			if (row == null || row.Size < width)
			{
				row = new BitArray(width);
			}
			int offset = y * rowSize;
			for (int x = 0; x < rowSize; x++)
			{
				row.setBulk(x << 5, bits[offset + x]);
			}
			return row;
		}
		
		public override System.String ToString()
		{
			System.Text.StringBuilder result = new System.Text.StringBuilder(height * (width + 1));
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					result.Append(get_Renamed(x, y)?"X ":"  ");
				}
				result.Append('\n');
			}
			return result.ToString();
		}
	}
}