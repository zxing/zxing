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
 * Formats a numeric value containing a GS1 checkdigit being succeeded by a
 * serial number.
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class NumericWithCheckDigitPlusSerialFormatter extends
		NumericWithCheckDigitFormatter {

	private final int serialMaxLength;

	public NumericWithCheckDigitPlusSerialFormatter(int numericLength,
			int serialMaxLength) {
		super(numericLength);
		this.serialMaxLength = serialMaxLength;
	}

	@Override
	public String format(String value) {
		return super.format(value.substring(0, getMaxLength())) + " "
				+ value.substring(getMaxLength(), value.length());
	}

	@Override
	public boolean matches(String value) {
		if (value.length() < getMaxLength()) {
			return false;
		}
		if (!super.matches(value.substring(0, getMaxLength()))) {
			return false;
		}
		if (value.length() > getMaxLength() + serialMaxLength) {
			return false;
		}
		return true;
	}

}
