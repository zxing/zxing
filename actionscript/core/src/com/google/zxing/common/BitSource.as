package com.google.zxing.common
{
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
    public class BitSource
    { 
    	
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
          private var bytes:Array;
          private var byteOffset:int;
          private var bitOffset:int;

          /**
           * @param bytes bytes from which this will read bits. Bits will be read from the first byte first.
           * Bits are read within a byte from most-significant to least-significant bit.
           */
          public function BitSource( bytes:Array) {
            this.bytes = bytes;
          }
          
        /**
		   * @return index of next byte in input byte array which would be read by the next call to {@link #readBits(int)}.
		   */
		  public function getByteOffset():int {
		    return byteOffset;
		  }

          /**
           * @param numBits number of bits to read
           * @return int representing the bits read. The bits will appear as the least-significant
           *         bits of the int
           * @throws IllegalArgumentException if numBits isn't in [1,32]
           */
          public function readBits(numBits:int):int 
          {
            if (numBits < 1 || numBits > 32) 
            {
              throw new IllegalArgumentException("BitSource : numBits out of range");
            }

            var result:int = 0;

            // First, read remainder from current byte
            if (bitOffset > 0) {
              var bitsLeft:int = 8 - bitOffset;
              var toRead:int = numBits < bitsLeft ? numBits : bitsLeft;
              var bitsToNotRead:int = bitsLeft - toRead;
              var mask:int = (0xFF >> (8 - toRead)) << bitsToNotRead;
              result = (bytes[byteOffset] & mask) >> bitsToNotRead;
              numBits -= toRead;
              bitOffset += toRead;
              if (bitOffset == 8) {
                bitOffset = 0;
                byteOffset++;
              }
            }

            // Next read whole bytes
            if (numBits > 0) {
              while (numBits >= 8) {
                result = (result << 8) | (bytes[byteOffset] & 0xFF);
                byteOffset++;
                numBits -= 8;
              }

              // Finally read a partial byte
              if (numBits > 0) {
                var bitsToNotRead2:int = 8 - numBits;
                var mask2:int = (0xFF >> bitsToNotRead2) << bitsToNotRead2;
                result = (result << numBits) | ((bytes[byteOffset] & mask2) >> bitsToNotRead2);
                bitOffset += numBits;
              }
            }

            return result;
          }

          /**
           * @return number of bits that can be read successfully
           */
          public function available():int {
          	var bits:int = 8 * (bytes.length - byteOffset) - bitOffset; 
            return bits;
          }
    }
}