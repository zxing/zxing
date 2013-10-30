package com.google.zxing.client.android.result.supplement;

import android.content.SharedPreferences;
import android.widget.TextView;

import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.result.GS1ParsedResult;
import com.google.zxing.client.result.GS1ParsedResult.GS1ListEntry;
import com.google.zxing.client.result.gs1.GS1ApplicationIdentifier;

public class GS1DisplayDeletgate {

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
	
	
	
	public static void fillResult(TextView textView, GS1ParsedResult parsedResult, GS1Representation representation) {
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
