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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>
 * Formats a date in format yyMMddhhmm(ss)?. The seconds are optional.
 * </p>
 * 
 * 
 * @author Melchior Rabe
 * 
 */
public class DateTimeFormatter extends NumericFormatter {
	private final boolean hasSeconds;

	/**
	 * Default ctor
	 * 
	 * @param hasSeconds
	 *            pass true if the encoded value contains two digits for the
	 *            seconds, false otherwise.
	 */
	public DateTimeFormatter(boolean hasSeconds) {
		super(hasSeconds ? 12 : 10, true);
		this.hasSeconds = hasSeconds;
	}

	@Override
	public String format(String value) {
		if (!matches(value)) {
			return value;
		}

		SimpleDateFormat inFormat;
		SimpleDateFormat outFormat;

		if (hasSeconds) {
			inFormat = new SimpleDateFormat("yyMMddHHmmss");
			outFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
		} else {
			inFormat = new SimpleDateFormat("yyMMddHHmm");
			outFormat = new SimpleDateFormat("yy-MM-dd HH:mm");
		}

		try {
			return outFormat.format(inFormat.parse(value));
		} catch (ParseException e) {
			return value;
		}
	}

	@Override
	public boolean matches(String value) {
		if (!super.matches(value)) {
			return false;
		}

		SimpleDateFormat df;
		if (hasSeconds) {
			df = new SimpleDateFormat("yyMMddHHmmss");
		} else {
			df = new SimpleDateFormat("yyMMddHHmm");
		}
		try {
			Date d = df.parse(value);
			String formatted = df.format(d);
			if (!formatted.equals(value)) {
				return false;
			}
		} catch (ParseException e) {
			return false;
		}
		return true;

	}

}
