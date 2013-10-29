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
 * Formats a date in format yyMMdd. dd can also be 00. Then the day is omitted
 * in the formatted string.
 * </p>
 * 
 * 
 * @author Melchior Rabe
 * 
 */
public class DateFormatter extends NumericFormatter {

	protected DateFormatter() {
		super(6, true);
	}

	@Override
	public String format(String value) {
		if (matches(value)) {
			String formattedDate = value.substring(0, 2);
			formattedDate += "-";
			formattedDate += value.substring(2, 4);
			if (!value.endsWith("00")) {
				formattedDate += "-";
				formattedDate += value.substring(4, 6);
			}
			return formattedDate;
		} else {
			return value;
		}
	}

	@Override
	public boolean matches(String value) {
		if (super.matches(value)) {
			if (value.endsWith("00")) {
				value = value.substring(0, 4) + "01";

			}

			SimpleDateFormat df = new SimpleDateFormat("yyMMdd");
			try {
				Date d = df.parse(value);
				if (!df.format(d).equals(value)) {
					return false;
				}
			} catch (ParseException e) {
				return false;
			}

			return true;
		} else {
			return false;
		}
	}

}