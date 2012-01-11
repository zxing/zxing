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
	import mx.messaging.AbstractConsumer;
/**
 * This class implements an array of unsigned bytes.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */ public class zxingByteArray
    {
    
        private static var INITIAL_SIZE:int = 32;
        
        private var bytes:Array;
        private var Size:int;

        public function zxingByteArray(size:Object=null)
        {
       		if (size == null)
       		{            
       			bytes = null;
	            this.Size = 0;
	        }
	        else if (size is int)
	        {
	            bytes = new Array(int(size));
    	        this.Size = int(size);
    	    }
    	    else if (size is Array)
    	    {
	            bytes = (size as Array);
    	        this.Size = size.length;
    	    	
    	    }
    	    else
    	    {
    	    	throw new Error("unknown type of size");
    	    }
    	    
        }

        /**
         * Access an unsigned byte at location index.
         * @param index The index in the array to access.
         * @return The unsigned value of the byte as an int.
         */
        public function at(index:int):int
        {
            return bytes[index] & 0xff;
        }

        /*public function set(index:int, value:int):void
        {
        	// Flex doesn't know bytes -> make it a byte
        	if (value > 127) { value = 256 - value);
            bytes[index] = value;
        }*/
        
        public function setByte(index:int, value:int):void
        {
        	// Flex doesn't know bytes -> make it a byte
        	if (value > 127) { value = (256 - value)*-1;}
        	bytes[index] = value;
        }


        public function getByte(index:int):int
        {
        	return bytes[index];
        }

        public function size():int
        {
            return Size;
        }

        public function empty():Boolean
        {
            return (Size == 0);
        }

        public function appendByte(value:int):void
        {
            if (Size == 0 || Size >= bytes.length)
            { 
                var newSize:int = Math.max(INITIAL_SIZE, Size << 1);
                reserve(newSize);
            }
           	// Flex doesn't know bytes -> make it a byte
        	if (value > 127) { value = (256 - value)*-1;}
            bytes[Size] = value;
            Size++;
        }

        public function reserve(capacity:int):void
        {
            if (bytes == null || bytes.length < capacity)
            {
                var newArray:Array = new Array(capacity);
                if (bytes != null)
                {
                    //System.Array.Copy(bytes, 0, newArray, 0, bytes.length);
                    for (var i:int=0;i<bytes.length;i++)
                    {
                    	newArray[i] = bytes[i];
                    }
                }
                bytes = newArray;
            }
        }

        // Copy count bytes from array source starting at offset.
        public function _set(source:Array, offset:int, count:int):void
        {
        	if (source == null)
        	{
        		this.bytes[offset] = count;
        	}
        	else
        	{
            	bytes = new Array(count);
            	Size = count;
	            for (var x:int = 0; x < count; x++)
	            {
        			// Flex doesn't know bytes -> make it a byte
        			if (source[offset + x] > 127) 
        			{
        				bytes[x] = (256-source[offset + x])*-1;
        			} 
        			else
        			{
    	                bytes[x] = source[offset + x];
	    			
        			}
	            }
	        }
        }
    }

}