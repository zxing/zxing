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
package com.google.zxing.qrcode.decoder
{
	public class BitMatrixParser
	{
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.common.zxingByteArray;
		import com.google.zxing.ReaderException;
		
          private var bitMatrix:BitMatrix;
          private var parsedVersion:Version ;
          private var parsedFormatInfo:FormatInformation;
          

          /**
           * @param bitMatrix {@link BitMatrix} to parse
           * @throws ReaderException if dimension is not >= 21 and 1 mod 4
           */
          public function BitMatrixParser(bitMatrix:BitMatrix )
          {
            var dimension:int = bitMatrix.getHeight();
            if ((dimension < 21) || ((dimension & 0x03) != 1)) 
            {
              throw new ReaderException("BitMatrixParser : BitMatrixParser : dimension ("+dimension+" less than 21 or not a power of 3)");
            }
            this.bitMatrix = bitMatrix;
          }

          /**
           * <p>Reads format information from one of its two locations within the QR Code.</p>
           *
           * @return {@link FormatInformation} encapsulating the QR Code's format info
           * @throws ReaderException if both format information locations cannot be parsed as
           * the valid encoding of format information
           */
          public function  readFormatInformation():FormatInformation
          {

            if (parsedFormatInfo != null) 
            {
              return parsedFormatInfo;
            }

            // Read top-left format info bits
            var formatInfoBits1:int = 0;
            for (var j:int = 0; j < 6; j++) 
            {
              formatInfoBits1 = copyBit(j, 8, formatInfoBits1);
            }
            // .. and skip a bit in the timing pattern ...
            formatInfoBits1 = copyBit(7, 8, formatInfoBits1);
            formatInfoBits1 = copyBit(8, 8, formatInfoBits1);
            formatInfoBits1 = copyBit(8, 7, formatInfoBits1);
            // .. and skip a bit in the timing pattern ...
            for (var i:int = 5; i >= 0; i--) 
            {
              formatInfoBits1 = copyBit(8, i, formatInfoBits1);
            }

            var dimension:int = bitMatrix.getHeight();
            var formatInfoBits2:int = 0;
            var jMin:int = dimension - 7;
             for (var j1:int= dimension - 1; j1 >= jMin; j1--) 
             {
      			formatInfoBits2 = copyBit(8, j1, formatInfoBits2);
    		 }
    		for (var i1:int = dimension - 8; i1 < dimension; i1++) 
    		{
      			formatInfoBits2 = copyBit(i1, 8, formatInfoBits2);
   			 }
   			 
            parsedFormatInfo = FormatInformation.decodeFormatInformation(formatInfoBits1,formatInfoBits2);
            if (parsedFormatInfo != null) 
            {
              return parsedFormatInfo;
            }
            throw new ReaderException("BitMatrixParser : readFormatInformation : parsedFormatInfo == null");
          }

          /**
           * <p>Reads version information from one of its two locations within the QR Code.</p>
           *
           * @return {@link Version} encapsulating the QR Code's version
           * @throws ReaderException if both version information locations cannot be parsed as
           * the valid encoding of version information
           */
          public function readVersion():Version
          {

            if (parsedVersion != null) 
            {
              return parsedVersion;
            }

            var dimension:int = bitMatrix.getHeight();

            var provisionalVersion:int = (dimension - 17) >> 2;
            if (provisionalVersion <= 6) 
            {
              return Version.getVersionForNumber(provisionalVersion);
            }

            // Read top-right version info: 3 wide by 6 tall
            var versionBits:int = 0;
			var jMin:int = dimension - 11;
            for (var i:int = 5; i >= 0; i--) 
            {
              for (var j2:int = dimension - 9; j2 >= jMin; j2--) 
              {
                versionBits = copyBit(j2, i, versionBits);
              }
            }

            parsedVersion = Version.decodeVersionInformation(versionBits);
            if (parsedVersion != null && parsedVersion.getDimensionForVersion() == dimension)
            {
              return parsedVersion;
            }

            // Hmm, failed. Try bottom left: 6 wide by 3 tall
            versionBits = 0;
			var iMin:int = dimension - 11;
            for (var j:int = 5; j >= 0; j--) 
            {
              for (var i2:int = dimension - 9; i2 >= iMin; i2--) 
              {
                versionBits = copyBit(j, i2, versionBits);
              }
            }

            parsedVersion = Version.decodeVersionInformation(versionBits);
            if (parsedVersion != null && parsedVersion.getDimensionForVersion() == dimension)
            {
              return parsedVersion;
            }
            throw new ReaderException("BitMatrixParser : readVersion : could not read version");
          }

          private function copyBit(i:int, j:int, versionBits:int):int 
          {
            return bitMatrix._get(i, j) ? (versionBits << 1) | 0x1 : versionBits << 1;
          }

          /**
           * <p>Reads the bits in the {@link BitMatrix} representing the finder pattern in the
           * correct order in order to reconstitute the codewords bytes contained within the
           * QR Code.</p>
           *
           * @return bytes encoded within the QR Code
           * @throws ReaderException if the exact number of bytes expected is not read
           */
          public function readCodewords():zxingByteArray
          {

            var formatInfo:FormatInformation = readFormatInformation();
            var version:Version = readVersion();

            // Get the data mask for the format used in this QR Code. This will exclude
            // some bits from reading as we wind through the bit matrix.
            var dataMask:DataMaskBase;
            var ref:int = formatInfo.getDataMask();
            dataMask = DataMask.forReference(ref);
            var dimension:int = bitMatrix.getHeight();
            dataMask.unmaskBitMatrix(bitMatrix, dimension);

            var functionPattern:BitMatrix = version.buildFunctionPattern();

            var readingUp:Boolean = true;
            var result:zxingByteArray = new zxingByteArray(version.getTotalCodewords());
            var resultOffset:int = 0;
            var currentByte:int = 0;
            var bitsRead:int = 0;
            // Read columns in pairs, from right to left
            for (var j:int = dimension - 1; j > 0; j -= 2) 
            {
              if (j == 6) 
              {
                // Skip whole column with vertical alignment pattern;
                // saves time and makes the other code proceed more cleanly
                j--;
              }
              // Read alternatingly from bottom to top then top to bottom
              for (var count:int = 0; count < dimension; count++) 
              {
                var i:int = readingUp ? dimension - 1 - count : count;
                for (var col:int = 0; col < 2; col++) 
                {
                  // Ignore bits covered by the function pattern
                  if (!functionPattern._get(j - col,i)) 
                  {
                    // Read a bit
                    bitsRead++;
                    currentByte <<= 1;
                    if (bitMatrix._get(j - col,i)) 
                    {
                      currentByte |= 1;
                    }
                    // If we've made a whole byte, save it off
                    if (bitsRead == 8) 
                    {
                      result.setByte(resultOffset,currentByte);
                      resultOffset++;
                      bitsRead = 0;
                      currentByte = 0;
                    }
                  }
                }
              }
              readingUp = !readingUp; // switch directions
            }
            if (resultOffset != version.getTotalCodewords()) 
            {
              throw new ReaderException("BitMatrixParser : readCodewords : resultOffset ("+resultOffset+") != totalcodewords ("+version.getTotalCodewords()+")");
            }
            return result;
          }
    
    
    }
}