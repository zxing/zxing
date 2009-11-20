package com.google.zxing.oned
{
   	import com.google.zxing.common.BitArray;
   	import com.google.zxing.common.flexdatatypes.HashTable;
   	import com.google.zxing.BinaryBitmap;
   	import com.google.zxing.Result;

	  /**
     * <p>This interfaces captures addtional functionality that readers of
     * UPC/EAN family of barcodes should expose.</p>
     *
     * @author Sean Owen
     */

    public class UPCEANReader implements OneDReader 
    {

           /**
           * <p>Like {@link #decodeRow(int, BitArray, java.util.Hashtable)}, but
           * allows caller to inform method about where the UPC/EAN start pattern is
           * found. This allows this to be computed once and reused across many implementations.</p>
           */
          
          public function decodeRow_Array(rowNumber:int, row:BitArray, startGuardRange:Array):Result 
          {
          	return null;
          };
          
          public function decode(image:BinaryBitmap, hints:HashTable=null):Result
          {
          	return null;
          }
    }

}