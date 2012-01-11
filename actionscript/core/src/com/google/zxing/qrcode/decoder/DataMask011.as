package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.qrcode.decoder.DataMaskBase;

  /**
   * 011: mask bits for which (x + y) mod 3 == 0
   */
  public class DataMask011 extends DataMaskBase 
  {
     public override function isMasked(i:int, j:int):Boolean {
      return int((i + j) % 3) == 0;
    }
  }

}