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
package com.google.zxing.common
{
/**
 * A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
 * unsigned container, it's up to you to do byteValue & 0xff at each location.
 *
 * JAVAPORT: The original code was a 2D array of ints, but since it only ever gets assigned
 * -1, 0, and 1, I'm going to use less memory and go with bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
     public class ByteMatrix
    {
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
        private var bytes:Array;
        private var Height:int;
        private var Width:int;


    public function ByteMatrix(height:int, width:int ) {
        bytes = new Array(height);
        for (var i:int = 0; i < height; i++) {
            bytes[i] = new Array(width);
        }
        this.Height = height;
        this.Width = width;
    }

        public function height():int
        {
            return Height;
        }

        public function width():int
        {
            return Width;
        }

        public  function _get(x:int, y:int):int
        {
            return bytes[y][x];
        }

        public function getArray():Array
        {
            return bytes;
        }

        //public function _set(y:int,x:int, value:int)
        //{
        //    bytes[y][x] = value;
        //}

        public function _set(x:int, y:int, value:Object ):void
        {
        	if (value is int)
        	{
            	bytes[y][x] = value as int;
         	}
         	else
         	{
         		throw new Error('ByteMatrix : _set : unknown type of value');
         	}
         	
        }

        public function clear(value:int):void
        {
            for (var y:int = 0; y < Height; ++y)
            {
                for (var x:int = 0; x < Width; ++x)
                {
                    bytes[y][x] = value;
                }
            }
        }

        public function sum():int
        {
            var result:int = 0;
            for (var y:int = 0; y < Height; ++y)
            {
                for (var x:int = 0; x < Width; ++x)
                {
             		result += bytes[y][x];   	
                }
            }
            return result;
        }
	
        public function toString():String
        {
            var result:StringBuilder  = new StringBuilder();
            for (var y:int = 0; y < Height; ++y)
            {
                for (var x:int = 0; x < Width; ++x)
                {
                    switch (bytes[y][x])
                    {
                        case 0:
                            result.Append("0");
                            break;
                        case 1:
                            result.Append("1");
                            break;
                        default:
                            result.Append(".");
                            break;
                    }
                }
                result.Append('\n');
            }
            return result.ToString();
        }   
        
        public function toString2():String
        {
            var result:StringBuilder  = new StringBuilder();
            for (var y:int = 0; y < Height; ++y)
            {
                for (var x:int = 0; x < Width; ++x)
                {
                    switch (bytes[y][x])
                    {
                        case 0:
                            result.Append("0");
                            break;
                        case 1:
                            result.Append("1");
                            break;
                        default:
                            result.Append("_");
                            break;
                    }
                }
            }
            return result.ToString();
        }     
    }

}