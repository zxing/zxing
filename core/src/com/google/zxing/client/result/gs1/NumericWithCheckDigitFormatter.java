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
 * Formats a numeric value containing a GS1 checkdigit at the last place (e.g. a
 * GTIN).
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class NumericWithCheckDigitFormatter extends NumericFormatter {

	public NumericWithCheckDigitFormatter(int length) {
		super(length);
	}

	@Override
	public String format(String value) {
		return value.substring(0, value.length() - 1) + " "
				+ value.substring(value.length() - 1);
	}

	@Override
	public boolean matches(String value) {
		return super.matches(value) && correctCheckDigit(value);
	}

	private boolean correctCheckDigit(String value) {

		int checkDigit = 0;
		int factor = 3;
		for (int index = value.length() - 2; index >= 0; index--) {
			checkDigit += Integer.parseInt(value.substring(index, index + 1),
					10) * factor;
			factor = 4 - factor;
		}
		checkDigit %= 10;
		if (checkDigit != 0) {
			checkDigit = 10 - checkDigit;
		}

		return Integer.parseInt(value.substring(value.length() - 1), 10) == checkDigit;
	}

}
