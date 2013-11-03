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
 * Interface that describves the api of a formatter to verify and format a GS
 * application identifier value.
 * </p>
 * 
 * 
 * @author Melchior Rabe
 * 
 */
interface GS1Formatter {
	/**
	 * Formats the given string according to the rules of the formatter
	 * 
	 * @param value
	 *            The string to be formatted
	 * @return The formatted string
	 */
	String format(String value);

	/**
	 * 
	 * @return The maximum length that is allowed for the value to be formatted.
	 */
	int getMaxLength();

	/**
	 * Checks if the value passed fulfills the formatting pattern.
	 * 
	 * @param value
	 *            The string to be checked
	 * @return true if the value matches, false otherwise
	 */
	boolean matches(String value);
	
	/**
	 * Returns the character to separate a checkdigit from the rest of the value 
	 * @return The separator
	 */
	char getCheckDigitSeparator();
	
	/**
	 * Returns the character to separate different parts of a value (e.g. iso country code and amount in ai 391n)
	 * @return The separator
	 */
	char getItemSeparator();

	/**
	 * Returns the string that indicates a malformed value.
	 * @return The marker
	 */
	String getMalformedValueMarker();

}