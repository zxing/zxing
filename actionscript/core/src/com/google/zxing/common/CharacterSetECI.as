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
 package com.google.zxing.common
{
 /**
 * Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1
 * of ISO 18004.
 *
 * @author Sean Owen
 */
   public  class CharacterSetECI extends ECI
    { 
    	import com.google.zxing.common.flexdatatypes.HashTable;
    	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
    	
        private static var  VALUE_TO_ECI:HashTable = new HashTable(29);
        private static var  NAME_TO_ECI:HashTable = new HashTable(29);

  private static function initialize():void {
    VALUE_TO_ECI = new HashTable(29);
    NAME_TO_ECI = new HashTable(29);
    // TODO figure out if these values are even right!
    addCharacterSet(0, "Cp437");
    addCharacterSet(1, ["ISO8859_1", "ISO-8859-1"]);
    addCharacterSet(2, "Cp437");
    addCharacterSet(3, ["ISO8859_1", "ISO-8859-1"]);
    addCharacterSet(4, "ISO8859_2");
    addCharacterSet(5, "ISO8859_3");
    addCharacterSet(6, "ISO8859_4");
    addCharacterSet(7, "ISO8859_5");
    addCharacterSet(8, "ISO8859_6");
    addCharacterSet(9, "ISO8859_7");
    addCharacterSet(10, "ISO8859_8");
    addCharacterSet(11, "ISO8859_9");
    addCharacterSet(12, "ISO8859_10");
    addCharacterSet(13, "ISO8859_11");
    addCharacterSet(15, "ISO8859_13");
    addCharacterSet(16, "ISO8859_14");
    addCharacterSet(17, "ISO8859_15");
    addCharacterSet(18, "ISO8859_16");
    addCharacterSet(20,  ["SJIS", "Shift_JIS"]);
  }

  private var encodingName:String;

  public function CharacterSetECI(value:int, encodingName:String) {
    super(value);
    this.encodingName = encodingName;
  }

  public function getEncodingName():String {
    return encodingName;
  }

 private static function addCharacterSet(value:int, encodingNames:Object):void 
  {
  	var eci:CharacterSetECI;
  	if (encodingNames is String)
  	{
  		eci = new CharacterSetECI(value, encodingNames as String);
    	VALUE_TO_ECI._put(value, eci);
    	NAME_TO_ECI._put(encodingNames as String, eci);
  	}
  	else if (encodingNames is Array)
  	{
    	eci = new CharacterSetECI(value, encodingNames[0]);
    	VALUE_TO_ECI._put(value, eci);
    	for (var i:int = 0; i < encodingNames.length; i++) { NAME_TO_ECI._put(encodingNames[i], eci);}
    }
  }

  /**
   * @param value character set ECI value
   * @return {@link CharacterSetECI} representing ECI of given value, or null if it is legal but
   *   unsupported
   * @throws IllegalArgumentException if ECI value is invalid
   */
  public static function getCharacterSetECIByValue(value:int):CharacterSetECI {
    if (VALUE_TO_ECI == null) 
    {
      initialize();
    }
    if (value < 0 || value >= 900) {
      throw new IllegalArgumentException("COMMON : CharacterSetECI : getCharacterSetECIByValue : Bad ECI value: " + value);
    }
    return  VALUE_TO_ECI.getValueByKey(value) as CharacterSetECI;
  }

  /**
   * @param name character set ECI encoding name
   * @return {@link CharacterSetECI} representing ECI for character encoding, or null if it is legal
   *   but unsupported
   */
  public static function getCharacterSetECIByName(name:String ):CharacterSetECI {
    if (NAME_TO_ECI == null) {
      initialize();
    }
    return  (NAME_TO_ECI.getValueByKey(name) as CharacterSetECI);
  }

}
}