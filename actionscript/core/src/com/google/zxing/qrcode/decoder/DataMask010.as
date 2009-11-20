package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.qrcode.decoder.DataMaskBase;
  /**
   * 010: mask bits for which y mod 3 == 0
   */
  public class DataMask010 extends DataMaskBase 
  {


    public override function isMasked(i:int, j:int):Boolean 
    {
      return j % 3 == 0;
    }
  }


 
}