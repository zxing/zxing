/*
 * Copyright 2013 Melchior Rabe
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
package com.google.zxing.client.android.result.supplement;

import android.content.SharedPreferences;
import android.widget.TextView;

import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.result.GS1ParsedResult;
import com.google.zxing.client.result.GS1ParsedResult.GS1ListEntry;
import com.google.zxing.client.result.gs1.GS1ApplicationIdentifier;
import com.google.zxing.client.result.gs1.IsoFormatter.CountryDecoding;
import com.google.zxing.client.result.gs1.IsoFormatter.CurrencyDecoding;

/**
 * Handles the generation of the text displayed.
 * 
 * @author Melchior Rabe
 * 
 */
public class GS1DisplayDeletgate {

	/**
	 * Represents the options for the GS1 display in the preferences
	 * @author Melchior Rabe
	 *
	 */
	public enum GS1Representation {

		  ValueOnly,
		  TextOnly,
		  ValueAndText;

		  private static GS1Representation parse(String modeString) {
		    return modeString == null || modeString.isEmpty() || "-".equals(modeString) ? ValueOnly : valueOf(modeString);
		  }

		  public static GS1Representation readPref(SharedPreferences sharedPrefs) {
		    return parse(sharedPrefs.getString(PreferencesActivity.KEY_GS1_AI_REPRESENTATION, null));
		  }
	}
	
	
	/**
	 * Fills the result into the textview
	 * @param textView The textview to display the formatted result
	 * @param parsedResult The result to be formatted
	 * @param representation The representation mode
	 * @param countryDecoding The type of country decoding 
	 * @param currencyDecoding The type of currency decoding
	 */
	public static void fillResult(TextView textView, GS1ParsedResult parsedResult, GS1Representation representation, CountryDecoding countryDecoding, CurrencyDecoding currencyDecoding) {

		GS1ApplicationIdentifier.setCountryDecoding(countryDecoding);
		GS1ApplicationIdentifier.setCurrencyDecoding(currencyDecoding);
		
		StringBuffer sb = new StringBuffer();
		
		for(GS1ListEntry e : parsedResult.getAiList()){
			if(!e.getId().matches(e.getValue())){
				sb.append("!");
			}
			sb.append(getAi(e, representation));
			sb.append(": ");
			sb.append(e.getId().formatValue(e.getValue()));
			sb.append("\n");
		}
		
		textView.setText(sb);
	}

	private static Object getAi(GS1ListEntry entry,
			GS1Representation representation) {
		GS1ApplicationIdentifier id = entry.getId();
		String value = entry.getValue();
		switch(representation){
		case TextOnly:
			return id.getDataTitle();
		case ValueAndText:
			return "(" + id.getIndexedIdentifier(value) + ") " + id.getDataTitle();
		case ValueOnly:
		default:
			return "(" + id.getIndexedIdentifier(value) + ") ";
		}
	}
}
