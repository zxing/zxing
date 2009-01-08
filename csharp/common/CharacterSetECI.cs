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
namespace com.google.zxing.common
{
    using System;
    using System.Text;
    using System.Collections;

    /// <summary> A class which wraps a 2D array of bytes. The default usage is signed. If you want to use it as a
    /// unsigned container, it's up to you to do byteValue & 0xff at each location.
    /// *
    /// JAVAPORT: I'm not happy about the argument ordering throughout the file, as I always like to have
    /// the horizontal component first, but this is for compatibility with the C++ code. The original
    /// code was a 2D array of ints, but since it only ever gets assigned -1, 0, and 1, I'm going to use
    /// less memory and go with bytes.
    /// *
    /// </summary>
    /// <author>  dswitkin@google.com (Daniel Switkin)
    /// 
    /// </author>
    public sealed class CharacterSetECI : ECI
    { 
        private static Hashtable VALUE_TO_ECI=new Hashtable(29);
        /*static VALUE_TO_ECI = new Hashtable(29){
        // TODO figure out if these values are even right!
        addCharacterSet(0, "Cp437");
        addCharacterSet(1, "ISO8859_1");
        addCharacterSet(2, "Cp437");
        addCharacterSet(3, "ISO8859_1");
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
        addCharacterSet(20, "SJIS");
      }*/

      private String encodingName;
     
      private CharacterSetECI(int value, String encodingName):base(value) {
        addCharacterSet(0, "Cp437");
        addCharacterSet(1, "ISO8859_1");
        addCharacterSet(2, "Cp437");
        addCharacterSet(3, "ISO8859_1");
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
        addCharacterSet(20, "SJIS");
        this.encodingName = encodingName;
      }

      public String getEncodingName() {
        return encodingName;
      }

      private static void addCharacterSet(int value, String encodingName) {
        VALUE_TO_ECI.Add(value, new CharacterSetECI(value, encodingName));
      }

      public static CharacterSetECI getCharacterSetECIByValue(int value) {          
        CharacterSetECI eci = (CharacterSetECI) VALUE_TO_ECI[value];
        if (eci == null) {
          throw new Exception("Unsupported value: " + value);
        }
        return eci;
      }
    }
}