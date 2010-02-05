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
namespace com.google.zxing.common
{
	
	/// <summary> Encapsulates a Character Set ECI, according to "Extended Channel Interpretations" 5.3.1.1
	/// of ISO 18004.
	/// 
	/// </summary>
	/// <author>  Sean Owen
	/// </author>
	/// <author>www.Redivivus.in (suraj.supekar@redivivus.in) - Ported from ZXING Java Source 
	/// </author>
	public sealed class CharacterSetECI:ECI
	{
		public System.String EncodingName
		{
			get
			{
				return encodingName;
			}
			
		}
		
		private static System.Collections.Hashtable VALUE_TO_ECI;
		private static System.Collections.Hashtable NAME_TO_ECI;
		
		private static void  initialize()
		{
			VALUE_TO_ECI = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable(29));
			NAME_TO_ECI = System.Collections.Hashtable.Synchronized(new System.Collections.Hashtable(29));
			// TODO figure out if these values are even right!
			addCharacterSet(0, "Cp437");
			addCharacterSet(1, new System.String[]{"ISO8859_1", "ISO-8859-1"});
			addCharacterSet(2, "Cp437");
			addCharacterSet(3, new System.String[]{"ISO8859_1", "ISO-8859-1"});
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
			addCharacterSet(20, new System.String[]{"SJIS", "Shift_JIS"});
		}
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'encodingName '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.String encodingName;
		
		private CharacterSetECI(int value_Renamed, System.String encodingName):base(value_Renamed)
		{
			this.encodingName = encodingName;
		}
		
		private static void  addCharacterSet(int value_Renamed, System.String encodingName)
		{
			CharacterSetECI eci = new CharacterSetECI(value_Renamed, encodingName);
			VALUE_TO_ECI[(System.Int32) value_Renamed] = eci; // can't use valueOf
			NAME_TO_ECI[encodingName] = eci;
		}
		
		private static void  addCharacterSet(int value_Renamed, System.String[] encodingNames)
		{
			CharacterSetECI eci = new CharacterSetECI(value_Renamed, encodingNames[0]);
			VALUE_TO_ECI[(System.Int32) value_Renamed] = eci; // can't use valueOf
			for (int i = 0; i < encodingNames.Length; i++)
			{
				NAME_TO_ECI[encodingNames[i]] = eci;
			}
		}
		
		/// <param name="value">character set ECI value
		/// </param>
		/// <returns> {@link CharacterSetECI} representing ECI of given value, or null if it is legal but
		/// unsupported
		/// </returns>
		/// <throws>  IllegalArgumentException if ECI value is invalid </throws>
		public static CharacterSetECI getCharacterSetECIByValue(int value_Renamed)
		{
			if (VALUE_TO_ECI == null)
			{
				initialize();
			}
			if (value_Renamed < 0 || value_Renamed >= 900)
			{
				throw new System.ArgumentException("Bad ECI value: " + value_Renamed);
			}
			return (CharacterSetECI) VALUE_TO_ECI[(System.Int32) value_Renamed];
		}
		
		/// <param name="name">character set ECI encoding name
		/// </param>
		/// <returns> {@link CharacterSetECI} representing ECI for character encoding, or null if it is legal
		/// but unsupported
		/// </returns>
		public static CharacterSetECI getCharacterSetECIByName(System.String name)
		{
			if (NAME_TO_ECI == null)
			{
				initialize();
			}
			return (CharacterSetECI) NAME_TO_ECI[name];
		}
	}
}