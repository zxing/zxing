/*
 * Copyright 2007 ZXing authors
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
	import com.google.zxing.qrcode.decoder.DataMaskBase;
	import com.google.zxing.qrcode.decoder.DataMask000;
	import com.google.zxing.qrcode.decoder.DataMask001;
	import com.google.zxing.qrcode.decoder.DataMask010;
	import com.google.zxing.qrcode.decoder.DataMask011;
	import com.google.zxing.qrcode.decoder.DataMask100;
	import com.google.zxing.qrcode.decoder.DataMask101;
	import com.google.zxing.qrcode.decoder.DataMask110;
	import com.google.zxing.qrcode.decoder.DataMask111;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;

public class DataMask
{
  public function DataMask() {}
	
  private static var DATA_MASKS:Array = [
      new DataMask000(),
      new DataMask001(),
      new DataMask010(),
      new DataMask011(),
      new DataMask100(),
      new DataMask101(),
      new DataMask110(),
      new DataMask111(),
  ];




  /**
   * @param reference a value between 0 and 7 indicating one of the eight possible
   * data mask patterns a QR Code may use
   * @return {@link DataMask} encapsulating the data mask pattern
   */
  public static function  forReference(reference:int):DataMaskBase 
  {
    if (reference < 0 || reference > 7) 
    {
      throw new IllegalArgumentException("QRCode : Decoder : DataMask : forReference : reference invalid");
    }
    return DATA_MASKS[reference];
  }
}
}
