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
namespace com.google.zxing.common
{
    using System;
    using System.Text;

    /// <summary> A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
    /// unsigned container, it's up to you to do byteValue & 0xff at each location.
    /// *
    /// JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
    /// the horizontal component first, but this is for compatibility with the C++ code. The original
    /// code was a 2D array of ints, but since it only ever gets assigned -1, 0, and 1, I'm going to use
    /// less memory and go with bytes.
    /// *
    /// </summary>
    /// <author>  dswitkin@google.com (Daniel Switkin)
    /// 
    /// </author>
    public sealed class ByteArray
    {
        private static int INITIAL_SIZE = 32;
        private sbyte[] bytes;
        private int Size;

        public ByteArray()
        {
            bytes = null;
            this.Size = 0;
        }

        public ByteArray(int size)
        {
            bytes = new sbyte[size];
            this.Size = size;
        }

        public ByteArray(sbyte[] byteArray)
        {
            bytes = byteArray;
            this.Size = bytes.Length;
        }

        /**
         * Access an unsigned byte at location index.
         * @param index The index in the array to access.
         * @return The unsigned value of the byte as an int.
         */
        public int at(int index)
        {
            return bytes[index] & 0xff;
        }

        public void set(int index, int value)
        {
            bytes[index] = (sbyte)value;
        }

        public int size()
        {
            return Size;
        }

        public bool empty()
        {
            return Size == 0;
        }

        public void appendByte(int value)
        {
            if (Size == 0 || Size >= bytes.Length)
            {
                int newSize = Math.Max(INITIAL_SIZE, Size << 1);
                reserve(newSize);
            }
            bytes[Size] = (sbyte)value;
            Size++;
        }

        public void reserve(int capacity)
        {
            if (bytes == null || bytes.Length < capacity)
            {
                sbyte[] newArray = new sbyte[capacity];
                if (bytes != null)
                {
                    System.Array.Copy(bytes, 0, newArray, 0, bytes.Length);
                }
                bytes = newArray;
            }
        }

        // Copy count bytes from array source starting at offset.
        public void set(sbyte[] source, int offset, int count)
        {
            bytes = new sbyte[count];
            Size = count;
            for (int x = 0; x < count; x++)
            {
                bytes[x] = source[offset + x];
            }
        }
    }
}