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
 * Formats an three digit ISO 3166 country code or a list of such codes.
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */
public class IsoCountryFormatter extends NumericFormatter {
	private final int numberOfCodes;

	/**
	 * ctor.
	 * 
	 * @param numberOfCodes
	 *            The number of three digit codes.
	 */
	public IsoCountryFormatter(int numberOfCodes) {
		super(3 * numberOfCodes, true);
		this.numberOfCodes = numberOfCodes;
	}

	@Override
	public String format(String value) {
		if (!matches(value)) {
			return value;
		}

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < numberOfCodes; i++) {
			if (i > 0) {
				sb.append(" ");
			}
			sb.append(value.substring(i*3, (i + 1) * 3));
		}
		return sb.toString();
	}

}
