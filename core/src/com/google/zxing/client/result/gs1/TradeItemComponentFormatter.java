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

package com.google.zxing.client.result.gs1;

/**
 * <p>
 * Special formatter for ai 8006
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class TradeItemComponentFormatter extends NumericFormatter {

	private static final NumericWithCheckDigitFormatter GTIN_FORMATTER = new NumericWithCheckDigitFormatter(
			14);

	public TradeItemComponentFormatter() {
		super(4);
	}

	@Override
	public String format(String value) {
		if(!matches(value)){
			return value;
		}
		return GTIN_FORMATTER.format(getGTIN(value)) + getItemSeparator() + getRelativeNumber(value) + getItemSeparator()
				+ getTotalNumber(value);
	}

	@Override
	public boolean matches(String value) {
		if(value==null || value.length() !=18){
			return false;
		}
		if (!super.matches(value.substring(14, 18))) {
			return false;
		}
		if (!GTIN_FORMATTER.matches(getGTIN(value))) {
			return false;
		}
		return true;
	}

	private String getGTIN(String value) {
		return value.substring(0, 14);
	}

	private String getRelativeNumber(String value) {
		return value.substring(14, 16);
	}

	private String getTotalNumber(String value) {
		return value.substring(16, 18);
	}
}
