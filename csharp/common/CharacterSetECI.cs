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

using System;
using System.Collections.Generic;

namespace com.google.zxing.common
{

    using FormatException = com.google.zxing.FormatException;


    /// <summary>
    /// Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1
    /// of ISO 18004.
    /// 
    /// @author Sean Owen
    /// </summary>
    public abstract class CharacterSetECI
    {
        private CharacterSetECI()
        {

        }

        private static object _syncLock;

        static CharacterSetECI()
        {
            lock (_syncLock)
            {
                addSet((innerCharacterSetECI)Cp437);
                addSet((innerCharacterSetECI)ISO8859_1);
                addSet((innerCharacterSetECI)ISO8859_2);
                addSet((innerCharacterSetECI)ISO8859_3);
                addSet((innerCharacterSetECI)ISO8859_4);
                addSet((innerCharacterSetECI)ISO8859_5);
                addSet((innerCharacterSetECI)ISO8859_6);
                addSet((innerCharacterSetECI)ISO8859_7);
                addSet((innerCharacterSetECI)ISO8859_8);
                addSet((innerCharacterSetECI)ISO8859_9);
                addSet((innerCharacterSetECI)ISO8859_10);
                addSet((innerCharacterSetECI)ISO8859_11);
                addSet((innerCharacterSetECI)ISO8859_13);
                addSet((innerCharacterSetECI)ISO8859_14);
                addSet((innerCharacterSetECI)ISO8859_15);
                addSet((innerCharacterSetECI)ISO8859_16);
                addSet((innerCharacterSetECI)SJIS);
                addSet((innerCharacterSetECI)Cp1250);
                addSet((innerCharacterSetECI)Cp1251);
                addSet((innerCharacterSetECI)Cp1252);
                addSet((innerCharacterSetECI)Cp1256);
                addSet((innerCharacterSetECI)UnicodeBigUnmarked);
                addSet((innerCharacterSetECI)UTF8);
                addSet((innerCharacterSetECI)ASCII);
                addSet((innerCharacterSetECI)Big5);
                addSet((innerCharacterSetECI)GB18030);
                addSet((innerCharacterSetECI)EUC_KR);
            }
        }

        private static void addSet(innerCharacterSetECI set)
        {
            foreach (int value in set.Values)
            {
                VALUE_TO_ECI[value] = set;
            }

            foreach (string name in set.OtherEncodingNames)
            {
                NAME_TO_ECI[name] = set;
            }
        }

        private class innerCharacterSetECI : CharacterSetECI
        {
            private int[] values;
            private string[] otherEncodingNames;



            //internal Thing(int value)
            //{
            //    setup(value);
            //}

            internal innerCharacterSetECI(int value, params string[] otherEncodingNames)
            {
                setup(value, otherEncodingNames);
            }

            internal innerCharacterSetECI(int[] values, params string[] otherEncodingNames)
            {
                setup(values, otherEncodingNames);
            }

            //void setup(int value)
            //{
            //    setup(new int[] { value });
            //}

            void setup(int value, params string[] otherEncodingNames)
            {
                this.values = new int[] { value };
                this.otherEncodingNames = otherEncodingNames;
            }

            void setup(int[] values, params string[] otherEncodingNames)
            {
                this.values = values;
                this.otherEncodingNames = otherEncodingNames;
            }

            public override int Value
            {
                get { return values[0]; }
            }

            public override string name()
            {
                return otherEncodingNames[0];
            }

            public int[] Values
            {
                get { return values; }
            }

            public string[] OtherEncodingNames
            {
                get { return otherEncodingNames; }
            }
        }

        public abstract string name();
       

        // Enum name is a Java encoding valid for java.lang and java.io
        public static readonly CharacterSetECI Cp437 = new innerCharacterSetECI(new int[] { 0, 2 },"Cp437","");
        public static readonly CharacterSetECI ISO8859_1 = new innerCharacterSetECI(new int[] { 1, 3 }, "ISO-8859-1","");

