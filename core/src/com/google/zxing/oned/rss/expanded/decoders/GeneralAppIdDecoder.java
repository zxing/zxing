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

package com.google.zxing.oned.rss.expanded.decoders;

import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class GeneralAppIdDecoder {

  private final BitArray information;
  private final CurrentParsingState current = new CurrentParsingState();
  private final StringBuffer buffer = new StringBuffer();

  GeneralAppIdDecoder(BitArray information){
    this.information = information;
  }

  String decodeAllCodes(StringBuffer buff, int initialPosition) throws NotFoundException {
    int currentPosition = initialPosition;
    String remaining = null;
    do{
      DecodedInformation info = this.decodeGeneralPurposeField(currentPosition, remaining);
      String parsedFields = FieldParser.parseFieldsInGeneralPurpose(info.getNewString());
      buff.append(parsedFields);
      if(info.isRemaining()) {
        remaining = String.valueOf(info.getRemainingValue());
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

  private boolean isStillNumeric(int pos) {
    // It's numeric if it still has 7 positions
    // and one of the first 4 bits is "1".
    if(pos + 7 > this.information.size){
      return pos + 4 <= this.information.size;
    }

    for(int i = pos; i < pos + 3; ++i) {
      if (this.information.get(i)) {
        return true;
      }
    }

    return this.information.get(pos + 3);
  }

  private DecodedNumeric decodeNumeric(int pos) {
    if(pos + 7 > this.information.size){
      int numeric = extractNumericValueFromBitArray(pos, 4);
      if(numeric == 0) {
        return new DecodedNumeric(this.information.size, DecodedNumeric.FNC1, DecodedNumeric.FNC1);
      }
      return new DecodedNumeric(this.information.size, numeric - 1, DecodedNumeric.FNC1);
    }
    int numeric = extractNumericValueFromBitArray(pos, 7);

    int digit1  = (numeric - 8) / 11;
    int digit2  = (numeric - 8) % 11;

    return new DecodedNumeric(pos + 7, digit1, digit2);
  }

  int extractNumericValueFromBitArray(int pos, int bits){
    return extractNumericValueFromBitArray(this.information, pos, bits);
  }

  static int extractNumericValueFromBitArray(BitArray information, int pos, int bits) {
    if(bits > 32) {
      throw new IllegalArgumentException("extractNumberValueFromBitArray can't handle more than 32 bits");
    }

    int value = 0;
    for(int i = 0; i < bits; ++i) {
      if (information.get(pos + i)) {
        value |= (1 << (bits - i - 1));
      }
    }

    return value;
  }

  DecodedInformation decodeGeneralPurposeField(int pos, String remaining) {
    this.buffer.setLength(0);

    if(remaining != null) {
      this.buffer.append(remaining);
    }

    this.current.position = pos;

    DecodedInformation lastDecoded = parseBlocks();
    if(lastDecoded != null && lastDecoded.isRemaining()) {
      return new DecodedInformation(this.current.position, this.buffer.toString(), lastDecoded.getRemainingValue());
    }
    return new DecodedInformation(this.current.position, this.buffer.toString());
  }

  private DecodedInformation parseBlocks() {
    boolean isFinished;
    BlockParsedResult result;
    do{
      int initialPosition = current.position;

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

      boolean positionChanged = initialPosition != current.position;
      if(!positionChanged && !isFinished) {
        break;
      }
    } while (!isFinished);

    return result.getDecodedInformation();
  }

  private BlockParsedResult parseNumericBlock() {
    while(isStillNumeric(current.position)){
      DecodedNumeric numeric = decodeNumeric(current.position);
      current.position = numeric.getNewPosition();

      if(numeric.isFirstDigitFNC1()){
        DecodedInformation information;
        if (numeric.isSecondDigitFNC1()) {
          information = new DecodedInformation(current.position, buffer.toString());
        } else {
          information = new DecodedInformation(current.position, buffer.toString(), numeric.getSecondDigit());
        }
        return new BlockParsedResult(information, true);
      }
      buffer.append(numeric.getFirstDigit());

      if(numeric.isSecondDigitFNC1()){
        DecodedInformation information = new DecodedInformation(current.position, buffer.toString());
        return new BlockParsedResult(information, true);
      }
      buffer.append(numeric.getSecondDigit());
    }

    if(isNumericToAlphaNumericLatch(current.position)){
      current.setAlpha();
      current.position += 4;
    }
    return new BlockParsedResult(false);
  }

  private BlockParsedResult parseIsoIec646Block() {
    while (isStillIsoIec646(current.position)) {
      DecodedChar iso = decodeIsoIec646(current.position);
      current.position = iso.getNewPosition();

      if (iso.isFNC1()) {
        DecodedInformation information = new DecodedInformation(current.position, buffer.toString());
        return new BlockParsedResult(information, true);
      }
      buffer.append(iso.getValue());
    }

    if (isAlphaOr646ToNumericLatch(current.position)) {
      current.position  += 3;
      current.setNumeric();
    } else if (isAlphaTo646ToAlphaLatch(current.position)) {
      if (current.position + 5 < this.information.size) {
        current.position += 5;
      } else {
        current.position = this.information.size;
      }

      current.setAlpha();
    }
    return new BlockParsedResult(false);
  }

  private BlockParsedResult parseAlphaBlock() {
    while (isStillAlpha(current.position)) {
      DecodedChar alpha = decodeAlphanumeric(current.position);
      current.position = alpha.getNewPosition();

      if(alpha.isFNC1()) {
        DecodedInformation information = new DecodedInformation(current.position, buffer.toString());
        return new BlockParsedResult(information, true); //end of the char block
      }

      buffer.append(alpha.getValue());
    }

    if (isAlphaOr646ToNumericLatch(current.position)) {
      current.position += 3;
      current.setNumeric();
    } else if (isAlphaTo646ToAlphaLatch(current.position)) {
      if (current.position + 5 < this.information.size) {
        current.position += 5;
      } else {
        current.position = this.information.size;
      }

      current.setIsoIec646();
    }
    return new BlockParsedResult(false);
  }

  private boolean isStillIsoIec646(int pos) {
    if(pos + 5 > this.information.size) {
      return false;
    }

    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if(fiveBitValue >= 5 && fiveBitValue < 16) {
      return true;
    }

    if(pos + 7 > this.information.size) {
      return false;
    }

    int sevenBitValue = extractNumericValueFromBitArray(pos, 7);
    if(sevenBitValue >= 64 && sevenBitValue < 116) {
      return true;
    }

    if(pos + 8 > this.information.size) {
      return false;
    }

    int eightBitValue = extractNumericValueFromBitArray(pos, 8);
    return eightBitValue >= 232 && eightBitValue < 253;

  }

  private DecodedChar decodeIsoIec646(int pos) {
    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if(fiveBitValue == 15) {
      return new DecodedChar(pos + 5, DecodedChar.FNC1);
    }

    if(fiveBitValue >= 5 && fiveBitValue < 15) {
      return new DecodedChar(pos + 5, (char) ('0' + fiveBitValue - 5));
    }

    int sevenBitValue = extractNumericValueFromBitArray(pos, 7);

    if(sevenBitValue >= 64 && sevenBitValue < 90) {
      return new DecodedChar(pos + 7, (char) (sevenBitValue + 1));
    }

    if(sevenBitValue >= 90 && sevenBitValue < 116) {
      return new DecodedChar(pos + 7, (char) (sevenBitValue + 7));
    }

    int eightBitValue = extractNumericValueFromBitArray(pos, 8);
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

    throw new RuntimeException("Decoding invalid ISO/IEC 646 value: " + eightBitValue);
  }

  private boolean isStillAlpha(int pos) {
    if(pos + 5 > this.information.size) {
      return false;
    }

    // We now check if it's a valid 5-bit value (0..9 and FNC1)
    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if(fiveBitValue >= 5 && fiveBitValue < 16) {
      return true;
    }

    if(pos + 6 > this.information.size) {
      return false;
    }

    int sixBitValue =  extractNumericValueFromBitArray(pos, 6);
    return sixBitValue >= 16 && sixBitValue < 63; // 63 not included
  }

  private DecodedChar decodeAlphanumeric(int pos) {
    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if(fiveBitValue == 15) {
      return new DecodedChar(pos + 5, DecodedChar.FNC1);
    }

    if(fiveBitValue >= 5 && fiveBitValue < 15) {
      return new DecodedChar(pos + 5, (char) ('0' + fiveBitValue - 5));
    }

    int sixBitValue =  extractNumericValueFromBitArray(pos, 6);

    if(sixBitValue >= 32 && sixBitValue < 58) {
      return new DecodedChar(pos + 6, (char) (sixBitValue + 33));
    }

    switch(sixBitValue){
      case 58: return new DecodedChar(pos + 6, '*');
      case 59: return new DecodedChar(pos + 6, ',');
      case 60: return new DecodedChar(pos + 6, '-');
      case 61: return new DecodedChar(pos + 6, '.');
      case 62: return new DecodedChar(pos + 6, '/');
    }

    throw new RuntimeException("Decoding invalid alphanumeric value: " + sixBitValue);
  }

  private boolean isAlphaTo646ToAlphaLatch(int pos) {
    if(pos + 1 > this.information.size) {
      return false;
    }

    for(int i = 0; i < 5 && i + pos < this.information.size; ++i){
      if(i == 2){
        if(!this.information.get(pos + 2)) {
          return false;
        }
      } else if(this.information.get(pos + i)) {
        return false;
      }
    }

    return true;
  }

  private boolean isAlphaOr646ToNumericLatch(int pos) {
    // Next is alphanumeric if there are 3 positions and they are all zeros
    if (pos + 3 > this.information.size) {
      return false;
    }

    for (int i = pos; i < pos + 3; ++i) {
      if (this.information.get(i)) {
        return false;
      }
    }
    return true;
  }

  private boolean isNumericToAlphaNumericLatch(int pos) {
    // Next is alphanumeric if there are 4 positions and they are all zeros, or
    // if there is a subset of this just before the end of the symbol
    if (pos + 1 > this.information.size) {
      return false;
    }

    for (int i = 0; i < 4 && i + pos < this.information.size; ++i) {
      if (this.information.get(pos + i)) {
        return false;
      }
    }
    return true;
  }
}
