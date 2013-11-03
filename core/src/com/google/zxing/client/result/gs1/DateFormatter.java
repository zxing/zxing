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
import java.util.Calendar;
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

	private SimpleDateFormat dateFormatWithDay = new SimpleDateFormat(
			"yyyy-MM-dd");
	private SimpleDateFormat dateFormatWithoutDay = new SimpleDateFormat(
			"yyyy-MM");

	protected DateFormatter() {
		super(6, true);
	}

	@Override
	public String format(String value) {
		if (matches(value)) {
			SimpleDateFormat df;

			if (hasDay(value)) {
				df = dateFormatWithDay;
			} else {
				df = dateFormatWithoutDay;
			}
			
			Date d = getDate(value);

			return df.format(d);
		} else {
			return value;
		}
	}

	/**
	 * converts the value given into a date object
	 * @param value
	 * @return
	 */
	private Date getDate(String value) {
		int year = Integer.parseInt(value.substring(0, 2), 10);
		int month = Integer.parseInt(value.substring(2, 4), 10);
		int day = Integer.parseInt(value.substring(4, 6), 10);
		
		// tweak the values
		int currentYear = calculateYear(year);
		// seat day to 1 as zero causes trouble!
		if(day == 0){
			day = 1;
		}
		Calendar c = Calendar.getInstance();
		c.set(currentYear, month-1, day);
		return c.getTime();
	}

	protected int calculateYear(int twoDigitYear) {
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		int currentYearInCentury = currentYear % 100;
		// 
		int normalizedYear = (twoDigitYear - currentYearInCentury + 100) % 100;
		
		// year is in the future
		if(normalizedYear <= 50){
			twoDigitYear = currentYear + normalizedYear;
		}else{
			twoDigitYear = currentYear - (100 - normalizedYear);
		}
		return twoDigitYear;
	}

	/**
	 * Returns if a value for a day is encode or not. Use only with valid values
	 * 
	 * @param value The date string
	 * @return
	 */
	private boolean hasDay(String value) {
		return !value.endsWith("00");
	}

	@Override
	public boolean matches(String value) {
		if (super.matches(value)) {
			if (!hasDay(value)) {
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