/*
 * Copyright (C) 2010 ZXing authors
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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.zxing.oned.rss.expanded.decoders;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 */
public class AI01_3X0X_1X_DecoderTest extends AbstractDecoderTest {
	
	private static final String header_310x_11 = "..XXX...";
	private static final String header_320x_11 = "..XXX..X";
	private static final String header_310x_13 = "..XXX.X.";
	private static final String header_320x_13 = "..XXX.XX";
	private static final String header_310x_15 = "..XXXX..";
	private static final String header_320x_15 = "..XXXX.X";
	private static final String header_310x_17 = "..XXXXX.";
	private static final String header_320x_17 = "..XXXXXX";
	
	public void test01_310X_1X_endDate() throws Exception {
		String data = header_310x_11 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_End;
		String expected = "(01)90012345678908(3100)001750";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_310X_11_1() throws Exception {
		String data = header_310x_11 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3100)001750(11)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_320X_11_1() throws Exception {
		String data = header_320x_11 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3200)001750(11)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_310X_13_1() throws Exception {
		String data = header_310x_13 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3100)001750(13)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_320X_13_1() throws Exception {
		String data = header_320x_13 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3200)001750(13)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_310X_15_1() throws Exception {
		String data = header_310x_15 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3100)001750(15)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_320X_15_1() throws Exception {
		String data = header_320x_15 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3200)001750(15)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_310X_17_1() throws Exception {
		String data = header_310x_17 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3100)001750(17)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
	public void test01_320X_17_1() throws Exception {
		String data = header_320x_17 + compressedGtin_900123456798908 + compressed20bitWeight_1750 + compressedDate_March_12th_2010;
		String expected = "(01)90012345678908(3200)001750(17)100312";
		
		assertCorrectBinaryString(data, expected);
	}
	
}
