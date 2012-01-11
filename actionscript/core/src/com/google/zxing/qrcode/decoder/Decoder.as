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
	import com.google.zxing.common.flexdatatypes.HashTable;
	
	
    public class Decoder
    { 
    	import com.google.zxing.common.BitMatrix;
		import com.google.zxing.common.DecoderResult;
    	import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
    	import com.google.zxing.common.reedsolomon.ReedSolomonException;
    	import com.google.zxing.common.reedsolomon.GenericGF;
    	import com.google.zxing.common.zxingByteArray;
    	import com.google.zxing.ReaderException;

/**
 * <p>The main class which implements QR Code decoding -- as opposed to locating and extracting
 * the QR Code from an image.</p>
 *
 * @author Sean Owen
 */    	
    	
          private var rsDecoder:ReedSolomonDecoder;

          public function Decoder() {
            rsDecoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
          }

          /**
           * <p>Convenience method that can decode a QR Code represented as a 2D array of booleans.
           * "true" is taken to mean a black module.</p>
           *
           * @param image booleans representing white/black QR Code modules
           * @return text and bytes encoded within the QR Code
           * @throws ReaderException if the QR Code cannot be decoded
           */
           
          public function decode(image:Object,hints:HashTable=null):DecoderResult
          {
          	if (image is Array) { return decode_Array(image as Array,hints);}
          	else if (image is BitMatrix) { return decode_BitMatrix(image as BitMatrix,hints);}
          	else { throw new Error('Decoder : decode : unknown type of image');} 
          }
          
          public function decode_Array(image:Array, hints:HashTable):DecoderResult {
                var dimension:int = image.length;
                var bits:BitMatrix = new BitMatrix(dimension);
                for (var i:int = 0; i < dimension; i++) {
                  for (var j:int = 0; j < dimension; j++) {
                    if (image[i][j]) {
                      bits._set(j, i);
                    }
                  }
                }
                return decode(bits,hints);
          }

          /**
           * <p>Decodes a QR Code represented as a {@link BitMatrix}. A 1 or "true" is taken to mean a black module.</p>
           *
           * @param bits booleans representing white/black QR Code modules
           * @return text and bytes encoded within the QR Code
           * @throws ReaderException if the QR Code cannot be decoded
           */
          public function decode_BitMatrix(bits:BitMatrix, hints:HashTable ):DecoderResult{
              try{
                // Construct a parser and read version, error-correction level
                var parser:BitMatrixParser = new BitMatrixParser(bits);
                var version:Version  = parser.readVersion();
                var ecLevel:ErrorCorrectionLevel = parser.readFormatInformation().getErrorCorrectionLevel();

                // Read codewords
                var codewords:zxingByteArray = parser.readCodewords();
                // Separate into data blocks
                var dataBlocks:Array = DataBlock.getDataBlocks(codewords, version, ecLevel);

                // Count total number of data bytes
                var totalBytes:int = 0;
                for (var i:int = 0; i < dataBlocks.length; i++) {
                  totalBytes += dataBlocks[i].NumDataCodewords;
                }
                
                var resultBytes:Array = new Array(totalBytes);
                var resultOffset:int = 0;

                // Error-correct and copy data blocks together into a stream of bytes
                for (var j:int = 0; j < dataBlocks.length; j++) {
                  var dataBlock:DataBlock = dataBlocks[j];
                  var codewordBytes:Array  = dataBlock.Codewords;
                  var numDataCodewords:int = dataBlock.NumDataCodewords;
                  correctErrors(codewordBytes, numDataCodewords);
                  for (var i2:int = 0; i2 < numDataCodewords; i2++) 
                  {
                    resultBytes[resultOffset++] = codewordBytes[i2];
                  }
                }

                // Decode the contents of that stream of bytes
                return DecodedBitStreamParser.decode(resultBytes, version,ecLevel,hints);
              }catch(e:ReedSolomonException){
                throw new ReaderException(e.message);
              }
            
            return null;
          }

          /**
           * <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
           * correct the errors in-place using Reed-Solomon error correction.</p>
           *
           * @param codewordBytes data and error correction codewords
           * @param numDataCodewords number of codewords that are data bytes
           * @throws ReaderException if error correction fails
           */
          private function correctErrors( codewordBytes:Array,  numDataCodewords:int):void{
                  var numCodewords:int = codewordBytes.length;
                  // First read into an array of ints
                  var codewordsInts:Array = new Array(numCodewords);
                  for (var i:int = 0; i < numCodewords; i++)
                  {
                      codewordsInts[i] = codewordBytes[i] & 0xFF;
                  }
                  var numECCodewords:int = codewordBytes.length - numDataCodewords;
                  try
                  {
                      rsDecoder.decode(codewordsInts, numECCodewords);
                  }
                  catch ( rse:ReedSolomonException)
                  {
                      throw new ReaderException(rse.message);
                  }
                  // Copy back into array of bytes -- only need to worry about the bytes that were data
                  // We don't care about errors in the error-correction codewords
                  for (var i3:int = 0; i3 < numDataCodewords; i3++)
                  {
                      codewordBytes[i3] = Math.floor(codewordsInts[i3]);
                      // Flex : make bytes
                      if (codewordBytes[i3] > 127) { codewordBytes[i3] = (256-(Math.floor(codewordBytes[i3])))*-1; }
                  }
          }
    }

}