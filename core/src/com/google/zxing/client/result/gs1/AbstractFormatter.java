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
 * Abstract base class that implements {@link GS1Formatter}. These formatters
 * are used to verify that a certain piece of the barcode content fulfills the
 * requirements of the application identifier (ai) and to format the value.
 * </p>
 * 
 * 
 * @author Melchior Rabe
 * 
 */
public abstract class AbstractFormatter implements GS1Formatter {

	private static final char DEFAULT_CHECKDIGIT_SEPARATOR = ' ';
	private static final String DEFAULT_MALFORMED_VALUE_MARKER = "! ";
	private static final char DEFAULT_ITEM_SEPARATOR = ' ';
	private final int maxLength;
	private char checkDigitSeparator;
	private char itemSeparator;
	private String malformedValueMarker;

	public AbstractFormatter(int maxLength) {
		super();
		this.maxLength = maxLength;
		this.checkDigitSeparator = DEFAULT_CHECKDIGIT_SEPARATOR;
		this.malformedValueMarker = DEFAULT_MALFORMED_VALUE_MARKER;
		this.itemSeparator = DEFAULT_ITEM_SEPARATOR;
	}

	@Override
	public int getMaxLength() {
		return maxLength;
	}

	@Override
	public boolean matches(String value) {
		return value != null && value.length() <= maxLength;
	}
	
	public void setCheckDigitSeparator(char checkDigitSeparator) {
		this.checkDigitSeparator = checkDigitSeparator;
	}

	@Override
	public char getCheckDigitSeparator() {
		return checkDigitSeparator;
	}

	@Override
	public char getItemSeparator() {
		return itemSeparator;
	}

	@Override
	public String getMalformedValueMarker() {
		return malformedValueMarker;
	}
	
}