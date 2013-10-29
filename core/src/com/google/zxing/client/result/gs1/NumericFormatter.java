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
 * Formats a numeric value.
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class NumericFormatter extends AbstractFormatter {

	protected final boolean fixLength;

	public NumericFormatter(int maxLength, boolean fixLength) {
		super(maxLength);
		this.fixLength = fixLength;
	}

	public NumericFormatter(int maxLength) {
		this(maxLength, true);
	}

	@Override
	public String format(String value) {
		return value;
	}

	@Override
	public boolean matches(String value) {
		if ((fixLength && value.length() != getMaxLength())
				|| value.length() > getMaxLength()) {
			return false;
		}
		if (!value.matches("^\\d+$")) {
			return false;
		}

		return true;
	}

}