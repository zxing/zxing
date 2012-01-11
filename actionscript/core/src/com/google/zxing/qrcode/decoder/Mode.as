/*
 * Copyright 2007 ZXing authors
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
package com.google.zxing.qrcode.decoder
{
/**
 * <p>See ISO 18004:2006, 6.4.1, Tables 2 and 3. This enum encapsulates the various modes in which
 * data can be encoded to bits in the QR code standard.</p>
 *
 * @author Sean Owen
 */

	public class Mode
	{

	      import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	      
﻿          // No, we can't use an enum here. J2ME doesn't support it.
          public static var TERMINATOR:Mode = new Mode([0, 0, 0], 0x00, "TERMINATOR"); // Not really a mode...
          public static var NUMERIC:Mode = new Mode([10, 12, 14], 0x01, "NUMERIC");
          public static var ALPHANUMERIC:Mode = new Mode([9, 11, 13], 0x02, "ALPHANUMERIC");
          public static var STRUCTURED_APPEND:Mode  = new Mode([0, 0, 0], 0x03, "STRUCTURED_APPEND"); // Not supported
          public static var BYTE:Mode = new Mode([8, 16, 16], 0x04, "BYTE");
          public static var ECI:Mode = new Mode(null, 0x07, "ECI"); // character counts don't apply
          public static var KANJI:Mode = new Mode([8, 10, 12], 0x08, "KANJI");
          public static var FNC1_FIRST_POSITION:Mode = new Mode(null, 0x05, "FNC1_FIRST_POSITION");
          public static var FNC1_SECOND_POSITION:Mode = new Mode(null, 0x09, "FNC1_SECOND_POSITION");
            /** See GBT 18284-2000; "Hanzi" is a transliteration of this mode name. */
		  public static var HANZI:Mode = new Mode([8, 10, 12], 0x0D, "HANZI");

          protected var characterCountBitsForVersions:Array;
          protected var bits:int;
          protected var name:String;

          public function Mode(characterCountBitsForVersions:Array, bits:int, name:String) 
          {
            this.characterCountBitsForVersions = characterCountBitsForVersions;
            this.bits = bits;
            this.name = name;
          }

          /**
           * @param bits four bits encoding a QR Code data mode
           * @return {@link Mode} encoded by these bits
           * @throws ArgumentException if bits do not correspond to a known mode
           */
          public static  function forBits(bits:int):Mode 
          {
          	
            switch (bits) {
              case 0x0:
                return TERMINATOR;
              case 0x1:
                return NUMERIC;
              case 0x2:
                return ALPHANUMERIC;
              case 0x4:
                return BYTE;
              case 0x5:
                return FNC1_FIRST_POSITION;
              case 0x7:
                return ECI;
              case 0x8:
                return KANJI;
              case 0x9:
                return FNC1_SECOND_POSITION;
 		      case 0xD:
		          // 0xD is defined in GBT 18284-2000, may not be supported in foreign country
		        return HANZI;
              default:
                throw new IllegalArgumentException("Mode : forBits : bits does not match any format : "+bits);
            }
          }

          /**
           * @param version version in question
           * @return number of bits used, in this QR Code symbol {@link Version}, to encode the
           *         count of characters that will follow encoded in this {@link Mode}
           */
          public function  getCharacterCountBits(version:Version):int 
          {
            if (characterCountBitsForVersions == null) 
            {
              throw new IllegalArgumentException("Character count doesn't apply to this mode");
            }
            var number:int = version.getVersionNumber();
            var offset:int;
            if (number <= 9)
            {
              offset = 0;
            } 
            else if (number <= 26) 
            {
              offset = 1;
            } 
            else 
            {
              offset = 2;
            }
            return characterCountBitsForVersions[offset];
          }

          public function getBits():int 
          {
            return bits;
          }

          public function getName():String 
          {
            return name;
          }

          public function toString():String 
          {
            return name;
          }

          public function getCharacterCountBitsForVersions():Array 
          {
            return characterCountBitsForVersions;
          }
          
          public function Equals(other:Mode):Boolean
          {
    	      if (this.name != other.getName()) { return false; }
    	      if (this.bits != other.getBits()) { return false; }
    	      var t:Array = other.getCharacterCountBitsForVersions()
    	      if (this.characterCountBitsForVersions.length != t.length) { return false; }
    	      for (var i:int=0;i<t.length;i++)
    	      {
    	      	if (t[i] != this.characterCountBitsForVersions[i]) { return false;}
    	      }
    	      return true;
          }
    }
}