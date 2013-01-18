using System;
using System.Collections;
using System.Text;

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

namespace com.google.zxing.common
{

	/// <summary>
	/// <p>Represents a 2D matrix of bits. In function arguments below, and throughout the common
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
	/// @author Sean Owen
	/// @author dswitkin@google.com (Daniel Switkin)
	/// </summary>
	public sealed class BitMatrix
	{

	  private readonly int width;
	  private readonly int height;
	  private readonly int rowSize;
	  private readonly int[] bits;

	  // A helper to construct a square matrix.
	  public BitMatrix(int dimension) : this(dimension, dimension)
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
		this.rowSize = (width + 31) >> 5;
		bits = new int[rowSize * height];
	  }

	  /// <summary>
	  /// <p>Gets the requested bit, where true means black.</p>
	  /// </summary>
	  /// <param name="x"> The horizontal component (i.e. which column) </param>
	  /// <param name="y"> The vertical component (i.e. which row) </param>
	  /// <returns> value of given bit in matrix </returns>
	  public bool get(int x, int y)
	  {
		int offset = y * rowSize + (x >> 5);
		return (((int)((uint)bits[offset] >> (x & 0x1f))) & 1) != 0;
	  }

	  /// <summary>
	  /// <p>Sets the given bit to true.</p>
	  /// </summary>
	  /// <param name="x"> The horizontal component (i.e. which column) </param>
	  /// <param name="y"> The vertical component (i.e. which row) </param>
	  public void set(int x, int y)
	  {
		int offset = y * rowSize + (x >> 5);
		bits[offset] |= 1 << (x & 0x1f);
	  }

	  /// <summary>
	  /// <p>Flips the given bit.</p>
	  /// </summary>
	  /// <param name="x"> The horizontal component (i.e. which column) </param>
	  /// <param name="y"> The vertical component (i.e. which row) </param>
	  public void flip(int x, int y)
	  {
		int offset = y * rowSize + (x >> 5);
		bits[offset] ^= 1 << (x & 0x1f);
	  }

	  /// <summary>
	  /// Clears all bits (sets to false).
	  /// </summary>
	  public void clear()
	  {
		int max = bits.Length;
		for (int i = 0; i < max; i++)
		{
		  bits[i] = 0;
		}
	  }

	  /// <summary>
	  /// <p>Sets a square region of the bit matrix to true.</p>
	  /// </summary>
	  /// <param name="left"> The horizontal position to begin at (inclusive) </param>
	  /// <param name="top"> The vertical position to begin at (inclusive) </param>
	  /// <param name="width"> The width of the region </param>
	  /// <param name="height"> The height of the region </param>
	  public void setRegion(int left, int top, int width, int height)
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

	  /// <summary>
	  /// A fast method to retrieve one row of data from the matrix as a BitArray.
	  /// </summary>
	  /// <param name="y"> The row to retrieve </param>
	  /// <param name="row"> An optional caller-allocated BitArray, will be allocated if null or too small </param>
	  /// <returns> The resulting BitArray - this reference should always be used even when passing
	  ///         your own row </returns>
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

	  /// <param name="y"> row to set </param>
	  /// <param name="row"> <seealso cref="BitArray"/> to copy from </param>
	  public void setRow(int y, BitArray row)
	  {
		Array.Copy(row.BitArrayBits, 0, bits, y * rowSize, rowSize);
	  }

