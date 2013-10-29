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
 * Special formatter for ai 8001
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class RollProductFormatter extends NumericFormatter {

	public RollProductFormatter() {
		super(14);
	}

	@Override
	public boolean matches(String value) {
		if (!super.matches(value)) {
			return false;
		}
		if (!getWindingDirection(value).matches("^[019]$")) {
			return false;
		}
		return true;
	}

	@Override
	public String format(String value) {

		return getSlitWidth(value) + " " + getLength(value) + " "
				+ getCoreDiameter(value) + " " + getWindingDirection(value)
				+ " " + getNumberOfSplices(value);
	}

	private String getSlitWidth(String value) {
		return value.substring(0, 4);
	}

	private String getLength(String value) {
		return value.substring(4, 9);
	}

	private String getCoreDiameter(String value) {
		return value.substring(9, 12);
	}

	private String getWindingDirection(String value) {
		return value.substring(12, 13);
	}

	private String getNumberOfSplices(String value) {
		return value.substring(13, 14);
	}

}
