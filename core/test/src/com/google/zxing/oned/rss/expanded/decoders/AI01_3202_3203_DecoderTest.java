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
public class AI01_3202_3203_DecoderTest extends AbstractDecoderTest {
	private static final String header = "..X.X";
	
	public void test01_3202_1() throws Exception {
		String data = header + compressedGtin_900123456798908 + compressed15bitWeight_1750;
		String expected = "(01)90012345678908(3202)001750";
		
		assertCorrectBinaryString(data, expected);
	}

	public void test01_3203_1() throws Exception {
		String data = header + compressedGtin_900123456798908 + compressed15bitWeight_11750;
		String expected = "(01)90012345678908(3203)001750";
		
		assertCorrectBinaryString(data, expected);
	}
}
