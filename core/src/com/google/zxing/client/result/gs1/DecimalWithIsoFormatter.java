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
 * Formats a decimal number being preceded by a three digit ISO country code.
 * See {@link DecimalFormatter} and {@link IsoCountryFormatter}
 * </p>
 * 
 * <p>
 * E.g. "2345" is formatted to "3.45"
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class DecimalWithIsoFormatter extends AbstractFormatter {

	private static final IsoCountryFormatter ISO_FORMATTER = new IsoCountryFormatter(
			1);
	private final DecimalFormatter DECIMAL_FORMATTER;

	public DecimalWithIsoFormatter(int maxLength, boolean fixedLength) {
		super(maxLength + 4);
		DECIMAL_FORMATTER = new DecimalFormatter(maxLength, fixedLength);
	}

	public DecimalWithIsoFormatter(int maxLength) {
		this(maxLength, true);
	}

	@Override
	public String format(String value) {
		return ISO_FORMATTER.format(getIsoCode(value)) + " "
				+ DECIMAL_FORMATTER.format(getDecimalValue(value));
	}

	@Override
	public boolean matches(String value) {
		if(value.length()<4){
			return false;
		}
		String decimalString = getDecimalValue(value);
		return DECIMAL_FORMATTER.matches(decimalString)
				&& ISO_FORMATTER.matches(getIsoCode(value));
	}

	private String getDecimalValue(String value) {
		return value.substring(0, 1) + value.substring(4);
	}

	private String getIsoCode(String value) {
		return value.substring(1, 4);
	}

}
