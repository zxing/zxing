/*
 * Copyright (C) 2010 ZXing authors
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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.zxing.oned.rss.expanded.decoders
{

	import com.google.zxing.common.BitArray;
	import com.google.zxing.common.flexdatatypes.StringBuilder;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public class GeneralAppIdDecoder {

  protected var information:BitArray;
  protected var current:CurrentParsingState = new CurrentParsingState();
  protected var buffer:StringBuilder = new StringBuilder();

  public function GeneralAppIdDecoder(information:BitArray ){
    this.information = information;
  }

  public function decodeAllCodes(buff:StringBuilder, initialPosition:int):String {
    var currentPosition:int = initialPosition;
    var remaining:String = null;
    do{
      var info:DecodedInformation = this.decodeGeneralPurposeField(currentPosition, remaining);
      var parsedFields:String = FieldParser.parseFieldsInGeneralPurpose(info.getNewString());
      if (parsedFields != null) {
        buff.Append(parsedFields);
      }
      if(info.isRemaining()) {
        remaining = info.getRemainingValue().toString();
      } else {
        remaining = null;
      }

      if(currentPosition == info.getNewPosition()) {// No step forward!
        break;
      }
      currentPosition = info.getNewPosition();
    }while(true);

    return buff.toString();
  }

  private function isStillNumeric(pos:int):Boolean {
    // It's numeric if it still has 7 positions
    // and one of the first 4 bits is "1".
    if(pos + 7 > this.information.Size){
      return pos + 4 <= this.information.Size;
    }

    for(var i:int = pos; i < pos + 3; ++i) {
      if (this.information._get(i)) {
        return true;
      }
    }

    return this.information._get(pos + 3);
  }

  private function decodeNumeric(pos:int):DecodedNumeric {
    if(pos + 7 > this.information.Size){
      var numeric:int = extractNumericValueFromBitArray2(pos, 4);
      if(numeric == 0) {
        return new DecodedNumeric(this.information.Size, DecodedNumeric.FNC1, DecodedNumeric.FNC1);
      }
      return new DecodedNumeric(this.information.Size, numeric - 1, DecodedNumeric.FNC1);
    }
    numeric = extractNumericValueFromBitArray2(pos, 7);

    var digit1:int  = int((numeric - 8) / 11);
    var digit2:int  = (numeric - 8) % 11;

    return new DecodedNumeric(pos + 7, digit1, digit2);
  }

  public function extractNumericValueFromBitArray2(pos:int, bits:int):int{
    return extractNumericValueFromBitArray(this.information, pos, bits);
  }

  public static function extractNumericValueFromBitArray(information:BitArray, pos:int, bits:int):int {

    if(bits > 32) {
      throw new IllegalArgumentException("extractNumberValueFromBitArray can't handle more than 32 bits");
    }

    var value:int = 0;
    for(var i:int = 0; i < bits; ++i) {
      if (information._get(pos + i)) {
        value |= (1 << (bits - i - 1));
      }
    }

    return value;
  }

  public function decodeGeneralPurposeField(pos:int, remaining:String):DecodedInformation {
    this.buffer.setLength(0);

    if(remaining != null) {
      this.buffer.Append(remaining);
    }

    this.current.position = pos;

    var lastDecoded:DecodedInformation = parseBlocks();
    if(lastDecoded != null && lastDecoded.isRemaining()) {
      return new DecodedInformation(this.current.position, this.buffer.toString(), lastDecoded.getRemainingValue());
    }
    return new DecodedInformation(this.current.position, this.buffer.toString());
  }

  private function parseBlocks():DecodedInformation {
    var isFinished:Boolean;
    var result:BlockParsedResult;
    do{
      var initialPosition:int = current.position;

      if (current.isAlpha()){
        result = parseAlphaBlock();
        isFinished = result.isFinished();
      }else if (current.isIsoIec646()){
        result = parseIsoIec646Block();
        isFinished = result.isFinished();
      }else{ // it must be numeric
        result = parseNumericBlock();
        isFinished = result.isFinished();
      }

      var positionChanged:Boolean = initialPosition != current.position;
      if(!positionChanged && !isFinished) {
        break;
      }
    } while (!isFinished);

    return result.getDecodedInformation();
  }

  private function parseNumericBlock():BlockParsedResult {
    while(isStillNumeric(current.position)){
      var numeric:DecodedNumeric = decodeNumeric(current.position);
      current.position = numeric.getNewPosition();

      if(numeric.isFirstDigitFNC1()){
        var information:DecodedInformation;
        if (numeric.isSecondDigitFNC1()) {
          information = new DecodedInformation(current.position, buffer.toString());
        } else {
          information = new DecodedInformation(current.position, buffer.toString(), numeric.getSecondDigit());
        }
        return new BlockParsedResult(information, true);
      }
      buffer.Append(String.fromCharCode(48+numeric.getFirstDigit()));

      if(numeric.isSecondDigitFNC1()){
        information = new DecodedInformation(current.position, buffer.toString());
        return new BlockParsedResult(information, true);
      }
      buffer.Append(String.fromCharCode(48+numeric.getSecondDigit()));
    }

    if(isNumericToAlphaNumericLatch(current.position)){
      current.setAlpha();
      current.position += 4;
    }
    return new BlockParsedResult(null,false);
  }

  private function parseIsoIec646Block():BlockParsedResult {
    while (isStillIsoIec646(current.position)) {
      var iso:DecodedChar = decodeIsoIec646(current.position);
      current.position = iso.getNewPosition();

      if (iso.isFNC1()) {
        var information:DecodedInformation = new DecodedInformation(current.position, buffer.toString());
        return new BlockParsedResult(information, true);
      }
      buffer.Append(iso.getValue());
    }

    if (isAlphaOr646ToNumericLatch(current.position)) {
      current.position  += 3;
      current.setNumeric();
    } else if (isAlphaTo646ToAlphaLatch(current.position)) {
      if (current.position + 5 < this.information.Size) {
        current.position += 5;
      } else {
        current.position = this.information.Size;
      }

      current.setAlpha();
    }
    return new BlockParsedResult(null,false);
  }

  private function parseAlphaBlock():BlockParsedResult {
    while (isStillAlpha(current.position)) {
      var alpha:DecodedChar = decodeAlphanumeric(current.position);
      current.position = alpha.getNewPosition();

      if(alpha.isFNC1()) {
        var information:DecodedInformation = new DecodedInformation(current.position, buffer.toString());
        return new BlockParsedResult(information, true); //end of the char block
      }

      buffer.Append(alpha.getValue());
    }

    if (isAlphaOr646ToNumericLatch(current.position)) {
      current.position += 3;
      current.setNumeric();
    } else if (isAlphaTo646ToAlphaLatch(current.position)) {
      if (current.position + 5 < this.information.Size) {
        current.position += 5;
      } else {
        current.position = this.information.Size;
      }

      current.setIsoIec646();
    }
    return new BlockParsedResult(null,false);
  }

  private function isStillIsoIec646(pos:int):Boolean 
  {
    if(pos + 5 > this.information.Size) {
      return false;
    }

    var fiveBitValue:int = extractNumericValueFromBitArray2(pos, 5);
    if(fiveBitValue >= 5 && fiveBitValue < 16) {
      return true;
    }

    if(pos + 7 > this.information.Size) {
      return false;
    }

    var sevenBitValue:int = extractNumericValueFromBitArray2(pos, 7);
    if(sevenBitValue >= 64 && sevenBitValue < 116) {
      return true;
    }

    if(pos + 8 > this.information.Size) {
      return false;
    }

    var eightBitValue:int = extractNumericValueFromBitArray2(pos, 8);
    return eightBitValue >= 232 && eightBitValue < 253;

  }

  private function decodeIsoIec646(pos:int):DecodedChar {
    var fiveBitValue:int = extractNumericValueFromBitArray2(pos, 5);
    if(fiveBitValue == 15) {
      return new DecodedChar(pos + 5, DecodedChar.FNC1);
    }

    if(fiveBitValue >= 5 && fiveBitValue < 15) {
      return new DecodedChar(pos + 5,  String.fromCharCode((48 + fiveBitValue - 5) as int));
    }

    var sevenBitValue:int = extractNumericValueFromBitArray2(pos, 7);

    if(sevenBitValue >= 64 && sevenBitValue < 90) {
      return new DecodedChar(pos + 7, (sevenBitValue + 1).toString());
    }

    if(sevenBitValue >= 90 && sevenBitValue < 116) {
      return new DecodedChar(pos + 7, String.fromCharCode(sevenBitValue + 7));
    }

    var eightBitValue:int = extractNumericValueFromBitArray2(pos, 8);
    switch (eightBitValue){
      case 232: return new DecodedChar(pos + 8, '!');
      case 233: return new DecodedChar(pos + 8, '"');
      case 234: return new DecodedChar(pos + 8, '%');
      case 235: return new DecodedChar(pos + 8, '&');
      case 236: return new DecodedChar(pos + 8, '\'');
      case 237: return new DecodedChar(pos + 8, '(');
      case 238: return new DecodedChar(pos + 8, ')');
      case 239: return new DecodedChar(pos + 8, '*');
      case 240: return new DecodedChar(pos + 8, '+');
      case 241: return new DecodedChar(pos + 8, ',');
      case 242: return new DecodedChar(pos + 8, '-');
      case 243: return new DecodedChar(pos + 8, '.');
      case 244: return new DecodedChar(pos + 8, '/');
      case 245: return new DecodedChar(pos + 8, ':');
      case 246: return new DecodedChar(pos + 8, ';');
      case 247: return new DecodedChar(pos + 8, '<');
      case 248: return new DecodedChar(pos + 8, '=');
      case 249: return new DecodedChar(pos + 8, '>');
      case 250: return new DecodedChar(pos + 8, '?');
      case 251: return new DecodedChar(pos + 8, '_');
      case 252: return new DecodedChar(pos + 8, ' ');
    }

    throw new Error("Decoding invalid ISO/IEC 646 value: " + eightBitValue);
  }

  private function isStillAlpha(pos:int):Boolean {
    if(pos + 5 > this.information.Size) {
      return false;
    }

    // We now check if it's a valid 5-bit value (0..9 and FNC1)
    var fiveBitValue:int = extractNumericValueFromBitArray2(pos, 5);
    if(fiveBitValue >= 5 && fiveBitValue < 16) {
      return true;
    }

    if(pos + 6 > this.information.Size) {
      return false;
    }

    var sixBitValue:int =  extractNumericValueFromBitArray2(pos, 6);
    return sixBitValue >= 16 && sixBitValue < 63; // 63 not included
  }

  private function decodeAlphanumeric(pos:int):DecodedChar {
    var fiveBitValue:int = extractNumericValueFromBitArray2(pos, 5);
    if(fiveBitValue == 15) {
      return new DecodedChar(pos + 5, DecodedChar.FNC1);
    }

    if(fiveBitValue >= 5 && fiveBitValue < 15) {
      return new DecodedChar(pos + 5,  String.fromCharCode((('0' as String).charCodeAt(0)  + fiveBitValue - 5)as int));
    }

    var sixBitValue:int =  extractNumericValueFromBitArray2(pos, 6);

    if(sixBitValue >= 32 && sixBitValue < 58) {
      return new DecodedChar(pos + 6, String.fromCharCode(sixBitValue + 33));
    }

    switch(sixBitValue){
      case 58: return new DecodedChar(pos + 6, '*');
      case 59: return new DecodedChar(pos + 6, ',');
      case 60: return new DecodedChar(pos + 6, '-');
      case 61: return new DecodedChar(pos + 6, '.');
      case 62: return new DecodedChar(pos + 6, '/');
    }

    throw new Error("Decoding invalid alphanumeric value: " + sixBitValue);
  }

  private function isAlphaTo646ToAlphaLatch(pos:int):Boolean {
    if(pos + 1 > this.information.Size) {
      return false;
    }

    for(var i:int = 0; i < 5 && i + pos < this.information.Size; ++i){
      if(i == 2){
        if(!this.information._get(pos + 2)) {
          return false;
        }
      } else if(this.information._get(pos + i)) {
        return false;
      }
    }

    return true;
  }

  private function isAlphaOr646ToNumericLatch(pos:int):Boolean {
    // Next is alphanumeric if there are 3 positions and they are all zeros
    if (pos + 3 > this.information.Size) {
      return false;
    }

    for (var i:int = pos; i < pos + 3; ++i) {
      if (this.information._get(i)) {
        return false;
      }
    }
    return true;
  }

  private function isNumericToAlphaNumericLatch(pos:int):Boolean {
    // Next is alphanumeric if there are 4 positions and they are all zeros, or
    // if there is a subset of this just before the end of the symbol
    if (pos + 1 > this.information.Size) {
      return false;
    }

    for (var i:int = 0; i < 4 && i + pos < this.information.Size; ++i) {
      if (this.information._get(pos + i)) {
        return false;
      }
    }
    return true;
  }
}
}