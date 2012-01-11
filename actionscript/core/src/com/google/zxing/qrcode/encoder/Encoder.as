package com.google.zxing.qrcode.encoder
{
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


import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.common.CharacterSetECI;
import com.google.zxing.common.flexdatatypes.ArrayList;
import com.google.zxing.common.flexdatatypes.HashTable;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;
import com.google.zxing.common.zxingByteArray;
import com.google.zxing.qrcode.decoder.ECBlocks;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;

import flash.utils.ByteArray;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class Encoder {

  // The original table is defined in the table 5 of JISX0510:2004 (p.19).
  public static  var ALPHANUMERIC_TABLE:Array = [
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x00-0x0f
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  // 0x10-0x1f
      36, -1, -1, -1, 37, 38, -1, -1, -1, -1, 39, 40, -1, 41, 42, 43,  // 0x20-0x2f
      0,   1,  2,  3,  4,  5,  6,  7,  8,  9, 44, -1, -1, -1, -1, -1,  // 0x30-0x3f
      -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,  // 0x40-0x4f
      25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, -1, -1, -1, -1, -1,  // 0x50-0x5f
  ];

  public static var DEFAULT_BYTE_MODE_ENCODING:String = "ISO-8859-1";

  public function Encoder()
  {
  }

  // The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
  // Basically it applies four rules and summate all penalties.
  private static function calculateMaskPenalty(matrix:ByteMatrix ):int {
    var penalty:int = 0;
    penalty += MaskUtil.applyMaskPenaltyRule1(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule2(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule3(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule4(matrix);
    return penalty;
  }

  /**
   *  Encode "bytes" with the error correction level "ecLevel". The encoding mode will be chosen
   * internally by chooseMode(). On success, store the result in "qrCode".
   *
   * We recommend you to use QRCode.EC_LEVEL_L (the lowest level) for
   * "getECLevel" since our primary use is to show QR code on desktop screens. We don't need very
   * strong error correction for this purpose.
   *
   * Note that there is no way to encode bytes in MODE_KANJI. We might want to add EncodeWithMode()
   * with which clients can specify the encoding mode. For now, we don't need the functionality.
   */
//  public static function encode(content:String , ecLevel:ErrorCorrectionLevel , qrCode:QRCode ):void
//  {
//    encode(content, ecLevel, null, qrCode);
//  }

  public static function encode(content:String , ecLevel:ErrorCorrectionLevel,  qrCode:QRCode,hints:HashTable=null ):void
  {
    var encoding:String = hints == null ? null : (hints._get(EncodeHintType.CHARACTER_SET) as String);
    if (encoding == null)
    {
      encoding = DEFAULT_BYTE_MODE_ENCODING;
    }

    // Step 1: Choose the mode (encoding).
    var mode:Mode = chooseMode(content, encoding);

    // Step 2: Append "bytes" into "dataBits" in appropriate encoding.
    var dataBits:BitVector = new BitVector();
    appendBytes(content, mode, dataBits, encoding);
    
    
	// Step 3: Initialize QR code that can contain "dataBits".
    var numInputBytes:int = dataBits.sizeInBytes();
    initQRCode(numInputBytes, ecLevel, mode, qrCode);

    // Step 4: Build another bit vector that contains header and data.
    var headerAndDataBits:BitVector = new BitVector();

    // Step 4.5: Append ECI message if applicable
    if (mode == Mode.BYTE &&  encoding != DEFAULT_BYTE_MODE_ENCODING) {
      var eci:CharacterSetECI = CharacterSetECI.getCharacterSetECIByName(encoding);
      if (eci != null) {
        appendECI(eci, headerAndDataBits);
      }
    }

    appendModeInfo(mode, headerAndDataBits);

    var numLetters:int = (mode==Mode.BYTE) ? dataBits.sizeInBytes() : content.length;
    appendLengthInfo(numLetters, qrCode.getVersion(), mode, headerAndDataBits);
    headerAndDataBits.appendBitVector(dataBits);
    
    headerAndDataBits.makeByteArray();// make byte array 

    // Step 5: Terminate the bits properly.
    terminateBits(qrCode.getNumDataBytes(), headerAndDataBits);

    // Step 6: Interleave data bits with error correction code.
    var finalBits:BitVector = new BitVector();
    interleaveWithECBytes(headerAndDataBits, qrCode.getNumTotalBytes(), qrCode.getNumDataBytes(), qrCode.getNumRSBlocks(), finalBits);

    finalBits.makeByteArray();// make byte array 

    // Step 7: Choose the mask pattern and set to "qrCode".
    var matrix:ByteMatrix = new ByteMatrix(qrCode.getMatrixWidth(), qrCode.getMatrixWidth());
    
    //finalBits
    var ec:ErrorCorrectionLevel = qrCode.getECLevel()
    var v:int= qrCode.getVersion();
    //matrix
    
    var maskpattern:int = chooseMaskPattern(finalBits, qrCode.getECLevel(), qrCode.getVersion(), matrix)
    qrCode.setMaskPattern(maskpattern);
    
    // Step 8.  Build the matrix and set it to "qrCode".
    MatrixUtil.buildMatrix(finalBits, qrCode.getECLevel(), qrCode.getVersion(), qrCode.getMaskPattern(), matrix);
    
    qrCode.setMatrix(matrix);
    var A:String = matrix.toString2();
    // Step 9.  Make sure we have a valid QR Code.
    if (!qrCode.isValid()) {
      throw new WriterException("Invalid QR code: " + qrCode.toString());
    }
  }

  /**
   * @return the code point of the table used in alphanumeric mode or
   *  -1 if there is no corresponding code in the table.
   */
  public static function getAlphanumericCode(code:int):int
  {
    if (code < Encoder.ALPHANUMERIC_TABLE.length) {
      return Encoder.ALPHANUMERIC_TABLE[code];
    }
    return -1;
  }

  //public static function chooseMode(content:String):Mode {
  //  return chooseMode(content, null);
 // }

  /**
   * Choose the best mode by examining the content. Note that 'encoding' is used as a hint;
   * if it is Shift_JIS then we assume the input is Kanji and return {@link Mode#KANJI}.
   */
  public static function chooseMode(content:String, encoding:String=null):Mode {
    if (encoding == "Shift_JIS")
    {
      return Mode.KANJI;
    }
    var hasNumeric:Boolean = false;
    var hasAlphanumeric:Boolean = false;
    for (var i:int = 0; i < content.length; ++i) {
      var c:String = content.charAt(i);
      if (c.charCodeAt(0) >= ('0').charCodeAt(0) && c.charCodeAt(0) <= ('9').charCodeAt(0)) {
        hasNumeric = true;
      } else if (Encoder.getAlphanumericCode(c.charCodeAt(0)) != -1) {
        hasAlphanumeric = true;
      } else {
        return Mode.BYTE;
      }
    }
    if (hasAlphanumeric) {
      return Mode.ALPHANUMERIC;
    } else if (hasNumeric) {
      return Mode.NUMERIC;
    }
    return Mode.BYTE;
  }

  private static function chooseMaskPattern(bits:BitVector , ecLevel:ErrorCorrectionLevel , version:int ,
      matrix:ByteMatrix):int
      {

    var minPenalty:int = int.MAX_VALUE;  // Lower penalty is better.
    var bestMaskPattern:int = -1;
    // We try all mask patterns to choose the best one.
    for (var maskPattern:int = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++)
    {
      MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
      var penalty:int = calculateMaskPenalty(matrix);
      
      if (penalty < minPenalty)
      {
        minPenalty = penalty;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }

  /**
   * Initialize "qrCode" according to "numInputBytes", "ecLevel", and "mode". On success,
   * modify "qrCode".
   */
  private static function initQRCode(numInputBytes:int, ecLevel:ErrorCorrectionLevel, mode:Mode,
       qrCode:QRCode):void
   {
    qrCode.setECLevel(ecLevel);
    qrCode.setMode(mode);

    // In the following comments, we use numbers of Version 7-H.
    for (var versionNum:int = 1; versionNum <= 40; versionNum++) {
      var version:Version = Version.getVersionForNumber(versionNum);
      // numBytes = 196
      var numBytes:int = version.getTotalCodewords();
      // getNumECBytes = 130
      var ecBlocks:ECBlocks = version.getECBlocksForLevel(ecLevel);
      var numEcBytes:int = ecBlocks.getTotalECCodewords();
      // getNumRSBlocks = 5
      var numRSBlocks:int = ecBlocks.getNumBlocks();
      // getNumDataBytes = 196 - 130 = 66
      var numDataBytes:int = numBytes - numEcBytes;
      // We want to choose the smallest version which can contain data of "numInputBytes" + some
      // extra bits for the header (mode info and length info). The header can be three bytes
      // (precisely 4 + 16 bits) at most. Hence we do +3 here.
      if (numDataBytes >= numInputBytes + 3) {
        // Yay, we found the proper rs block info!
        qrCode.setVersion(versionNum);
        qrCode.setNumTotalBytes(numBytes);
        qrCode.setNumDataBytes(numDataBytes);
        qrCode.setNumRSBlocks(numRSBlocks);
        // getNumECBytes = 196 - 66 = 130
        qrCode.setNumECBytes(numEcBytes);
        // matrix width = 21 + 6 * 4 = 45
        qrCode.setMatrixWidth(version.getDimensionForVersion());
        return;
      }
    }
    throw new WriterException("Cannot find proper rs block info (input data too big?)");
  }

  /**
   * Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
   */
  public static function terminateBits(numDataBytes:int, bits:BitVector) :void
  {
    var capacity:int = numDataBytes << 3;
    if (bits.size() > capacity) {
      throw new WriterException("data bits cannot fit in the QR Code" + bits.size() + " > " +
          capacity);
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    // TODO: srowen says we can remove this for loop, since the 4 terminator bits are optional if
    // the last byte has less than 4 bits left. So it amounts to padding the last byte with zeroes
    // either way.
    for (var i3:int = 0; i3 < 4 && bits.size() < capacity; ++i3) {
      bits.appendBit(0);
    }
    var numBitsInLastByte:int = bits.size() % 8;
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    if (numBitsInLastByte > 0) {
      var numPaddingBits:int = 8 - numBitsInLastByte;
      for (var i2:int = 0; i2 < numPaddingBits; ++i2) {
        bits.appendBit(0);
      }
    }
    // Should be 8-bit aligned here.
    if (bits.size() % 8 != 0) {
      throw new WriterException("Number of bits is not a multiple of 8");
    }
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    var numPaddingBytes:int = numDataBytes - bits.sizeInBytes();
    for (var i:int = 0; i < numPaddingBytes; ++i) {
      if (i % 2 == 0) {
        bits.appendBits(0xec, 8);
      } else {
        bits.appendBits(0x11, 8);
      }
    }
    if (bits.size() != capacity) {
      throw new WriterException("Bits size does not equal capacity");
    }
  }

  /**
   * Get number of data bytes and number of error correction bytes for block id "blockID". Store
   * the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
   * JISX0510:2004 (p.30)
   */
  public static function getNumDataBytesAndNumECBytesForBlockID(numTotalBytes:int ,  numDataBytes:int,
      numRSBlocks:int ,blockID:int, numDataBytesInBlock:Array,
      numECBytesInBlock:Array):void {
    if (blockID >= numRSBlocks) {
      throw new WriterException("Block ID too large");
    }
    // numRsBlocksInGroup2 = 196 % 5 = 1
    var numRsBlocksInGroup2:int = numTotalBytes % numRSBlocks;
    // numRsBlocksInGroup1 = 5 - 1 = 4
    var numRsBlocksInGroup1:int = numRSBlocks - numRsBlocksInGroup2;
    // numTotalBytesInGroup1 = 196 / 5 = 39
    var numTotalBytesInGroup1:int = numTotalBytes / numRSBlocks;
    // numTotalBytesInGroup2 = 39 + 1 = 40
    var numTotalBytesInGroup2:int = numTotalBytesInGroup1 + 1;
    // numDataBytesInGroup1 = 66 / 5 = 13
    var numDataBytesInGroup1:int = numDataBytes / numRSBlocks;
    // numDataBytesInGroup2 = 13 + 1 = 14
    var numDataBytesInGroup2:int = numDataBytesInGroup1 + 1;
    // numEcBytesInGroup1 = 39 - 13 = 26
    var numEcBytesInGroup1:int = numTotalBytesInGroup1 - numDataBytesInGroup1;
    // numEcBytesInGroup2 = 40 - 14 = 26
    var numEcBytesInGroup2:int = numTotalBytesInGroup2 - numDataBytesInGroup2;
    // Sanity checks.
    // 26 = 26
    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
      throw new WriterException("EC bytes mismatch");
    }
    // 5 = 4 + 1.
    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
      throw new WriterException("RS blocks mismatch");
    }
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    if (numTotalBytes !=
        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1) +
            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
                numRsBlocksInGroup2)) {
      throw new WriterException("Total bytes mismatch");
    }

    if (blockID < numRsBlocksInGroup1) {
      numDataBytesInBlock[0] = numDataBytesInGroup1;
      numECBytesInBlock[0] = numEcBytesInGroup1;
    } else {
      numDataBytesInBlock[0] = numDataBytesInGroup2;
      numECBytesInBlock[0] = numEcBytesInGroup2;
    }
  }

  /**
   * Interleave "bits" with corresponding error correction bytes. On success, store the result in
   * "result". The interleave rule is complicated. See 8.6 of JISX0510:2004 (p.37) for details.
   */
  public static function interleaveWithECBytes(bits:BitVector, numTotalBytes:int,
      numDataBytes:int , numRSBlocks:int , result:BitVector ):void  {

    // "bits" must have "getNumDataBytes" bytes of data.
    if (bits.sizeInBytes() != numDataBytes) {
      throw new WriterException("Number of bits and data bytes does not match");
    }

    // Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
    // store the divided data bytes blocks and error correction bytes blocks into "blocks".
    var dataBytesOffset:int = 0;
    var maxNumDataBytes :int= 0;
    var maxNumEcBytes:int = 0;

    // Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
    //var blocks:ArrayList = new ArrayList(numRSBlocks);
    var blocks:ArrayList = new ArrayList();

    for (var i4:int = 0; i4 < numRSBlocks; ++i4)
    {
      var numDataBytesInBlock:Array = new Array(1);
      var numEcBytesInBlock:Array = new Array(1);
      getNumDataBytesAndNumECBytesForBlockID( numTotalBytes, numDataBytes, numRSBlocks, i4, numDataBytesInBlock, numEcBytesInBlock);

      var dataBytes2:zxingByteArray  = new zxingByteArray();
      dataBytes2._set(bits.getArray(), dataBytesOffset, numDataBytesInBlock[0]);
      var ecBytes2:zxingByteArray = generateECBytes(dataBytes2, numEcBytesInBlock[0]);
      blocks.addElement(new BlockPair(dataBytes2, ecBytes2));

      maxNumDataBytes = Math.max(maxNumDataBytes, dataBytes2.size());
      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes2.size());
      dataBytesOffset += numDataBytesInBlock[0];
    }
    
    if (numDataBytes != dataBytesOffset) {
      throw new WriterException("Data bytes does not match offset");
    }

    // First, place data blocks.
    for (var i2:int = 0; i2 < maxNumDataBytes; ++i2) {
      for (var j2:int = 0; j2 < blocks.size(); ++j2) {
        var dataBytes:zxingByteArray = (blocks.elementAt(j2) as BlockPair).getDataBytes();
        if (i2 < dataBytes.size()) {
          result.appendBits(dataBytes.at(i2), 8);
        }
      }
    }
    // Then, place error correction blocks.
    for (var i:int = 0; i < maxNumEcBytes; ++i) {
      for (var j:int = 0; j < blocks.size(); ++j) {
        var  ecBytes:zxingByteArray = (blocks.elementAt(j) as BlockPair).getErrorCorrectionBytes();
        if (i < ecBytes.size()) {
          result.appendBits(ecBytes.at(i), 8);
        }
      }
    }
    if (numTotalBytes != result.sizeInBytes()) {  // Should be same.
      throw new WriterException("Interleaving error: " + numTotalBytes + " and " +
          result.sizeInBytes() + " differ.");
    }
  }

  public static function  generateECBytes(dataBytes:zxingByteArray , numEcBytesInBlock:int):zxingByteArray {
    var numDataBytes:int = dataBytes.size();
    var toEncode:Array = new Array(numDataBytes + numEcBytesInBlock);
    for (var i:int = 0; i < numDataBytes; i++) {
      toEncode[i] = dataBytes.at(i);
    }
    new ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256).encode(toEncode, numEcBytesInBlock);

    var ecBytes:zxingByteArray = new zxingByteArray(numEcBytesInBlock);
    for (var i4:int = 0; i4 < numEcBytesInBlock; i4++)
    {
      ecBytes.setByte(i4, toEncode[numDataBytes + i4]);
    }
    return ecBytes;
  }

  /**
   * Append mode info. On success, store the result in "bits".
   */
  public static function appendModeInfo(mode:Mode , bits:BitVector ):void {
    bits.appendBits(mode.getBits(), 4);
  }


  /**
   * Append length info. On success, store the result in "bits".
   */
  public static function  appendLengthInfo(numLetters:int , version:int, mode:Mode , bits:BitVector ):void
  {
    var numBits:int = mode.getCharacterCountBits(Version.getVersionForNumber(version));
    if (numLetters > ((1 << numBits) - 1)) {
      throw new WriterException(numLetters + "is bigger than" + ((1 << numBits) - 1));
    }
    bits.appendBits(numLetters, numBits);
  }

  /**
   * Append "bytes" in "mode" mode (encoding) into "bits". On success, store the result in "bits".
   */
  public static function appendBytes(content:String , mode:Mode , bits:BitVector , encoding:String ):void
  {
    if (mode == Mode.NUMERIC) {
      appendNumericBytes(content, bits);
    } else if (mode == Mode.ALPHANUMERIC) {
      appendAlphanumericBytes(content, bits);
    } else if (mode == Mode.BYTE) {
      append8BitBytes(content, bits, encoding);
    } else if (mode == Mode.KANJI) {
      appendKanjiBytes(content, bits);
    } else {
      throw new WriterException("Invalid mode: " + mode);
    }
  }

  public static function appendNumericBytes(content:String , bits:BitVector):void
  {
    var length:int = content.length;
    var i:int = 0;
    while (i < length) {
      var num1:int = content.charCodeAt(i) - ('0').charCodeAt(0);
      if (i + 2 < length) {
        // Encode three numeric letters in ten bits.
        var num2:int = content.charCodeAt(i + 1) - ('0').charCodeAt(0);
        var num3:int = content.charCodeAt(i + 2) - ('0').charCodeAt(0);
        bits.appendBits(num1 * 100 + num2 * 10 + num3, 10);
        i += 3;
      } else if (i + 1 < length) {
        // Encode two numeric letters in seven bits.
        var num22:int = content.charCodeAt(i + 1) - ('0').charCodeAt(0);
        bits.appendBits(num1 * 10 + num22, 7);
        i += 2;
      } else {
        // Encode one numeric letter in four bits.
        bits.appendBits(num1, 4);
        i++;
      }
    }
  }

  public static function appendAlphanumericBytes(content:String , bits:BitVector ):void
  {
    var length:int = content.length;
    var i:int = 0;
    while (i < length) {
      var code1:int = getAlphanumericCode(content.charCodeAt(i));
      if (code1 == -1) {
        throw new WriterException();
      }
      if (i + 1 < length) {
        var code2:int = getAlphanumericCode(content.charCodeAt(i + 1));
        if (code2 == -1) {
          throw new WriterException();
        }
        // Encode two alphanumeric letters in 11 bits.
        bits.appendBits(code1 * 45 + code2, 11);
        i += 2;
      } else {
        // Encode one alphanumeric letter in six bits.
        bits.appendBits(code1, 6);
        i++;
      }
    }
  }

  public static function append8BitBytes(content:String , bits:BitVector , encoding:String ):void
  {
	var bytes:ByteArray = new ByteArray();
	
    try {

      //bytes = content.getBytes(encoding);
		if ((encoding == "Shift_JIS") || (encoding == "SJIS")) { bytes.writeMultiByte(content, "shift-jis");}
		else if (encoding == "Cp437")     { bytes.writeMultiByte(content, "IBM437"); }
		else if (encoding == "ISO8859_2") { bytes.writeMultiByte(content, "iso-8859-2"); }
    	else if (encoding == "ISO8859_3") { bytes.writeMultiByte(content, "iso-8859-3"); }
    	else if (encoding == "ISO8859_4") { bytes.writeMultiByte(content, "iso-8859-4"); }
    	else if (encoding == "ISO8859_5") { bytes.writeMultiByte(content, "iso-8859-5"); }
    	else if (encoding == "ISO8859_6") { bytes.writeMultiByte(content, "iso-8859-6"); }
    	else if (encoding == "ISO8859_7") { bytes.writeMultiByte(content, "iso-8859-7"); }
    	else if (encoding == "ISO8859_8") { bytes.writeMultiByte(content, "iso-8859-8"); }
    	else if (encoding == "ISO8859_9") { bytes.writeMultiByte(content, "iso-8859-9"); }
    	else if (encoding == "ISO8859_11"){ bytes.writeMultiByte(content, "iso-8859-11"); }
    	else if (encoding == "ISO8859_15"){ bytes.writeMultiByte(content, "iso-8859-15"); }
		else if ((encoding == "ISO-8859-1") || (encoding == "ISO8859-1")) { bytes.writeMultiByte(content, "iso-8859-1"); }
		else if ((encoding == "UTF-8") || (encoding == "UTF8")) { bytes.writeMultiByte(content, "utf-8"); }
		else
		{
			//other encodings not supported
			throw new Error("Encoding "+ encoding + " not supported");
							
		}
		bytes.position = 0; 

    } catch (uee:Error) {
      throw new WriterException(uee.toString());
    }
    for (var i:int = 0; i < bytes.length; ++i) {
      bits.appendBits(bytes[i], 8);
    }
  }

  public static function appendKanjiBytes(content:String , bits:BitVector ):void
  {
    var bytes:ByteArray;
    try {
		// we need data in the ShiftJis format
		//bytes = content.getBytes("Shift_JIS");
		bytes.writeMultiByte(content, "shift-jis");
		bytes.position = 0; 
      
    } catch (uee:Error) {
      throw new WriterException(uee.toString());
    }
    var length:int = bytes.length;
    for (var i:int = 0; i < length; i += 2) {
      var byte1:int = bytes[i] & 0xFF;
      var byte2:int = bytes[i + 1] & 0xFF;
      var code:int = (byte1 << 8) | byte2;
      var subtracted:int = -1;
      if (code >= 0x8140 && code <= 0x9ffc) {
        subtracted = code - 0x8140;
      } else if (code >= 0xe040 && code <= 0xebbf) {
        subtracted = code - 0xc140;
      }
      if (subtracted == -1) {
        throw new WriterException("Invalid byte sequence");
      }
      var encoded:int = ((subtracted >> 8) * 0xc0) + (subtracted & 0xff);
      bits.appendBits(encoded, 13);
    }
  }

  public static function appendECI(eci:CharacterSetECI , bits:BitVector ):void {
    bits.appendBits(Mode.ECI.getBits(), 4);
    // This is correct for values up to 127, which is all we need now.
    bits.appendBits(eci.getValue(), 8);
  }

}
}
