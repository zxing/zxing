/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.aztec.decoder
{

	import com.google.zxing.FormatException;
	import com.google.zxing.aztec.AztecDetectorResult;
	import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.DecoderResult;
	import com.google.zxing.common.flexdatatypes.StringBuilder;
	import com.google.zxing.common.reedsolomon.GenericGF;
	import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
	import com.google.zxing.common.reedsolomon.ReedSolomonException;

	/**
	 * <p>The main class which implements Aztec Code decoding -- as opposed to locating and extracting
	 * the Aztec Code from an image.</p>
	 *
	 * @author David Olivier
	 */
	public 	class Decoder 
	{

  private static var UPPER:int = 0;
  private static var LOWER:int = 1;
  private static var MIXED:int = 2;
  private static var DIGIT:int = 3;
  private static var PUNCT:int = 4;
  private static var BINARY:int = 5;

  private static var NB_BITS_COMPACT:Array = [0, 104, 240, 408, 608];

  private static  var NB_BITS:Array = [
      0, 128, 288, 480, 704, 960, 1248, 1568, 1920, 2304, 2720, 3168, 3648, 4160, 4704, 5280, 5888, 6528,
      7200, 7904, 8640, 9408, 10208, 11040, 11904, 12800, 13728, 14688, 15680, 16704, 17760, 18848, 19968
  ];

  private static  var NB_DATABLOCK_COMPACT:Array = [
      0, 17, 40, 51, 76
  ];

  private static  var NB_DATABLOCK:Array = [
      0, 21, 48, 60, 88, 120, 156, 196, 240, 230, 272, 316, 364, 416, 470, 528, 588, 652, 720, 790, 864,
      940, 1020, 920, 992, 1066, 1144, 1224, 1306, 1392, 1480, 1570, 1664
  ];

  private static  var UPPER_TABLE:Array = [
      "CTRL_PS", " ", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
      "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "CTRL_LL", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  ];

  private static  var LOWER_TABLE:Array = [
      "CTRL_PS", " ", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
      "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "CTRL_US", "CTRL_ML", "CTRL_DL", "CTRL_BS"
  ];

  private static var  MIXED_TABLE:Array = [
      "CTRL_PS", " ", "\1", "\2", "\3", "\4", "\5", "\6", "\7", "\b", "\t", "\n",
      "\13", "\f", "\r", "\33", "\34", "\35", "\36", "\37", "@", "\\", "^", "_",
      "`", "|", "~", "\177", "CTRL_LL", "CTRL_UL", "CTRL_PL", "CTRL_BS"
  ];

  private static var PUNCT_TABLE:Array = [
      "", "\r", "\r\n", ". ", ", ", ": ", "!", "\"", "#", "$", "%", "&", "'", "(", ")",
      "*", "+", ",", "-", ".", "/", ":", ";", "<", "=", ">", "?", "[", "]", "{", "}", "CTRL_UL"
  ];

  private static var DIGIT_TABLE:Array = [
    "CTRL_PS", " ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ",", ".", "CTRL_UL", "CTRL_US"
  ];

  private var numCodewords:int;
  private var codewordSize:int;
  private var ddata:AztecDetectorResult;
  private var invertedBitCount:int;

  public function decode( detectorResult:AztecDetectorResult):DecoderResult {
    ddata = detectorResult;
    var matrix:BitMatrix  = detectorResult.getBits();

    if (!ddata.isCompact()) {
      matrix = removeDashedLines(ddata.getBits());
    }

    var rawbits:Array = extractBits(matrix);
    var rawbitsString:String = "";for(var k:int=0;k<rawbits.length;k++){ rawbitsString += ((rawbits[k])?"T":"F");}

    var correctedBits:Array = correctBits(rawbits);
    var correctedBitsString:String = "";for(var k1:int=0;k1<correctedBits.length;k1++){ correctedBitsString += ((correctedBits[k1])?"T":"F");}

    var result:String = getEncodedData(correctedBits);

    return new DecoderResult(null, result, null, null);
  }

  
  /**
   *
   * Gets the string encoded in the aztec code bits
   *
   * @return the decoded string
   * @throws FormatException if the input is not valid
   */
  private function getEncodedData(correctedBits:Array):String {

    var endIndex:int = codewordSize * ddata.getNbDatablocks() - invertedBitCount;
    if (endIndex > correctedBits.length) {
      throw FormatException.getFormatInstance();
    }

    var lastTable:int = UPPER;
    var table:int = UPPER;
    var startIndex:int = 0;
    var result:StringBuilder = new StringBuilder(20);
    var end:Boolean = false;
    var shift:Boolean = false;
    var switchShift:Boolean = false;

    while (!end) {
 
      if (shift) {
        // the table is for the next character only
        switchShift = true;
      } else {
        // save the current table in case next one is a shift
        lastTable = table;
      }

      var code:int;
      switch (table) {
      case BINARY:
        if (endIndex - startIndex < 8) {
          end = true;
          break;
        }
        code = readCode(correctedBits, startIndex, 8);
        startIndex += 8;

        result.Append(String.fromCharCode(code));
        break;

      default:
        var size:int = 5;

        if (table == DIGIT) {
          size = 4;
        }

        if (endIndex - startIndex < size) {
          end = true;
          break;
        }

        code = readCode(correctedBits, startIndex, size);
        startIndex += size;

        var str:String = getCharacter(table, code);
        if (str.substr(0,5) == "CTRL_") {
          // Table changes
          table = getTable(str.charAt(5));

          if (str.charAt(6) == 'S') {
            shift = true;
          }
        } else {
          result.Append(str);
        }

        break;
      }

      if (switchShift) {
        table = lastTable;
        shift = false;
        switchShift = false;
      }

    }
    return result.ToString();
  }


  /**
   * gets the table corresponding to the char passed
   */
  private static function getTable(t:String):int {
    var table:int = UPPER;

    switch (t) {
      case 'U':
        table = UPPER;
        break;
      case 'L':
        table = LOWER;
        break;
      case 'P':
        table = PUNCT;
        break;
      case 'M':
        table = MIXED;
        break;
      case 'D':
        table = DIGIT;
        break;
      case 'B':
        table = BINARY;
        break;
    }

    return table;
  }
  
  /**
   *
   * Gets the character (or string) corresponding to the passed code in the given table
   *
   * @param table the table used
   * @param code the code of the character
   */
  private static function getCharacter(table:int, code:int):String {
    switch (table) {
      case UPPER:
        return UPPER_TABLE[code];
      case LOWER:
        return LOWER_TABLE[code];
      case MIXED:
        return MIXED_TABLE[code];
      case PUNCT:
        return PUNCT_TABLE[code];
      case DIGIT:
        return DIGIT_TABLE[code];
      default:
        return "";
    }
  }

  /**
   *
   * <p> performs RS error correction on an array of bits </p>
   *
   * @return the corrected array
   * @throws FormatException if the input contains too many errors
   */
  private function correctBits(rawbits:Array):Array {
     var gf:GenericGF;

    if (ddata.getNbLayers() <= 2) {
      codewordSize = 6;
      gf = GenericGF.AZTEC_DATA_6;
    } else if (ddata.getNbLayers() <= 8) {
      codewordSize = 8;
      gf = GenericGF.AZTEC_DATA_8;
    } else if (ddata.getNbLayers() <= 22) {
      codewordSize = 10;
      gf = GenericGF.AZTEC_DATA_10;
    } else {
      codewordSize = 12;
      gf = GenericGF.AZTEC_DATA_12;
      
    }

    var numDataCodewords:int = ddata.getNbDatablocks();
    var numECCodewords:int;
    var offset:int;

    if (ddata.isCompact()) {
      offset = NB_BITS_COMPACT[ddata.getNbLayers()] - numCodewords*codewordSize;
      numECCodewords = NB_DATABLOCK_COMPACT[ddata.getNbLayers()] - numDataCodewords;
    } else {
      offset = NB_BITS[ddata.getNbLayers()] - numCodewords*codewordSize;
      numECCodewords = NB_DATABLOCK[ddata.getNbLayers()] - numDataCodewords;
    }

    var dataWords:Array = new Array(numCodewords);
    for (var m:int=0;m<dataWords.length;m++) { dataWords[m] = 0;}
    
    for (var ii:int = 0; ii < numCodewords; ii++) 
    {
      var flag:int = 1;
      for (var j2:int = 1; j2 <= codewordSize; j2++) {
        if (rawbits[codewordSize*ii + codewordSize - j2 + offset]) {
          dataWords[ii] += flag;
        }
        flag <<= 1;
      }

      //if (dataWords[i] >= flag) {
      //  flag++;
      //}
    }

    try {
      var rsDecoder:ReedSolomonDecoder = new ReedSolomonDecoder(gf);
      rsDecoder.decode(dataWords, numECCodewords);
    } catch (rse:ReedSolomonException ) {
      throw FormatException.getFormatInstance();
    }

    offset = 0;
    invertedBitCount = 0;

    var correctedBits:Array = new Array(numDataCodewords*codewordSize);
    for(var z:int=0;z<(numDataCodewords*codewordSize);z++) {correctedBits[z] = 0; }
     
    for (var i:int = 0; i < numDataCodewords; i ++) {

      var seriesColor:Boolean = false;
      var seriesCount:int = 0;
      var _flag:int = 1 << (codewordSize - 1);

      for (var j:int = 0; j < codewordSize; j++) {

        var color:Boolean = (dataWords[i] & _flag) == _flag;

        if (seriesCount == codewordSize - 1) {

          if (color == seriesColor) {
            //bit must be inverted
            throw FormatException.getFormatInstance();
          }

          seriesColor = false;
          seriesCount = 0;
          offset++;
          invertedBitCount++;
        } else {

          if (seriesColor == color) {
            seriesCount++;
          } else {
            seriesCount = 1;
            seriesColor = color;
          }

          correctedBits[i * codewordSize + j - offset] = color;

        }

        _flag >>>= 1;
      }
    }

    return correctedBits;
  }

  /**
   *
   * Gets the array of bits from an Aztec Code matrix
   *
   * @return the array of bits
   * @throws FormatException if the matrix is not a valid aztec code
   */
  private function extractBits(matrix:BitMatrix ):Array {

    var rawbits:Array;
    if (ddata.isCompact()) {
      if (ddata.getNbLayers() > NB_BITS_COMPACT.length) {
        throw FormatException.getFormatInstance();
      }
      rawbits = new Array(NB_BITS_COMPACT[ddata.getNbLayers()]);
      numCodewords = NB_DATABLOCK_COMPACT[ddata.getNbLayers()];
    } else {
      if (ddata.getNbLayers() > NB_BITS.length) {
        throw FormatException.getFormatInstance();
      }
      rawbits = new Array(NB_BITS[ddata.getNbLayers()]);
      numCodewords = NB_DATABLOCK[ddata.getNbLayers()];
    }

    var layer:int = ddata.getNbLayers();
    var size:int = matrix.getHeight();
    var rawbitsOffset:int = 0;
    var matrixOffset:int = 0;

    while (layer != 0) {

      var flip:int = 0;
      for (var i:int = 0; i < 2*size - 4; i++) 
      {
        rawbits[rawbitsOffset+i] = matrix._get(matrixOffset + flip, matrixOffset + int(i/2));
        rawbits[rawbitsOffset+2*size - 4 + i] = matrix._get(matrixOffset + int(i/2), matrixOffset + size-1-flip);
        flip = int((flip + 1)%2);
      }

      flip = 0;
      for (i = 2*size+1; i > 5; i--) {
        rawbits[rawbitsOffset+4*size - 8 + (2*size-i) + 1] = matrix._get(matrixOffset + size-1-flip, matrixOffset + int(i/2) - 1);
        rawbits[rawbitsOffset+6*size - 12 + (2*size-i) + 1] = matrix._get(matrixOffset + int(i/2) - 1, matrixOffset + flip);
        flip = int((flip + 1)%2);
      }

      matrixOffset += 2;
      rawbitsOffset += 8*size-16;
      layer--;
      size -= 4;
    }

    return rawbits;
  }
  

  /**
   * Transforms an Aztec code matrix by removing the control dashed lines
   */
  private static function removeDashedLines(matrix:BitMatrix):BitMatrix 
  {
  	
    var nbDashed:int = 1+ 2* int(int((matrix.getWidth() - 1)/2) / 16); // Bas : int casts added to make calculation the same as Java 
    var newMatrix:BitMatrix  = new BitMatrix(matrix.getWidth() - nbDashed, matrix.getHeight() - nbDashed);

    var nx:int = 0;

    for (var x:int = 0; x < matrix.getWidth(); x++) {
		var term1:int = int(int((int(matrix.getWidth() / 2)) - x)%16); 
      if (term1 == 0) {
        continue;
      }

      var ny:int = 0;
      for (var y:int = 0; y < matrix.getHeight(); y++) {
		var term2:int = int(int((int(matrix.getWidth() / 2)) - y)%16); 
        if (term2 == 0) {
          continue;
        }

        if (matrix._get(x, y)) 
        {
          newMatrix._set(nx, ny);
        }
        ny++;
      }
      nx++;
    }

    return newMatrix;
  }

  /**
   * Reads a code of given length and at given index in an array of bits
   */
  private static function readCode(rawbits:Array, startIndex:int, length:int):int {
    var res:int = 0;

    for (var i:int = startIndex; i < startIndex + length; i++) {
      res <<= 1;
      if (rawbits[i]) {
        res++;
      }
    }

    return res;
  }

}
}
