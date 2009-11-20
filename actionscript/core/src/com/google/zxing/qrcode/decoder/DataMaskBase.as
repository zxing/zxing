package com.google.zxing.qrcode.decoder
{
		import com.google.zxing.common.BitMatrix;
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;

	public class DataMaskBase
	{
    	  /**
		   * <p>Implementations of this method reverse the data masking process applied to a QR Code and
		   * make its bits ready to read.</p>
		   *
		   * @param bits representation of QR Code bits
		   * @param dimension dimension of QR Code, represented by bits, being unmasked
		   */
		  public function unmaskBitMatrix(bits:BitMatrix , dimension:int):void {
		    for (var i:int = 0; i < dimension; i++) {
		      for (var j:int = 0; j < dimension; j++) {
		        if (isMasked(i, j)) {
		          bits.flip(j, i);
		        }
		      }
		    }
		  }
		  
		  public function isMasked(i:int, j:int):Boolean {return false;}

	}
}