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
 * Special formatter for ai 703
 * </p>
 * 
 * @author Melchior Rabe
 * 
 */

public class ProcessorFormatter extends AlphaNumericFormatter implements IndexedAI {
	private static final AbstractIsoFormatter ISO_FORMATTER = new IsoCountryFormatter(
			1);

	public ProcessorFormatter() {
		super(27);

	}

	@Override
	public String format(String value) {
		return ISO_FORMATTER.format(getIsoCode(value))
				+ getItemSeparator() + getApprovalNumber(value);
	}

	@Override
	public boolean matches(String value) {
		if (value.length() < 4) {
			return false;
		}
		if (!super.matches(getApprovalNumber(value))) {
			return false;
		}
		if (!getIndex(value).matches("^\\d$")) {
			return false;
		}
		if (!ISO_FORMATTER.matches(getIsoCode(value))) {
			return false;
		}
		return true;
	}

	@Override
	public String getIndex(String value) {
		return value.substring(0, 1);
	}

	private String getIsoCode(String value) {
		return value.substring(1, 4);
	}

	private String getApprovalNumber(String value) {
		return value.substring(4);
	}

}
