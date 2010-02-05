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
namespace com.google.zxing.common
{
	
	/// <summary> This class implements an array of unsigned bytes.
	/// 
	/// </summary>
	/// <author>  dswitkin@google.com (Daniel Switkin)
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class ByteArray
	{
		public bool Empty
		{
			get
			{
				return size_Renamed_Field == 0;
			}
			
		}
		
		private const int INITIAL_SIZE = 32;
		
		private sbyte[] bytes;
		private int size_Renamed_Field;
		
		public ByteArray()
		{
			bytes = null;
			size_Renamed_Field = 0;
		}
		
		public ByteArray(int size)
		{
			bytes = new sbyte[size];
			this.size_Renamed_Field = size;
		}
		
		public ByteArray(sbyte[] byteArray)
		{
			bytes = byteArray;
			size_Renamed_Field = bytes.Length;
		}
		
		/// <summary> Access an unsigned byte at location index.</summary>
		/// <param name="index">The index in the array to access.
		/// </param>
		/// <returns> The unsigned value of the byte as an int.
		/// </returns>
		public int at(int index)
		{
			return bytes[index] & 0xff;
		}
		
		public void  set_Renamed(int index, int value_Renamed)
		{
			bytes[index] = (sbyte) value_Renamed;
		}
		
		public int size()
		{
			return size_Renamed_Field;
		}
		
		public void  appendByte(int value_Renamed)
		{
			if (size_Renamed_Field == 0 || size_Renamed_Field >= bytes.Length)
			{
				int newSize = System.Math.Max(INITIAL_SIZE, size_Renamed_Field << 1);
				reserve(newSize);
			}
			bytes[size_Renamed_Field] = (sbyte) value_Renamed;
			size_Renamed_Field++;
		}
		
		public void  reserve(int capacity)
		{
			if (bytes == null || bytes.Length < capacity)
			{
				sbyte[] newArray = new sbyte[capacity];
				if (bytes != null)
				{
					Array.Copy(bytes, 0, newArray, 0, bytes.Length);
				}
				bytes = newArray;
			}
		}
		
		// Copy count bytes from array source starting at offset.
		public void  set_Renamed(sbyte[] source, int offset, int count)
		{
			bytes = new sbyte[count];
			size_Renamed_Field = count;
			for (int x = 0; x < count; x++)
			{
				bytes[x] = source[offset + x];
			}
		}
	}
}