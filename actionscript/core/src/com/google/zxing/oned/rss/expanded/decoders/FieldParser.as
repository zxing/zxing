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

import com.google.zxing.NotFoundException;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
public class FieldParser {

  protected static var VARIABLE_LENGTH:Object = new Object();

  private static var TWO_DIGIT_DATA_LENGTH:Array = [
    // "DIGITS", new Integer(LENGTH)
    //    or
    // "DIGITS", VARIABLE_LENGTH, new Integer(MAX_SIZE)

    [ "00", new Array(18) ],
    [ "01", new Array(14) ],
    [ "02", new Array(14) ],

    [ "10", VARIABLE_LENGTH, new Array(20) ],
    [ "11", new Array(6) ],
    [ "12", new Array(6) ],
    [ "13", new Array(6) ],
    [ "15", new Array(6) ],
    [ "17", new Array(6) ],

    [ "20", new Array(2) ],
    [ "21", VARIABLE_LENGTH, new Array(20) ],
    [ "22", VARIABLE_LENGTH, new Array(29) ],

    [ "30", VARIABLE_LENGTH, new Array( 8) ],
    [ "37", VARIABLE_LENGTH, new Array( 8) ],

    //internal company codes
    [ "90", VARIABLE_LENGTH, new Array(30) ],
    [ "91", VARIABLE_LENGTH, new Array(30) ],
    [ "92", VARIABLE_LENGTH, new Array(30) ],
    [ "93", VARIABLE_LENGTH, new Array(30) ],
    [ "94", VARIABLE_LENGTH, new Array(30) ],
    [ "95", VARIABLE_LENGTH, new Array(30) ],
    [ "96", VARIABLE_LENGTH, new Array(30) ],
    [ "97", VARIABLE_LENGTH, new Array(30) ],
    [ "98", VARIABLE_LENGTH, new Array(30) ],
    [ "99", VARIABLE_LENGTH, new Array(30) ],
  ];

  private static var THREE_DIGIT_DATA_LENGTH:Array = [
    // Same format as above

    [ "240", VARIABLE_LENGTH, new Array(30) ],
    [ "241", VARIABLE_LENGTH, new Array(30) ],
    [ "242", VARIABLE_LENGTH, new Array( 6) ],
    [ "250", VARIABLE_LENGTH, new Array(30) ],
    [ "251", VARIABLE_LENGTH, new Array(30) ],
    [ "253", VARIABLE_LENGTH, new Array(17) ],
    [ "254", VARIABLE_LENGTH, new Array(20) ],

    [ "400", VARIABLE_LENGTH, new Array(30) ],
    [ "401", VARIABLE_LENGTH, new Array(30) ],
    [ "402", new Array(17) ],
    [ "403", VARIABLE_LENGTH, new Array(30) ],
    [ "410", new Array(13) ],
    [ "411", new Array(13) ],
    [ "412", new Array(13) ],
    [ "413", new Array(13) ],
    [ "414", new Array(13) ],
    [ "420", VARIABLE_LENGTH, new Array(20) ],
    [ "421", VARIABLE_LENGTH, new Array(15) ],
    [ "422", new Array( 3) ],
    [ "423", VARIABLE_LENGTH, new Array(15) ],
    [ "424", new Array(3) ],
    [ "425", new Array(3) ],
    [ "426", new Array(3) ],
  ];

  private static var THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH:Array = [
    // Same format as above

    [ "310", new Array(6) ],
    [ "311", new Array(6) ],
    [ "312", new Array(6) ],
    [ "313", new Array(6) ],
    [ "314", new Array(6) ],
    [ "315", new Array(6) ],
    [ "316", new Array(6) ],
    [ "320", new Array(6) ],
    [ "321", new Array(6) ],
    [ "322", new Array(6) ],
    [ "323", new Array(6) ],
    [ "324", new Array(6) ],
    [ "325", new Array(6) ],
    [ "326", new Array(6) ],
    [ "327", new Array(6) ],
    [ "328", new Array(6) ],
    [ "329", new Array(6) ],
    [ "330", new Array(6) ],
    [ "331", new Array(6) ],
    [ "332", new Array(6) ],
    [ "333", new Array(6) ],
    [ "334", new Array(6) ],
    [ "335", new Array(6) ],
    [ "336", new Array(6) ],
    [ "340", new Array(6) ],
    [ "341", new Array(6) ],
    [ "342", new Array(6) ],
    [ "343", new Array(6) ],
    [ "344", new Array(6) ],
    [ "345", new Array(6) ],
    [ "346", new Array(6) ],
    [ "347", new Array(6) ],
    [ "348", new Array(6) ],
    [ "349", new Array(6) ],
    [ "350", new Array(6) ],
    [ "351", new Array(6) ],
    [ "352", new Array(6) ],
    [ "353", new Array(6) ],
    [ "354", new Array(6) ],
    [ "355", new Array(6) ],
    [ "356", new Array(6) ],
    [ "357", new Array(6) ],
    [ "360", new Array(6) ],
    [ "361", new Array(6) ],
    [ "362", new Array(6) ],
    [ "363", new Array(6) ],
    [ "364", new Array(6) ],
    [ "365", new Array(6) ],
    [ "366", new Array(6) ],
    [ "367", new Array(6) ],
    [ "368", new Array(6) ],
    [ "369", new Array(6) ],
    [ "390", VARIABLE_LENGTH, new Array(15) ],
    [ "391", VARIABLE_LENGTH, new Array(18) ],
    [ "392", VARIABLE_LENGTH, new Array(15) ],
    [ "393", VARIABLE_LENGTH, new Array(18) ],
    [ "703", VARIABLE_LENGTH, new Array(30) ]
  ];

