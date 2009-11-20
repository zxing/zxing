package com.google.zxing.qrcode.decoder
{
		import com.google.zxing.qrcode.decoder.DataMaskBase;

  /**
   * 110: mask bits for which (xy mod 2 + xy mod 3) mod 2 == 0
   */
  public class DataMask110 extends DataMaskBase
  {
  	 
    public override function isMasked(i:int, j:int):Boolean 
    {
      var temp:int = i * j;
      return (((temp & 0x01) + (temp % 3)) & 0x01) == 0;
    }
  }


}