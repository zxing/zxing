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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class FieldParser {

  private static final Map<String,DataLength> TWO_DIGIT_DATA_LENGTH = new HashMap<>();
  static {
    TWO_DIGIT_DATA_LENGTH.put("00", DataLength.fixed(18));
    TWO_DIGIT_DATA_LENGTH.put("01", DataLength.fixed(14));
    TWO_DIGIT_DATA_LENGTH.put("02", DataLength.fixed(14));
    TWO_DIGIT_DATA_LENGTH.put("10", DataLength.variable(20));
    TWO_DIGIT_DATA_LENGTH.put("11", DataLength.fixed(6));
    TWO_DIGIT_DATA_LENGTH.put("12", DataLength.fixed(6));
    TWO_DIGIT_DATA_LENGTH.put("13", DataLength.fixed(6));
    TWO_DIGIT_DATA_LENGTH.put("15", DataLength.fixed(6));
    TWO_DIGIT_DATA_LENGTH.put("16", DataLength.fixed(6));
    TWO_DIGIT_DATA_LENGTH.put("17", DataLength.fixed(6));
    TWO_DIGIT_DATA_LENGTH.put("20", DataLength.fixed(2));
    TWO_DIGIT_DATA_LENGTH.put("21", DataLength.variable(20));
    TWO_DIGIT_DATA_LENGTH.put("22", DataLength.variable(29)); // limited to 20 in latest versions of spec
    TWO_DIGIT_DATA_LENGTH.put("30", DataLength.variable(8));
    TWO_DIGIT_DATA_LENGTH.put("37", DataLength.variable(8));
    //internal company codes
    for (int i = 90; i <= 99; i++) {
      TWO_DIGIT_DATA_LENGTH.put(String.valueOf(i), DataLength.variable(30));
    }
  }

  private static final Map<String,DataLength> THREE_DIGIT_DATA_LENGTH = new HashMap<>();
  static {
    THREE_DIGIT_DATA_LENGTH.put("235", DataLength.variable(28));
    THREE_DIGIT_DATA_LENGTH.put("240", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("241", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("242", DataLength.variable(6));
    THREE_DIGIT_DATA_LENGTH.put("243", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("250", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("251", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("253", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("254", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("255", DataLength.variable(25));
    THREE_DIGIT_DATA_LENGTH.put("400", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("401", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("402", DataLength.fixed(17));
    THREE_DIGIT_DATA_LENGTH.put("403", DataLength.variable(30));
    THREE_DIGIT_DATA_LENGTH.put("410", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("411", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("412", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("413", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("414", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("415", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("416", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("417", DataLength.fixed(13));
    THREE_DIGIT_DATA_LENGTH.put("420", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("421", DataLength.variable(15)); // limited to 12 in latest versions of spec
    THREE_DIGIT_DATA_LENGTH.put("422", DataLength.fixed(3));
    THREE_DIGIT_DATA_LENGTH.put("423", DataLength.variable(15));
    THREE_DIGIT_DATA_LENGTH.put("424", DataLength.fixed(3));
    THREE_DIGIT_DATA_LENGTH.put("425", DataLength.variable(15));
    THREE_DIGIT_DATA_LENGTH.put("426", DataLength.fixed(3));
    THREE_DIGIT_DATA_LENGTH.put("427", DataLength.variable(3));
    THREE_DIGIT_DATA_LENGTH.put("710", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("711", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("712", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("713", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("714", DataLength.variable(20));
    THREE_DIGIT_DATA_LENGTH.put("715", DataLength.variable(20));
  }

  private static final Map<String,DataLength> THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH = new HashMap<>();
  static {
    for (int i = 310; i <= 316; i++) {
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), DataLength.fixed(6));
    }
    for (int i = 320; i <= 337; i++) {
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), DataLength.fixed(6));
    }
    for (int i = 340; i <= 357; i++) {
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), DataLength.fixed(6));
    }
    for (int i = 360; i <= 369; i++) {
      THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put(String.valueOf(i), DataLength.fixed(6));
    }
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("390", DataLength.variable(15));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("391", DataLength.variable(18));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("392", DataLength.variable(15));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("393", DataLength.variable(18));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("394", DataLength.fixed(4));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("395", DataLength.fixed(6));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("703", DataLength.variable(30));
    THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.put("723", DataLength.variable(30));
  }

  private static final Map<String,DataLength> FOUR_DIGIT_DATA_LENGTH = new HashMap<>();
  static {
    FOUR_DIGIT_DATA_LENGTH.put("4300", DataLength.variable(35));
    FOUR_DIGIT_DATA_LENGTH.put("4301", DataLength.variable(35));
    FOUR_DIGIT_DATA_LENGTH.put("4302", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4303", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4304", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4305", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4306", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4307", DataLength.fixed(2));
    FOUR_DIGIT_DATA_LENGTH.put("4308", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("4309", DataLength.fixed(20));
    FOUR_DIGIT_DATA_LENGTH.put("4310", DataLength.variable(35));
    FOUR_DIGIT_DATA_LENGTH.put("4311", DataLength.variable(35));
    FOUR_DIGIT_DATA_LENGTH.put("4312", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4313", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4314", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4315", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4316", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("4317", DataLength.fixed(2));
    FOUR_DIGIT_DATA_LENGTH.put("4318", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("4319", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("4320", DataLength.variable(35));
    FOUR_DIGIT_DATA_LENGTH.put("4321", DataLength.fixed(1));
    FOUR_DIGIT_DATA_LENGTH.put("4322", DataLength.fixed(1));
    FOUR_DIGIT_DATA_LENGTH.put("4323", DataLength.fixed(1));
    FOUR_DIGIT_DATA_LENGTH.put("4324", DataLength.fixed(10));
    FOUR_DIGIT_DATA_LENGTH.put("4325", DataLength.fixed(10));
    FOUR_DIGIT_DATA_LENGTH.put("4326", DataLength.fixed(6));
    FOUR_DIGIT_DATA_LENGTH.put("7001", DataLength.fixed(13));
    FOUR_DIGIT_DATA_LENGTH.put("7002", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("7003", DataLength.fixed(10));
    FOUR_DIGIT_DATA_LENGTH.put("7004", DataLength.variable(4));
    FOUR_DIGIT_DATA_LENGTH.put("7005", DataLength.variable(12));
    FOUR_DIGIT_DATA_LENGTH.put("7006", DataLength.fixed(6));
    FOUR_DIGIT_DATA_LENGTH.put("7007", DataLength.variable(12));
    FOUR_DIGIT_DATA_LENGTH.put("7008", DataLength.variable(3));
    FOUR_DIGIT_DATA_LENGTH.put("7009", DataLength.variable(10));
    FOUR_DIGIT_DATA_LENGTH.put("7010", DataLength.variable(2));
    FOUR_DIGIT_DATA_LENGTH.put("7011", DataLength.variable(10));
    FOUR_DIGIT_DATA_LENGTH.put("7020", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("7021", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("7022", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("7023", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("7040", DataLength.fixed(4));
    FOUR_DIGIT_DATA_LENGTH.put("7240", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("8001", DataLength.fixed(14));
    FOUR_DIGIT_DATA_LENGTH.put("8002", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("8003", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("8004", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("8005", DataLength.fixed(6));
    FOUR_DIGIT_DATA_LENGTH.put("8006", DataLength.fixed(18));
    FOUR_DIGIT_DATA_LENGTH.put("8007", DataLength.variable(34));
    FOUR_DIGIT_DATA_LENGTH.put("8008", DataLength.variable(12));
    FOUR_DIGIT_DATA_LENGTH.put("8009", DataLength.variable(50));
    FOUR_DIGIT_DATA_LENGTH.put("8010", DataLength.variable(30));
    FOUR_DIGIT_DATA_LENGTH.put("8011", DataLength.variable(12));
    FOUR_DIGIT_DATA_LENGTH.put("8012", DataLength.variable(20));
    FOUR_DIGIT_DATA_LENGTH.put("8013", DataLength.variable(25));
    FOUR_DIGIT_DATA_LENGTH.put("8017", DataLength.fixed(18));
    FOUR_DIGIT_DATA_LENGTH.put("8018", DataLength.fixed(18));
    FOUR_DIGIT_DATA_LENGTH.put("8019", DataLength.variable(10));
    FOUR_DIGIT_DATA_LENGTH.put("8020", DataLength.variable(25));
    FOUR_DIGIT_DATA_LENGTH.put("8026", DataLength.fixed(18));
    FOUR_DIGIT_DATA_LENGTH.put("8100", DataLength.fixed(6)); // removed from latest versions of spec
    FOUR_DIGIT_DATA_LENGTH.put("8101", DataLength.fixed(10)); // removed from latest versions of spec
    FOUR_DIGIT_DATA_LENGTH.put("8102", DataLength.fixed(2)); // removed from latest versions of spec
    FOUR_DIGIT_DATA_LENGTH.put("8110", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("8111", DataLength.fixed(4));
    FOUR_DIGIT_DATA_LENGTH.put("8112", DataLength.variable(70));
    FOUR_DIGIT_DATA_LENGTH.put("8200", DataLength.variable(70));
  }

  private FieldParser() {
  }

  static String parseFieldsInGeneralPurpose(String rawInformation) throws NotFoundException {
    if (rawInformation.isEmpty()) {
      return null;
    }

    // Processing 2-digit AIs

    if (rawInformation.length() < 2) {
      throw NotFoundException.getNotFoundInstance();
    }

    DataLength twoDigitDataLength = TWO_DIGIT_DATA_LENGTH.get(rawInformation.substring(0, 2));
    if (twoDigitDataLength != null) {
      if (twoDigitDataLength.variable) {
        return processVariableAI(2, twoDigitDataLength.length, rawInformation);
      }
      return processFixedAI(2, twoDigitDataLength.length, rawInformation);
    }

    if (rawInformation.length() < 3) {
      throw NotFoundException.getNotFoundInstance();
    }

    String firstThreeDigits = rawInformation.substring(0, 3);
    DataLength threeDigitDataLength = THREE_DIGIT_DATA_LENGTH.get(firstThreeDigits);
    if (threeDigitDataLength != null) {
      if (threeDigitDataLength.variable) {
        return processVariableAI(3, threeDigitDataLength.length, rawInformation);
      }
      return processFixedAI(3, threeDigitDataLength.length, rawInformation);
    }

    if (rawInformation.length() < 4) {
      throw NotFoundException.getNotFoundInstance();
    }

    DataLength threeDigitPlusDigitDataLength = THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.get(firstThreeDigits);
    if (threeDigitPlusDigitDataLength != null) {
      if (threeDigitPlusDigitDataLength.variable) {
        return processVariableAI(4, threeDigitPlusDigitDataLength.length, rawInformation);
      }
      return processFixedAI(4, threeDigitPlusDigitDataLength.length, rawInformation);
    }

    DataLength firstFourDigitLength = FOUR_DIGIT_DATA_LENGTH.get(rawInformation.substring(0, 4));
    if (firstFourDigitLength != null) {
      if (firstFourDigitLength.variable) {
        return processVariableAI(4, firstFourDigitLength.length, rawInformation);
      }
      return processFixedAI(4, firstFourDigitLength.length, rawInformation);
    }

    throw NotFoundException.getNotFoundInstance();
  }

  private static String processFixedAI(int aiSize, int fieldSize, String rawInformation) throws NotFoundException {
    if (rawInformation.length() < aiSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    String ai = rawInformation.substring(0, aiSize);

    if (rawInformation.length() < aiSize + fieldSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    String field = rawInformation.substring(aiSize, aiSize + fieldSize);
    String remaining = rawInformation.substring(aiSize + fieldSize);
    String result = '(' + ai + ')' + field;
    String parsedAI = parseFieldsInGeneralPurpose(remaining);
    return parsedAI == null ? result : result + parsedAI;
  }

  private static String processVariableAI(int aiSize, int variableFieldSize, String rawInformation)
      throws NotFoundException {
    String ai = rawInformation.substring(0, aiSize);
    int maxSize = Math.min(rawInformation.length(), aiSize + variableFieldSize);
    String field = rawInformation.substring(aiSize, maxSize);
    String remaining = rawInformation.substring(maxSize);
    String result = '(' + ai + ')' + field;
    String parsedAI = parseFieldsInGeneralPurpose(remaining);
    return parsedAI == null ? result : result + parsedAI;
  }

  private static final class DataLength {

    final boolean variable;
    final int length;

    private DataLength(boolean variable, int length) {
      this.variable = variable;
      this.length = length;
    }

    static DataLength fixed(int length) {
      return new DataLength(false, length);
    }

    static DataLength variable(int length) {
      return new DataLength(true, length);
    }

  }

}
