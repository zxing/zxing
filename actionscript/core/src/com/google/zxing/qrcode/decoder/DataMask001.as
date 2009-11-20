package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.qrcode.decoder.DataMask;
	
  /**
   * 001: mask bits for which x mod 2 == 0
   */
  public class DataMask001 extends DataMaskBase
  {
  	 
    public override function isMasked(i:int, j:int):Boolean 
    {
      return (i & 0x01) == 0;
    }
  }
}