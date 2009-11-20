package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.qrcode.decoder.DataMaskBase;

  /**
   * 111: mask bits for which ((x+y)mod 2 + xy mod 3) mod 2 == 0
   */
  public class DataMask111 extends DataMaskBase 
  {

    public override function isMasked(i:int, j:int):Boolean {
      return ((((i + j) & 0x01) + ((i * j) % 3)) & 0x01) == 0;
    }
  }

}