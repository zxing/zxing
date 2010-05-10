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

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class FieldParser {

  private static final Object VARIABLE_LENGTH = new Object();

  private static final Object [][] TWO_DIGIT_DATA_LENGTH = {
    // "DIGITS", new Integer(LENGTH)
    //    or
    // "DIGITS", VARIABLE_LENGTH, new Integer(MAX_SIZE)

    { "00", new Integer(18) },
    { "01", new Integer(14) },
    { "02", new Integer(14) },

    { "10", VARIABLE_LENGTH, new Integer(20) },
    { "11", new Integer(6) },
    { "12", new Integer(6) },
    { "13", new Integer(6) },
    { "15", new Integer(6) },
    { "17", new Integer(6) },

    { "20", new Integer(2) },
    { "21", VARIABLE_LENGTH, new Integer(20) },
    { "22", VARIABLE_LENGTH, new Integer(29) },

    { "30", VARIABLE_LENGTH, new Integer( 8) },
    { "37", VARIABLE_LENGTH, new Integer( 8) },

    //internal company codes
    { "90", VARIABLE_LENGTH, new Integer(30) },
    { "91", VARIABLE_LENGTH, new Integer(30) },
    { "92", VARIABLE_LENGTH, new Integer(30) },
    { "93", VARIABLE_LENGTH, new Integer(30) },
    { "94", VARIABLE_LENGTH, new Integer(30) },
    { "95", VARIABLE_LENGTH, new Integer(30) },
    { "96", VARIABLE_LENGTH, new Integer(30) },
    { "97", VARIABLE_LENGTH, new Integer(30) },
    { "98", VARIABLE_LENGTH, new Integer(30) },
    { "99", VARIABLE_LENGTH, new Integer(30) },
  };

  private static final Object [][] THREE_DIGIT_DATA_LENGTH = {
    // Same format as above

    { "240", VARIABLE_LENGTH, new Integer(30) },
    { "241", VARIABLE_LENGTH, new Integer(30) },
    { "242", VARIABLE_LENGTH, new Integer( 6) },
    { "250", VARIABLE_LENGTH, new Integer(30) },
    { "251", VARIABLE_LENGTH, new Integer(30) },
    { "253", VARIABLE_LENGTH, new Integer(17) },
    { "254", VARIABLE_LENGTH, new Integer(20) },

    { "400", VARIABLE_LENGTH, new Integer(30) },
    { "401", VARIABLE_LENGTH, new Integer(30) },
    { "402", new Integer(17) },
    { "403", VARIABLE_LENGTH, new Integer(30) },
    { "410", new Integer(13) },
    { "411", new Integer(13) },
    { "412", new Integer(13) },
    { "413", new Integer(13) },
    { "414", new Integer(13) },
    { "420", VARIABLE_LENGTH, new Integer(20) },
    { "421", VARIABLE_LENGTH, new Integer(15) },
    { "422", new Integer( 3) },
    { "423", VARIABLE_LENGTH, new Integer(15) },
    { "424", new Integer(3) },
    { "425", new Integer(3) },
    { "426", new Integer(3) },
  };

  private static final Object [][] THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH = {
    // Same format as above

    { "310", new Integer(6) },
    { "311", new Integer(6) },
    { "312", new Integer(6) },
    { "313", new Integer(6) },
    { "314", new Integer(6) },
    { "315", new Integer(6) },
    { "316", new Integer(6) },
    { "320", new Integer(6) },
    { "321", new Integer(6) },
    { "322", new Integer(6) },
    { "323", new Integer(6) },
    { "324", new Integer(6) },
    { "325", new Integer(6) },
    { "326", new Integer(6) },
    { "327", new Integer(6) },
    { "328", new Integer(6) },
    { "329", new Integer(6) },
    { "330", new Integer(6) },
    { "331", new Integer(6) },
    { "332", new Integer(6) },
    { "333", new Integer(6) },
    { "334", new Integer(6) },
    { "335", new Integer(6) },
    { "336", new Integer(6) },
    { "340", new Integer(6) },
    { "341", new Integer(6) },
    { "342", new Integer(6) },
    { "343", new Integer(6) },
    { "344", new Integer(6) },
    { "345", new Integer(6) },
    { "346", new Integer(6) },
    { "347", new Integer(6) },
    { "348", new Integer(6) },
    { "349", new Integer(6) },
    { "350", new Integer(6) },
    { "351", new Integer(6) },
    { "352", new Integer(6) },
    { "353", new Integer(6) },
    { "354", new Integer(6) },
    { "355", new Integer(6) },
    { "356", new Integer(6) },
    { "357", new Integer(6) },
    { "360", new Integer(6) },
    { "361", new Integer(6) },
    { "362", new Integer(6) },
    { "363", new Integer(6) },
    { "364", new Integer(6) },
    { "365", new Integer(6) },
    { "366", new Integer(6) },
    { "367", new Integer(6) },
    { "368", new Integer(6) },
    { "369", new Integer(6) },
    { "390", VARIABLE_LENGTH, new Integer(15) },
    { "391", VARIABLE_LENGTH, new Integer(18) },
    { "392", VARIABLE_LENGTH, new Integer(15) },
    { "393", VARIABLE_LENGTH, new Integer(18) },
    { "703", VARIABLE_LENGTH, new Integer(30) }
  };

  private static final Object [][] FOUR_DIGIT_DATA_LENGTH = {
    // Same format as above

    { "7001", new Integer(13) },
    { "7002", VARIABLE_LENGTH, new Integer(30) },
    { "7003", new Integer(10) },

    { "8001", new Integer(14) },
    { "8002", VARIABLE_LENGTH, new Integer(20) },
    { "8003", VARIABLE_LENGTH, new Integer(30) },
    { "8004", VARIABLE_LENGTH, new Integer(30) },
    { "8005", new Integer(6) },
    { "8006", new Integer(18) },
    { "8007", VARIABLE_LENGTH, new Integer(30) },
    { "8008", VARIABLE_LENGTH, new Integer(12) },
    { "8018", new Integer(18) },
    { "8020", VARIABLE_LENGTH, new Integer(25) },
    { "8100", new Integer(6) },
    { "8101", new Integer(10) },
    { "8102", new Integer(2) },
    { "8110", VARIABLE_LENGTH, new Integer(30) },
  };

  private FieldParser() {
  }

  static String parseFieldsInGeneralPurpose(String rawInformation) throws NotFoundException{
    if(rawInformation.length() == 0) {
      return "";
    }

    // Processing 2-digit AIs

    if(rawInformation.length() < 2) {
      throw NotFoundException.getNotFoundInstance();
    }

    String firstTwoDigits = rawInformation.substring(0, 2);

    for (int i=0; i<TWO_DIGIT_DATA_LENGTH.length; ++i){
      if (TWO_DIGIT_DATA_LENGTH[i][0].equals(firstTwoDigits)){
        if(TWO_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) {
          return processVariableAI(2, ((Integer) TWO_DIGIT_DATA_LENGTH[i][2]).intValue(), rawInformation);
        }
        return processFixedAI(2, ((Integer)TWO_DIGIT_DATA_LENGTH[i][1]).intValue(), rawInformation);
      }
    }

    if(rawInformation.length() < 3) {
      throw NotFoundException.getNotFoundInstance();
    }

    String firstThreeDigits = rawInformation.substring(0, 3);

    for (int i=0; i<THREE_DIGIT_DATA_LENGTH.length; ++i){
      if (THREE_DIGIT_DATA_LENGTH[i][0].equals(firstThreeDigits)){
        if (THREE_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) {
          return processVariableAI(3, ((Integer) THREE_DIGIT_DATA_LENGTH[i][2]).intValue(), rawInformation);
        }
        return processFixedAI(3, ((Integer)THREE_DIGIT_DATA_LENGTH[i][1]).intValue(), rawInformation);
      }
    }


    for (int i=0; i<THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.length; ++i){
      if (THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][0].equals(firstThreeDigits)){
        if (THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) {
          return processVariableAI(4, ((Integer) THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][2]).intValue(), rawInformation);
        }
        return processFixedAI(4, ((Integer)THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][1]).intValue(), rawInformation);
      }
    }

    if(rawInformation.length() < 4) {
      throw NotFoundException.getNotFoundInstance();
    }

    String firstFourDigits = rawInformation.substring(0, 4);

    for (int i=0; i<FOUR_DIGIT_DATA_LENGTH.length; ++i){
      if (FOUR_DIGIT_DATA_LENGTH[i][0].equals(firstFourDigits)){
        if (FOUR_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) {
          return processVariableAI(4, ((Integer) FOUR_DIGIT_DATA_LENGTH[i][2]).intValue(), rawInformation);
        }
        return processFixedAI(4, ((Integer)FOUR_DIGIT_DATA_LENGTH[i][1]).intValue(), rawInformation);
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  private static String processFixedAI(int aiSize, int fieldSize, String rawInformation) throws NotFoundException{
    if (rawInformation.length() < aiSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    String ai = rawInformation.substring(0, aiSize);

    if(rawInformation.length() < aiSize + fieldSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    String field = rawInformation.substring(aiSize, aiSize + fieldSize);
    String remaining = rawInformation.substring(aiSize + fieldSize);
    return '(' + ai + ')' + field + parseFieldsInGeneralPurpose(remaining);
  }

  private static String processVariableAI(int aiSize, int variableFieldSize, String rawInformation) throws NotFoundException {
    String ai = rawInformation.substring(0, aiSize);
    int maxSize;
    if (rawInformation.length() < aiSize + variableFieldSize) {
      maxSize = rawInformation.length();
    } else {
      maxSize = aiSize + variableFieldSize;
    }
    String field = rawInformation.substring(aiSize, maxSize);
    String remaining = rawInformation.substring(maxSize);
    return '(' + ai + ')' + field + parseFieldsInGeneralPurpose(remaining);
  }
}
