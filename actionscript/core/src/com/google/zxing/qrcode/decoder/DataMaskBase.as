/*
 * Copyright 2013 ZXing authors
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

package com.google.zxing.qrcode.decoder
{
		import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;

	public class DataMaskBase
	{
    	  /**
		   * <p>Implementations of this method reverse the data masking process applied to a QR Code and
		   * make its bits ready to read.</p>
		   *
		   * @param bits representation of QR Code bits
		   * @param dimension dimension of QR Code, represented by bits, being unmasked
		   */
		  public function unmaskBitMatrix(bits:BitMatrix , dimension:int):void {
		    for (var i:int = 0; i < dimension; i++) {
		      for (var j:int = 0; j < dimension; j++) {
		        if (isMasked(i, j)) {
		          bits.flip(j, i);
		        }
		      }
		    }
		  }
		  
		  public function isMasked(i:int, j:int):Boolean {return false;}

	}
}