  private static var FOUR_DIGIT_DATA_LENGTH:Array = [
    // Same format as above

    [ "7001", new Array(13) ],
    [ "7002", VARIABLE_LENGTH, new Array(30) ],
    [ "7003", new Array(10) ],

    [ "8001", new Array(14) ],
    [ "8002", VARIABLE_LENGTH, new Array(20) ],
    [ "8003", VARIABLE_LENGTH, new Array(30) ],
    [ "8004", VARIABLE_LENGTH, new Array(30) ],
    [ "8005", new Array(6) ],
    [ "8006", new Array(18) ],
    [ "8007", VARIABLE_LENGTH, new Array(30) ],
    [ "8008", VARIABLE_LENGTH, new Array(12) ],
    [ "8018", new Array(18) ],
    [ "8020", VARIABLE_LENGTH, new Array(25) ],
    [ "8100", new Array(6) ],
    [ "8101", new Array(10) ],
    [ "8102", new Array(2) ],
    [ "8110", VARIABLE_LENGTH, new Array(30) ],
  ];

  public function FieldParser() {
  }

  public static function parseFieldsInGeneralPurpose(rawInformation:String):String{
    if(rawInformation.length == 0) {
      return null;
    }

    // Processing 2-digit AIs

    if(rawInformation.length < 2) 
    {
      throw NotFoundException.getNotFoundInstance();
    }

    var firstTwoDigits:String = rawInformation.substring(0, 2);
    for (var i:int=0; i<TWO_DIGIT_DATA_LENGTH.length; ++i)
    {
      if (TWO_DIGIT_DATA_LENGTH[i][0] == firstTwoDigits)
      {
        if(TWO_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) 
        {
          return processVariableAI(2, (TWO_DIGIT_DATA_LENGTH[i][2] as Array).length, rawInformation);
        }
        return processFixedAI(2,( TWO_DIGIT_DATA_LENGTH[i][1] as Array).length, rawInformation);
      }
    }

    if(rawInformation.length < 3) {
      throw NotFoundException.getNotFoundInstance();
    }

    var firstThreeDigits:String = rawInformation.substring(0, 3);

    for (i=0; i<THREE_DIGIT_DATA_LENGTH.length; ++i)
    {
      if (THREE_DIGIT_DATA_LENGTH[i][0] == firstThreeDigits)
      {
        if (THREE_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) 
        {
          return processVariableAI(3, (THREE_DIGIT_DATA_LENGTH[i][2] as Array).length, rawInformation);
        }
        return processFixedAI(3, (THREE_DIGIT_DATA_LENGTH[i][1] as Array).length, rawInformation);
      }
    }


    for (i=0; i<THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH.length; ++i)
    {
      if (THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][0] == firstThreeDigits)
      {
        if (THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) 
        {
          return processVariableAI(4, (THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][2] as Array).length, rawInformation);
        }
        return processFixedAI(4, (THREE_DIGIT_PLUS_DIGIT_DATA_LENGTH[i][1] as Array).length, rawInformation);
      }
    }

    if(rawInformation.length < 4) 
    {
      throw NotFoundException.getNotFoundInstance();
    }

    var firstFourDigits:String = rawInformation.substring(0, 4);

    for (i=0; i<FOUR_DIGIT_DATA_LENGTH.length; ++i)
    {
      if (FOUR_DIGIT_DATA_LENGTH[i][0] == firstFourDigits)
      {
        if (FOUR_DIGIT_DATA_LENGTH[i][1] == VARIABLE_LENGTH) 
        {
          return processVariableAI(4, (FOUR_DIGIT_DATA_LENGTH[i][2] as Array).length, rawInformation);
        }
        return processFixedAI(4, (FOUR_DIGIT_DATA_LENGTH[i][1] as Array).length, rawInformation);
      }
    }

    throw NotFoundException.getNotFoundInstance();
  }

  private static function processFixedAI(aiSize:int, fieldSize:int, rawInformation:String):String{
    if (rawInformation.length < aiSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    var ai:String = rawInformation.substring(0, aiSize);

    if(rawInformation.length < aiSize + fieldSize) {
      throw NotFoundException.getNotFoundInstance();
    }

    var field:String = rawInformation.substring(aiSize, aiSize + fieldSize);
    var remaining:String = rawInformation.substring(aiSize + fieldSize);
    var result:String = '(' + ai + ')' + field;
    var parsedAI:String = parseFieldsInGeneralPurpose(remaining);
    return parsedAI == null ? result : result + parsedAI;
  }

  private static function processVariableAI(aiSize:int, variableFieldSize:int, rawInformation:String):String {
    var ai:String = rawInformation.substring(0, aiSize);
    var maxSize:int;
    if (rawInformation.length < aiSize + variableFieldSize) {
      maxSize = rawInformation.length;
    } else {
      maxSize = aiSize + variableFieldSize;
    }
    var field:String = rawInformation.substring(aiSize, maxSize);
    var remaining:String = rawInformation.substring(maxSize);
    var result:String = '(' + ai + ')' + field;
    var parsedAI:String = parseFieldsInGeneralPurpose(remaining);
    return parsedAI == null ? result : result + parsedAI;
  }
}
}