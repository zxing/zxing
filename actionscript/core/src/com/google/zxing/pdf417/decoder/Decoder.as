package com.google.zxing.pdf417.decoder
{
	import mx.core.BitmapAsset;
	
/*
 * Copyright 2009 ZXing authors
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



/**
 * <p>The main class which implements PDF417 Code decoding -- as
 * opposed to locating and extracting the PDF417 Code from an image.</p>
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
public class Decoder 
{
	import com.google.zxing.ReaderException;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DecoderResult;
	//import com.google.zxing.pdf417.reedsolomon.ReedSolomonDecoder;

  private static var MAX_ERRORS:int = 3;
  private static var MAX_EC_CODEWORDS:int = 512;
  //private ReedSolomonDecoder rsDecoder;

  public function Decoder() {
    // TODO MGMG
    //rsDecoder = new ReedSolomonDecoder();
  }
  
  public function decode(o:Object):DecoderResult
  {
  	if (o is Array) { return this.decode_Array(o as Array); }
  	else if (o is BitMatrix) { return this.decode_BitMatrix(o as BitMatrix); }
  	else { throw new ReaderException("pdf417 : Decoder : decode : unknown input parameter type"); }
  }

  /**
   * <p>Convenience method that can decode a PDF417 Code represented as a 2D array of booleans.
   * "true" is taken to mean a black module.</p>
   *
   * @param image booleans representing white/black PDF417 modules
   * @return text and bytes encoded within the PDF417 Code
   * @throws ReaderException if the PDF417 Code cannot be decoded
   */
  public function decode_Array(image:Array):DecoderResult {
    var dimension:int = image.length;
    var bits:BitMatrix = new BitMatrix(dimension);
    for (var i:int = 0; i < dimension; i++) {
      for (var j:int = 0; j < dimension; j++) {
        if (image[j][i]) {
          bits._set(j, i);
        }
      }
    }
    return decode(bits);
  }

  /**
   * <p>Decodes a PDF417 Code represented as a {@link BitMatrix}.
   * A 1 or "true" is taken to mean a black module.</p>
   *
   * @param bits booleans representing white/black PDF417 Code modules
   * @return text and bytes encoded within the PDF417 Code
   * @throws ReaderException if the PDF417 Code cannot be decoded
   */
  public function decode_BitMatrix(bits:BitMatrix ):DecoderResult {
    // Construct a parser to read the data codewords and error-correction level

    var parser:BitMatrixParser = new BitMatrixParser(bits);
    var codewords:Array = parser.readCodewords();
    
    if (codewords == null || codewords.length == 0) {
      throw new ReaderException("Decoder : decode");
    }

    var ecLevel:int = parser.getECLevel();
    var numECCodewords:int = 1 << (ecLevel + 1);
    var erasures:Array = parser.getErasures();

    correctErrors(codewords, erasures, numECCodewords);
    verifyCodewordCount(codewords, numECCodewords);

    // Decode the codewords
    return DecodedBitStreamParser.decode(codewords);
  }

  /**
   * Verify that all is OK with the codeword array.
   *
   * @param codewords
   * @return an index to the first data codeword.
   * @throws ReaderException
   */
  private static function verifyCodewordCount(codewords:Array, numECCodewords:int) :int {
    if (codewords.length < 4) {
      // Codeword array size should be at least 4 allowing for
      // Count CW, At least one Data CW, Error Correction CW, Error Correction CW
      throw new ReaderException("pfd417 : decoder : verifyCodewordCount : 1");
    }
    // The first codeword, the Symbol Length Descriptor, shall always encode the total number of data
    // codewords in the symbol, including the Symbol Length Descriptor itself, data codewords and pad
    // codewords, but excluding the number of error correction codewords.
    var numberOfCodewords:int = codewords[0];
    if (numberOfCodewords > codewords.length) {
       throw new ReaderException("pfd417 : decoder : verifyCodewordCount : 2");
    }
    if (numberOfCodewords == 0) {
      // Reset to the length of the array - 8 (Allow for at least level 3 Error Correction (8 Error Codewords)
      if (numECCodewords < codewords.length) {
        codewords[0] = codewords.length - numECCodewords;
      } else {
         throw new ReaderException("pfd417 : decoder : verifyCodewordCount : 3");
      }
    }
    return 1; // Index to first data codeword
  }

  /**
   * <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
   * correct the errors in-place using Reed-Solomon error correction.</p>
   *
   * @param codewords   data and error correction codewords
   * @throws ReaderException if error correction fails
   */
  private static function correctErrors(codewords:Array, erasures:Array, numECCodewords:int):int {
    if ((erasures != null && erasures.length > numECCodewords / 2 + MAX_ERRORS) ||
        (numECCodewords < 0 || numECCodewords > MAX_EC_CODEWORDS)) {
      // Too many errors or EC Codewords is corrupted
      throw new ReaderException("pdf417 : Decoder : correctErrors : 1");
    }
    // Try to correct the errors
    var result:int = 0; // rsDecoder.correctErrors(codewords, numECCodewords);
    if (erasures != null) {
      var numErasures:int = erasures.length;
      if (result > 0) {
        numErasures -= result;
      }
      if (numErasures > MAX_ERRORS) {
        // Still too many errors
        throw new ReaderException("pdf417 : Decoder : correctErrors : 2");
      }
    }
    return result;
	}
}

}