	  /// <summary>
	  /// This is useful in detecting the enclosing rectangle of a 'pure' barcode.
	  /// </summary>
	  /// <returns> {left,top,width,height} enclosing rectangle of all 1 bits, or null if it is all white </returns>
	  public int[] EnclosingRectangle
	  {
		  get
		  {
			int left = this.width;
			int top = this.height;
			int right = -1;
			int bottom = -1;
    
			for (int y = 0; y < this.height; y++)
			{
			  for (int x32 = 0; x32 < rowSize; x32++)
			  {
				int theBits = bits[y * rowSize + x32];
				if (theBits != 0)
				{
				  if (y < top)
				  {
					top = y;
				  }
				  if (y > bottom)
				  {
					bottom = y;
				  }
				  if (x32 * 32 < left)
				  {
					int bit = 0;
					while ((theBits << (31 - bit)) == 0)
					{
					  bit++;
					}
					if ((x32 * 32 + bit) < left)
					{
					  left = x32 * 32 + bit;
					}
				  }
				  if (x32 * 32 + 31 > right)
				  {
					int bit = 31;
					while (((int)((uint)theBits >> bit)) == 0)
					{
					  bit--;
					}
					if ((x32 * 32 + bit) > right)
					{
					  right = x32 * 32 + bit;
					}
				  }
				}
			  }
			}
    
			int width = right - left;
			int height = bottom - top;
    
			if (width < 0 || height < 0)
			{
			  return null;
			}
    
			return new int[] {left, top, width, height};
		  }
	  }

	  /// <summary>
	  /// This is useful in detecting a corner of a 'pure' barcode.
	  /// </summary>
	  /// <returns> {x,y} coordinate of top-left-most 1 bit, or null if it is all white </returns>
	  public int[] TopLeftOnBit
	  {
		  get
		  {
			int bitsOffset = 0;
			while (bitsOffset < bits.Length && bits[bitsOffset] == 0)
			{
			  bitsOffset++;
			}
			if (bitsOffset == bits.Length)
			{
			  return null;
			}
			int y = bitsOffset / rowSize;
			int x = (bitsOffset % rowSize) << 5;
    
			int theBits = bits[bitsOffset];
			int bit = 0;
			while ((theBits << (31 - bit)) == 0)
			{
			  bit++;
			}
			x += bit;
			return new int[] {x, y};
		  }
	  }

	  public int[] BottomRightOnBit
	  {
		  get
		  {
			int bitsOffset = bits.Length - 1;
			while (bitsOffset >= 0 && bits[bitsOffset] == 0)
			{
			  bitsOffset--;
			}
			if (bitsOffset < 0)
			{
			  return null;
			}
    
			int y = bitsOffset / rowSize;
			int x = (bitsOffset % rowSize) << 5;
    
			int theBits = bits[bitsOffset];
			int bit = 31;
			while (((int)((uint)theBits >> bit)) == 0)
			{
			  bit--;
			}
			x += bit;
    
			return new int[] {x, y};
		  }
	  }

	  /// <returns> The width of the matrix </returns>
	  public int Width
	  {
		  get
		  {
			return width;
		  }
	  }

	  /// <returns> The height of the matrix </returns>
	  public int Height
	  {
		  get
		  {
			return height;
		  }
	  }

	  public override bool Equals(object o)
	  {
		if (!(o is BitMatrix))
		{
		  return false;
		}
		BitMatrix other = (BitMatrix) o;
		if (width != other.width || height != other.height || rowSize != other.rowSize || bits.Length != other.bits.Length)
		{
		  return false;
		}
		for (int i = 0; i < bits.Length; i++)
		{
		  if (bits[i] != other.bits[i])
		  {
			return false;
		  }
		}
		return true;
	  }

	  public override int GetHashCode()
	  {
		int hash = width;
		hash = 31 * hash + width;
		hash = 31 * hash + height;
		hash = 31 * hash + rowSize;
		foreach (int bit in bits)
		{
		  hash = 31 * hash + bit;
		}
		return hash;
	  }

	  public override string ToString()
	  {
		StringBuilder result = new StringBuilder(height * (width + 1));
		for (int y = 0; y < height; y++)
		{
		  for (int x = 0; x < width; x++)
		  {
			result.Append(get(x, y) ? "X " : "  ");
		  }
		  result.Append('\n');
		}
		return result.ToString();
	  }

	}

}