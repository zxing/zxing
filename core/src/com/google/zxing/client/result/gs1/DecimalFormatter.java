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
 * Formats a decimal number. The first digit defines the place of the decimal
 * separator. It belongs according to the definition to the application
 * identifier, but for structural reasons it is "moved" to the value.
 * 
 * </p>
 * 
 * <p>
 * E.g. "2345" is formatted to "3.45"
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class DecimalFormatter extends NumericFormatter implements IndexedAI {

	/**
	 * Constructs a decimal formatter for a number with the given amount of
	 * digits (not including the indicator digit for the decimal separator).
	 * 
	 * @param length
	 */
	public DecimalFormatter(int length) {
		this(length + 1, true);
	}

	public DecimalFormatter(int maxLength, boolean fixedLength) {
		super(maxLength + 1, fixedLength);
	}

	@Override
	public String format(String value) {
		if (!this.matches(value)) {
			return value;
		}
		int decimalPlace = getDecimalPlace(value);

		String integerPart = value.substring(1, value.length() - decimalPlace);
		String fractionPart = value.substring(value.length() - decimalPlace,
				value.length());
		if (integerPart.isEmpty()) {
			integerPart = "0";
		}
		if (fractionPart.isEmpty()) {
			return integerPart;
		}
		return integerPart + "." + fractionPart;
	}

	@Override
	public boolean matches(String value) {
		if (!super.matches(value)) {
			return false;
		}

		int digits = value.length() - 1;
		int decimalPlace = getDecimalPlace(value);

		return decimalPlace >= 0 && decimalPlace <= digits;
	}

	private int getDecimalPlace(String value) {
		return Integer.parseInt(getIndex(value), 10);
	}

	@Override
	public String getIndex(String value) {
		return value.substring(0, 1);
	}

}
