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

import java.util.regex.Pattern;

/**
 * <p>
 * Formatter for alphanumeric values. Allowed values are according to
 * "GS1 AI Encodable Character Set 82".
 * </p>
 * 
 * 
 * @author Melchior Rabe
 * 
 */
public class AlphaNumericFormatter extends AbstractFormatter {

	private static final Pattern VALID_CHARS = Pattern
			.compile("^[!\"%&'()*+,\\-./0-9:;<=>?A-Z_a-z]*$");

	/**
	 * Creates a new formatter with the given maximum length.
	 * 
	 * @param maxLength
	 *            The maximum length of the value.
	 */
	public AlphaNumericFormatter(int maxLength) {
		super(maxLength);
	}

	@Override
	public String format(String value) {
		return value;
	}

	@Override
	public boolean matches(String value) {
		return super.matches(value) && VALID_CHARS.matcher(value).matches();
	}

}
