package com.google.zxing.qrcode.decoder
{
		import com.google.zxing.qrcode.decoder.DataMaskBase;

  /**
   * 100: mask bits for which (x/2 + y/3) mod 2 == 0
   */
  public class DataMask100 extends DataMaskBase {

    public override function isMasked(i:int, j:int):Boolean {
      return (((i >>> 1) + (j /3)) & 0x01) == 0;
    }
  }


    
}