        public static readonly CharacterSetECI ISO8859_2 = new innerCharacterSetECI(4, "ISO-8859-2","ISO8859_2");
        public static readonly CharacterSetECI ISO8859_3 = new innerCharacterSetECI(5, "ISO-8859-3","ISO8859_3");
        public static readonly CharacterSetECI ISO8859_4 = new innerCharacterSetECI(6, "ISO-8859-4","ISO8859_4");
        public static readonly CharacterSetECI ISO8859_5 = new innerCharacterSetECI(7, "ISO-8859-5","ISO8859_5");
        public static readonly CharacterSetECI ISO8859_6 = new innerCharacterSetECI(8, "ISO-8859-6","ISO8859_6");
        public static readonly CharacterSetECI ISO8859_7 = new innerCharacterSetECI(9, "ISO-8859-7","ISO8859_7");
        public static readonly CharacterSetECI ISO8859_8 = new innerCharacterSetECI(10, "ISO-8859-8","ISO8859_8");
        public static readonly CharacterSetECI ISO8859_9 = new innerCharacterSetECI(11, "ISO-8859-9","ISO8859_9");
        public static readonly CharacterSetECI ISO8859_10 = new innerCharacterSetECI(12, "ISO-8859-10","ISO8859_10");
        public static readonly CharacterSetECI ISO8859_11 = new innerCharacterSetECI(13, "ISO-8859-11","ISO8859_11");
        public static readonly CharacterSetECI ISO8859_13 = new innerCharacterSetECI(15, "ISO-8859-13","ISO8859_13");
        public static readonly CharacterSetECI ISO8859_14 = new innerCharacterSetECI(16, "ISO-8859-14","ISO8859_14");
        public static readonly CharacterSetECI ISO8859_15 = new innerCharacterSetECI(17, "ISO-8859-15","ISO8859_15");
        public static readonly CharacterSetECI ISO8859_16 = new innerCharacterSetECI(18, "ISO-8859-16","ISO8859_16");
        public static readonly CharacterSetECI SJIS = new innerCharacterSetECI(20, "Shift_JIS","SJIS");
        public static readonly CharacterSetECI Cp1250 = new innerCharacterSetECI(21, "windows-1250","Cp1250");
        public static readonly CharacterSetECI Cp1251 = new innerCharacterSetECI(22, "windows-1251","Cp1251");
        public static readonly CharacterSetECI Cp1252 = new innerCharacterSetECI(23, "windows-1252","Cp1252");
        public static readonly CharacterSetECI Cp1256 = new innerCharacterSetECI(24, "windows-1256","Cp1256");
        public static readonly CharacterSetECI UnicodeBigUnmarked = new innerCharacterSetECI(25, "UTF-16BE", "UnicodeBig","UnicodeBigUnmarked");
        public static readonly CharacterSetECI UTF8 = new innerCharacterSetECI(26, "UTF-8","");
        public static readonly CharacterSetECI ASCII = new innerCharacterSetECI(new int[] { 27, 170 }, "US-ASCII","ASCII");
        public static readonly CharacterSetECI Big5 = new innerCharacterSetECI(28,"Big5","Big5");
        public static readonly CharacterSetECI GB18030 = new innerCharacterSetECI(29, "GB2312", "EUC_CN", "GBK","GB18030");
        public static readonly CharacterSetECI EUC_KR = new innerCharacterSetECI(30, "EUC-KR","EUC_KR");

        private static readonly Dictionary<int, CharacterSetECI> VALUE_TO_ECI = new Dictionary<int, CharacterSetECI>();
        private static readonly Dictionary<string, CharacterSetECI> NAME_TO_ECI = new Dictionary<string, CharacterSetECI>();

        /// <param name="value"> character set ECI value </param>
        /// <returns> CharacterSetECI representing ECI of given value, or null if it is legal but
        ///   unsupported </returns>
        /// <exception cref="IllegalArgumentException"> if ECI value is invalid </exception>
        public static CharacterSetECI getCharacterSetECIByValue(int value)
        {
            if (value < 0 || value >= 900)
            {
                throw FormatException.FormatInstance;
            }
            if (VALUE_TO_ECI.ContainsKey(value))
            {
                return VALUE_TO_ECI[value];
            }
            return null;
            
        }

        /// <param name="name"> character set ECI encoding name </param>
        /// <returns> CharacterSetECI representing ECI for character encoding, or null if it is legal
        ///   but unsupported </returns>
        public static CharacterSetECI getCharacterSetECIByName(string name)
        {
            if (NAME_TO_ECI.ContainsKey(name))
            {
                return NAME_TO_ECI[name];
            }
            return null;
        }

        public virtual int Value { get { throw new NotImplementedException(); } }
    }
    //public static partial class EnumExtensionMethods
    //{
    //    public static int getValue(this CharacterSetECI instanceJavaToDotNetTempPropertyGetValue)
    //    {
    //        return values[0];
    //    }
    //}

}