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
    public sealed class ByteMatrix
    {
        private sbyte[][] bytes;
        private int Height;
        private int Width;


    public ByteMatrix(int height, int width) {
        bytes = new sbyte[height][];
        for (int i = 0; i < height; i++) {
            bytes[i] = new sbyte[width];
        }
        this.Height = height;
        this.Width = width;
    }

        public int height()
        {
            return Height;
        }

        public int width()
        {
            return Width;
        }

        public sbyte get(int y, int x)
        {
            return bytes[y][x];
        }

        public sbyte[][] getArray()
        {
            return bytes;
        }

        public void set(int y, int x, sbyte value)
        {
            bytes[y][x] = value;
        }

        public void set(int y, int x, int value)
        {
            bytes[y][x] = (sbyte)value;
        }

        public void clear(sbyte value)
        {
            for (int y = 0; y < Height; ++y)
            {
                for (int x = 0; x < Width; ++x)
                {
                    bytes[y][x] = value;
                }
            }
        }

        public String toString()
        {
            StringBuilder result = new StringBuilder();
            for (int y = 0; y < Height; ++y)
            {
                for (int x = 0; x < Width; ++x)
                {
                    switch (bytes[y][x])
                    {
                        case 0:
                            result.Append(" 0");
                            break;
                        case 1:
                            result.Append(" 1");
                            break;
                        default:
                            result.Append("  ");
                            break;
                    }
                }
                result.Append('\n');
            }
            return result.ToString();
        }        
    }
}