package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.qrcode.decoder.DataMaskBase;

  /**
   * 101: mask bits for which xy mod 2 + xy mod 3 == 0
   */
  public class DataMask101 extends DataMaskBase 
  {

    public override function isMasked(i:int, j:int):Boolean 
    {
      var temp:int = i * j;
      return (temp & 0x01) + (int(temp % 3)) == 0;
    }
  }


}