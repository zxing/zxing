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
 * Formatter for alphanumeric values being preceded by a three digit ISO country
 * code (ISO 3166).
 * </p>
 * 
 * 
 * @author Melchior Rabe
 * 
 */
public class AlphaNumericWithIsoFormatter extends AlphaNumericFormatter
		implements IsoFormatter {

	private static final AbstractIsoFormatter ISO_FORMATTER = new IsoCountryFormatter(
			1);

	/**
	 * Creates a new formatter for a given length (not including the three
	 * digits for the ISO code).
	 * 
	 * @param maxLength
	 *            length of the value without three digit ISO code.
	 */
	public AlphaNumericWithIsoFormatter(int maxLength) {
		super(maxLength);
	}

	@Override
	public String format(String value) {
		if (matches(value)) {
			return ISO_FORMATTER.format(getIsoCode(value)) + getItemSeparator()
					+ super.format(getAlphaValue(value));
		}
		return value;
	}

	@Override
	public boolean matches(String value) {
		if (value == null || value.length() < 3
				|| value.length() > getMaxLength() + 3) {
			return false;
		}
		String alphaString = getAlphaValue(value);
		String isoCode = getIsoCode(value);
		return super.matches(alphaString) && ISO_FORMATTER.matches(isoCode);
	}

	private String getAlphaValue(String value) {
		return value.substring(3);
	}

	private String getIsoCode(String value) {
		return value.substring(0, 3);
	}

	@Override
	public void setCountryDecoding(CountryDecoding countryDecoding) {
		ISO_FORMATTER.setCountryDecoding(countryDecoding);
	}

	@Override
	public void setCurrencyDecoding(CurrencyDecoding currencyDecoding) {
		ISO_FORMATTER.setCurrencyDecoding(currencyDecoding);
	}
}
