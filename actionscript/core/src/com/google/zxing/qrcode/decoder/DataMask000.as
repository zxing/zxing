package com.google.zxing.qrcode.decoder
{
	import com.google.zxing.qrcode.decoder.DataMaskBase;
    /// <summary> 000: mask bits for which (i + j) mod 2 == 0</summary>
    public class DataMask000 extends DataMaskBase
    {

     	public override function isMasked(i:int, j:int):Boolean 
     	{
      		return ((i + j) & 0x01) == 0;
    	}    
    }

}