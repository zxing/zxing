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

import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.BitArray;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class GeneralAppIdDecoder {

  private final BitArray information;
  private final CurrentParsingState current = new CurrentParsingState();
  private final StringBuilder buffer = new StringBuilder();

  GeneralAppIdDecoder(BitArray information){
    this.information = information;
  }

  String decodeAllCodes(StringBuilder buff, int initialPosition) throws NotFoundException, FormatException {
    int currentPosition = initialPosition;
    String remaining = null;
    do{
      DecodedInformation info = this.decodeGeneralPurposeField(currentPosition, remaining);
      String parsedFields = FieldParser.parseFieldsInGeneralPurpose(info.getNewString());
      if (parsedFields != null) {
        buff.append(parsedFields);
      }
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
    if(pos + 7 > this.information.getSize()){
      return pos + 4 <= this.information.getSize();
    }

    for (int i = pos; i < pos + 3; ++i) {
      if (this.information.get(i)) {
        return true;
      }
    }

    return this.information.get(pos + 3);
  }

  private DecodedNumeric decodeNumeric(int pos) throws FormatException {
    if(pos + 7 > this.information.getSize()){
      int numeric = extractNumericValueFromBitArray(pos, 4);
      if(numeric == 0) {
        return new DecodedNumeric(this.information.getSize(), DecodedNumeric.FNC1, DecodedNumeric.FNC1);
      }
      return new DecodedNumeric(this.information.getSize(), numeric - 1, DecodedNumeric.FNC1);
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
    int value = 0;
    for (int i = 0; i < bits; ++i) {
      if (information.get(pos + i)) {
        value |= 1 << (bits - i - 1);
      }
    }

    return value;
  }

  DecodedInformation decodeGeneralPurposeField(int pos, String remaining) throws FormatException {
    this.buffer.setLength(0);

    if(remaining != null) {
      this.buffer.append(remaining);
    }

    this.current.setPosition(pos);

    DecodedInformation lastDecoded = parseBlocks();
    if(lastDecoded != null && lastDecoded.isRemaining()) {
      return new DecodedInformation(this.current.getPosition(), this.buffer.toString(), lastDecoded.getRemainingValue());
    }
    return new DecodedInformation(this.current.getPosition(), this.buffer.toString());
  }

  private DecodedInformation parseBlocks() throws FormatException {
    boolean isFinished;
    BlockParsedResult result;
    do{
      int initialPosition = current.getPosition();

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

      boolean positionChanged = initialPosition != current.getPosition();
      if(!positionChanged && !isFinished) {
        break;
      }
    } while (!isFinished);

    return result.getDecodedInformation();
  }

  private BlockParsedResult parseNumericBlock() throws FormatException {
    while (isStillNumeric(current.getPosition())) {
      DecodedNumeric numeric = decodeNumeric(current.getPosition());
      current.setPosition(numeric.getNewPosition());

      if(numeric.isFirstDigitFNC1()){
        DecodedInformation information;
        if (numeric.isSecondDigitFNC1()) {
          information = new DecodedInformation(current.getPosition(), buffer.toString());
        } else {
          information = new DecodedInformation(current.getPosition(), buffer.toString(), numeric.getSecondDigit());
        }
        return new BlockParsedResult(information, true);
      }
      buffer.append(numeric.getFirstDigit());

      if(numeric.isSecondDigitFNC1()){
        DecodedInformation information = new DecodedInformation(current.getPosition(), buffer.toString());
        return new BlockParsedResult(information, true);
      }
      buffer.append(numeric.getSecondDigit());
    }

    if (isNumericToAlphaNumericLatch(current.getPosition())) {
      current.setAlpha();
      current.incrementPosition(4);
    }
    return new BlockParsedResult(false);
  }

  private BlockParsedResult parseIsoIec646Block() throws FormatException {
    while (isStillIsoIec646(current.getPosition())) {
      DecodedChar iso = decodeIsoIec646(current.getPosition());
      current.setPosition(iso.getNewPosition());

      if (iso.isFNC1()) {
        DecodedInformation information = new DecodedInformation(current.getPosition(), buffer.toString());
        return new BlockParsedResult(information, true);
      }
      buffer.append(iso.getValue());
    }

    if (isAlphaOr646ToNumericLatch(current.getPosition())) {
      current.incrementPosition(3);
      current.setNumeric();
    } else if (isAlphaTo646ToAlphaLatch(current.getPosition())) {
      if (current.getPosition() + 5 < this.information.getSize()) {
        current.incrementPosition(5);
      } else {
        current.setPosition(this.information.getSize());
      }

      current.setAlpha();
    }
    return new BlockParsedResult(false);
  }

  private BlockParsedResult parseAlphaBlock() {
    while (isStillAlpha(current.getPosition())) {
      DecodedChar alpha = decodeAlphanumeric(current.getPosition());
      current.setPosition(alpha.getNewPosition());

      if(alpha.isFNC1()) {
        DecodedInformation information = new DecodedInformation(current.getPosition(), buffer.toString());
        return new BlockParsedResult(information, true); //end of the char block
      }

      buffer.append(alpha.getValue());
    }

    if (isAlphaOr646ToNumericLatch(current.getPosition())) {
      current.incrementPosition(3);
      current.setNumeric();
    } else if (isAlphaTo646ToAlphaLatch(current.getPosition())) {
      if (current.getPosition() + 5 < this.information.getSize()) {
        current.incrementPosition(5);
      } else {
        current.setPosition(this.information.getSize());
      }

      current.setIsoIec646();
    }
    return new BlockParsedResult(false);
  }

  private boolean isStillIsoIec646(int pos) {
    if (pos + 5 > this.information.getSize()) {
      return false;
    }

    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if (fiveBitValue >= 5 && fiveBitValue < 16) {
      return true;
    }

    if (pos + 7 > this.information.getSize()) {
      return false;
    }

    int sevenBitValue = extractNumericValueFromBitArray(pos, 7);
    if(sevenBitValue >= 64 && sevenBitValue < 116) {
      return true;
    }

    if (pos + 8 > this.information.getSize()) {
      return false;
    }

    int eightBitValue = extractNumericValueFromBitArray(pos, 8);
    return eightBitValue >= 232 && eightBitValue < 253;

  }

  private DecodedChar decodeIsoIec646(int pos) throws FormatException {
    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if (fiveBitValue == 15) {
      return new DecodedChar(pos + 5, DecodedChar.FNC1);
    }

    if (fiveBitValue >= 5 && fiveBitValue < 15) {
      return new DecodedChar(pos + 5, (char) ('0' + fiveBitValue - 5));
    }

    int sevenBitValue = extractNumericValueFromBitArray(pos, 7);

    if (sevenBitValue >= 64 && sevenBitValue < 90) {
      return new DecodedChar(pos + 7, (char) (sevenBitValue + 1));
    }

    if (sevenBitValue >= 90 && sevenBitValue < 116) {
      return new DecodedChar(pos + 7, (char) (sevenBitValue + 7));
    }

    int eightBitValue = extractNumericValueFromBitArray(pos, 8);
    char c;
    switch (eightBitValue) {
      case 232:
        c = '!';
        break;
      case 233:
        c = '"';
        break;
      case 234:
        c = '%';
        break;
      case 235:
        c = '&';
        break;
      case 236:
        c = '\'';
        break;
      case 237:
        c = '(';
        break;
      case 238:
        c = ')';
        break;
      case 239:
        c = '*';
        break;
      case 240:
        c = '+';
        break;
      case 241:
        c = ',';
        break;
      case 242:
        c = '-';
        break;
      case 243:
        c = '.';
        break;
      case 244:
        c = '/';
        break;
      case 245:
        c = ':';
        break;
      case 246:
        c = ';';
        break;
      case 247:
        c = '<';
        break;
      case 248:
        c = '=';
        break;
      case 249:
        c = '>';
        break;
      case 250:
        c = '?';
        break;
      case 251:
        c = '_';
        break;
      case 252:
        c = ' ';
        break;
      default:
        throw FormatException.getFormatInstance();
    }
    return new DecodedChar(pos + 8, c);
  }

  private boolean isStillAlpha(int pos) {
    if(pos + 5 > this.information.getSize()) {
      return false;
    }

    // We now check if it's a valid 5-bit value (0..9 and FNC1)
    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if (fiveBitValue >= 5 && fiveBitValue < 16) {
      return true;
    }

    if (pos + 6 > this.information.getSize()) {
      return false;
    }

    int sixBitValue =  extractNumericValueFromBitArray(pos, 6);
    return sixBitValue >= 16 && sixBitValue < 63; // 63 not included
  }

  private DecodedChar decodeAlphanumeric(int pos) {
    int fiveBitValue = extractNumericValueFromBitArray(pos, 5);
    if (fiveBitValue == 15) {
      return new DecodedChar(pos + 5, DecodedChar.FNC1);
    }

    if (fiveBitValue >= 5 && fiveBitValue < 15) {
      return new DecodedChar(pos + 5, (char) ('0' + fiveBitValue - 5));
    }

    int sixBitValue =  extractNumericValueFromBitArray(pos, 6);

    if (sixBitValue >= 32 && sixBitValue < 58) {
      return new DecodedChar(pos + 6, (char) (sixBitValue + 33));
    }

    char c;
    switch (sixBitValue){
      case 58:
        c = '*';
        break;
      case 59:
        c = ',';
        break;
      case 60:
        c = '-';
        break;
      case 61:
        c = '.';
        break;
      case 62:
        c = '/';
        break;
      default:
        throw new IllegalStateException("Decoding invalid alphanumeric value: " + sixBitValue);
    }
    return new DecodedChar(pos + 6, c);
  }

  private boolean isAlphaTo646ToAlphaLatch(int pos) {
    if (pos + 1 > this.information.getSize()) {
      return false;
    }

    for (int i = 0; i < 5 && i + pos < this.information.getSize(); ++i) {
      if(i == 2){
        if (!this.information.get(pos + 2)) {
          return false;
        }
      } else if (this.information.get(pos + i)) {
        return false;
      }
    }

    return true;
  }

  private boolean isAlphaOr646ToNumericLatch(int pos) {
    // Next is alphanumeric if there are 3 positions and they are all zeros
    if (pos + 3 > this.information.getSize()) {
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
    if (pos + 1 > this.information.getSize()) {
      return false;
    }

    for (int i = 0; i < 4 && i + pos < this.information.getSize(); ++i) {
      if (this.information.get(pos + i)) {
        return false;
      }
    }
    return true;
  }
}
