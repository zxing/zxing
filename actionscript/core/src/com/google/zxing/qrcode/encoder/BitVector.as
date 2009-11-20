package com.google.zxing.qrcode.encoder
{
	    public class BitVector 
	    { 
    
    		import com.google.zxing.common.flexdatatypes.StringBuilder;
    		import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    		
          private var sizeInBits:int;
          private var array:Array;

          // For efficiency, start out with some room to work.
          private static var DEFAULT_SIZE_IN_BYTES:int = 32;

          public function BitVector() 
          {
            sizeInBits = 0;
            array = new Array(DEFAULT_SIZE_IN_BYTES);
            // init array
            for (var i:int=0;i<array.length;i++) { array[i] = 0; }
          }

          // Return the bit value at "index".
          public function at(index:int):int {
            if (index < 0 || index >= sizeInBits) {
              throw new IllegalArgumentException("Bad index: " + index);
            }
            var value:int = array[index >> 3] & 0xff;
            return (value >> (7 - (index & 0x7))) & 1;
          }

          // Return the number of bits in the bit vector.
          public function size():int {
            return sizeInBits;
          }

          // Return the number of bytes in the bit vector.
          public function sizeInBytes():int {
            return (sizeInBits + 7) >> 3;
          }

          // Append one bit to the bit vector.
          public function appendBit(bit:int):void 
          {
            if (!(bit == 0 || bit == 1)) {
              throw new IllegalArgumentException("Bad bit");
            }
            var numBitsInLastByte:int = sizeInBits & 0x7;
            // We'll expand array if we don't have bits in the last byte.
            if (numBitsInLastByte == 0) {
              appendByte(0);
              sizeInBits -= 8;
            }
            // Modify the last byte.
            array[sizeInBits >> 3] |= int((bit << (7 - numBitsInLastByte)));
            ++sizeInBits;
          }

          // Append "numBits" bits in "value" to the bit vector.
          // REQUIRES: 0<= numBits <= 32.
          //
          // Examples:
          // - appendBits(0x00, 1) adds 0.
          // - appendBits(0x00, 4) adds 0000.
          // - appendBits(0xff, 8) adds 11111111.
          public function appendBits(value:int, numBits:int):void {
            if (numBits < 0 || numBits > 32) {
              throw new IllegalArgumentException("Num bits must be between 0 and 32");
            }
            var numBitsLeft:int = numBits;
            while (numBitsLeft > 0) {
              // Optimization for byte-oriented appending.
              if ((sizeInBits & 0x7) == 0 && numBitsLeft >= 8) {
                var newByte:int = (value >> (numBitsLeft - 8)) & 0xff;
                appendByte(newByte);
                numBitsLeft -= 8;
              } else {
                var bit:int = (value >> (numBitsLeft - 1)) & 1;
                appendBit(bit);
                --numBitsLeft;
              }
            }
          }

          // Append "bits".
          public function appendBitVector(bits:BitVector):void {
            var size:int = bits.size();
            for (var i:int = 0; i < size; ++i) 
            {
              appendBit(bits.at(i));
            }
          }

          // Modify the bit vector by XOR'ing with "other"
          public function xor(other:BitVector):void {
            if (sizeInBits != other.size()) {
              throw new IllegalArgumentException("BitVector sizes don't match");
            }
            var sizeInBytes:int = (sizeInBits + 7) >> 3;
            for (var i:int = 0; i < sizeInBytes; ++i) {
              // The last byte could be incomplete (i.e. not have 8 bits in
              // it) but there is no problem since 0 XOR 0 == 0.
              array[i] ^= other.array[i];
            }
          }

          // Return String like "01110111" for debugging.
          public function toString():String {
              var result:StringBuilder = new StringBuilder(sizeInBits);
            for (var i:int = 0; i < sizeInBits; ++i) {
              if (at(i) == 0) {
                result.Append('0');
              } else if (at(i) == 1) {
                result.Append('1');
              } else {
                throw new IllegalArgumentException("Byte isn't 0 or 1");
              }
            }
            return result.ToString();
          }

          // Callers should not assume that array.length is the exact number of bytes needed to hold
          // sizeInBits - it will typically be larger for efficiency.
          public function getArray():Array {
            return array;
          }

          // Add a new byte to the end, possibly reallocating and doubling the size of the array if we've
          // run out of room.
          private function appendByte(value:int) :void
          {
            /* not needed -> array size not limited in Actionscript
            if ((sizeInBits >> 3) == array.length) 
            {
              var newArray:sbyteArray = new Array((array.length << 1));
              for (var i:int=0;i<array.length;i++)
              {
              	newArray[i] = array[i];
              }
              array = newArray;
            }
            */
            
            array[sizeInBits >> 3] = value;
            sizeInBits += 8;
          }
          
          public function makeByteArray():void
          {
          	for (var i:int=0;i<this.array.length;i++)
          	{
          		if (this.array[i] > 127)
          		{
          			this.array[i] = this.array[i] - 256;
          		}
          	}
          }
    }
}