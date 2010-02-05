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
	
	/// <summary> <p>A simple, fast array of bits, represented compactly by an array of ints internally.</p>
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class BitArray
	{
		public int Size
		{
			get
			{
				return size;
			}
			
		}
		
		// TODO: I have changed these members to be public so ProGuard can inline get() and set(). Ideally
		// they'd be private and we'd use the -allowaccessmodification flag, but Dalvik rejects the
		// resulting binary at runtime on Android. If we find a solution to this, these should be changed
		// back to private.
		public int[] bits;
		//UPGRADE_NOTE: Final was removed from the declaration of 'size '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		public int size;
		
		public BitArray(int size)
		{
			if (size < 1)
			{
				throw new System.ArgumentException("size must be at least 1");
			}
			this.size = size;
			this.bits = makeArray(size);
		}
		
		/// <param name="i">bit to get
		/// </param>
		/// <returns> true iff bit i is set
		/// </returns>
		public bool get_Renamed(int i)
		{
			return (bits[i >> 5] & (1 << (i & 0x1F))) != 0;
		}
		
		/// <summary> Sets bit i.
		/// 
		/// </summary>
		/// <param name="i">bit to set
		/// </param>
		public void  set_Renamed(int i)
		{
			bits[i >> 5] |= 1 << (i & 0x1F);
		}
		
		/// <summary> Flips bit i.
		/// 
		/// </summary>
		/// <param name="i">bit to set
		/// </param>
		public void  flip(int i)
		{
			bits[i >> 5] ^= 1 << (i & 0x1F);
		}
		
		/// <summary> Sets a block of 32 bits, starting at bit i.
		/// 
		/// </summary>
		/// <param name="i">first bit to set
		/// </param>
		/// <param name="newBits">the new value of the next 32 bits. Note again that the least-significant bit
		/// corresponds to bit i, the next-least-significant to i+1, and so on.
		/// </param>
		public void  setBulk(int i, int newBits)
		{
			bits[i >> 5] = newBits;
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
		
		/// <summary> Efficient method to check if a range of bits is set, or not set.
		/// 
		/// </summary>
		/// <param name="start">start of range, inclusive.
		/// </param>
		/// <param name="end">end of range, exclusive
		/// </param>
		/// <param name="value">if true, checks that bits in range are set, otherwise checks that they are not set
		/// </param>
		/// <returns> true iff all bits are set or not set in range, according to value argument
		/// </returns>
		/// <throws>  IllegalArgumentException if end is less than or equal to start </throws>
		public bool isRange(int start, int end, bool value_Renamed)
		{
			if (end < start)
			{
				throw new System.ArgumentException();
			}
			if (end == start)
			{
				return true; // empty range matches
			}
			end--; // will be easier to treat this as the last actually set bit -- inclusive    
			int firstInt = start >> 5;
			int lastInt = end >> 5;
			for (int i = firstInt; i <= lastInt; i++)
			{
				int firstBit = i > firstInt?0:start & 0x1F;
				int lastBit = i < lastInt?31:end & 0x1F;
				int mask;
				if (firstBit == 0 && lastBit == 31)
				{
					mask = - 1;
				}
				else
				{
					mask = 0;
					for (int j = firstBit; j <= lastBit; j++)
					{
						mask |= 1 << j;
					}
				}
				
				// Return false if we're looking for 1s and the masked bits[i] isn't all 1s (that is,
				// equals the mask, or we're looking for 0s and the masked portion is not all 0s
				if ((bits[i] & mask) != (value_Renamed?mask:0))
				{
					return false;
				}
			}
			return true;
		}
		
		/// <returns> underlying array of ints. The first element holds the first 32 bits, and the least
		/// significant bit is bit 0.
		/// </returns>
		public int[] getBitArray()
		{
			return bits;
		}
		
		/// <summary> Reverses all bits in the array.</summary>
		public void  reverse()
		{
			int[] newBits = new int[bits.Length];
			int size = this.size;
			for (int i = 0; i < size; i++)
			{
				if (get_Renamed(size - i - 1))
				{
					newBits[i >> 5] |= 1 << (i & 0x1F);
				}
			}
			bits = newBits;
		}
		
		private static int[] makeArray(int size)
		{
			int arraySize = size >> 5;
			if ((size & 0x1F) != 0)
			{
				arraySize++;
			}
			return new int[arraySize];
		}
		
		public override System.String ToString()
		{
			System.Text.StringBuilder result = new System.Text.StringBuilder(size);
			for (int i = 0; i < size; i++)
			{
				if ((i & 0x07) == 0)
				{
					result.Append(' ');
				}
				result.Append(get_Renamed(i)?'X':'.');
			}
			return result.ToString();
		}
	}
}