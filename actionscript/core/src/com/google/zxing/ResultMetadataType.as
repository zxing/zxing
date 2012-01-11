package com.google.zxing
{
	import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
	import com.google.zxing.common.flexdatatypes.HashTable;
	
	public class ResultMetadataType
	{
		 // No, we can't use an enum here. J2ME doesn't support it.

          /**
           * Unspecified, application-specific metadata. Maps to an unspecified {@link Object}.
           */
          public static var OTHER:ResultMetadataType = new ResultMetadataType("OTHER");

          /**
           * Denotes the likely approximate orientation of the barcode in the image. This value
           * is given as degrees rotated clockwise from the normal, upright orientation.
           * For example a 1D barcode which was found by reading top-to-bottom would be
           * said to have orientation "90". This key maps to an {@link Integer} whose
           * value is in the range [0,360).
           */
          public static var ORIENTATION:ResultMetadataType = new ResultMetadataType("ORIENTATION");

          /**
           * <p>2D barcode formats typically encode text, but allow for a sort of 'byte mode'
           * which is sometimes used to encode binary data. While {@link Result} makes available
           * the complete raw bytes in the barcode for these formats, it does not offer the bytes
           * from the byte segments alone.</p>
           *
           * <p>This maps to a {@link java.util.Vector} of byte arrays corresponding to the
           * raw bytes in the byte segments in the barcode, in order.</p>
           */
          public static var BYTE_SEGMENTS:ResultMetadataType = new ResultMetadataType("BYTE SEGMENTS");

          /**
		   * Error correction level used, if applicable. The value type depends on the
		   * format, but is typically a String.
		   */
		  public static var ERROR_CORRECTION_LEVEL:ResultMetadataType = new ResultMetadataType("ORIENTATION");


         /**
		   * For some periodicals, indicates the issue number as an {@link Integer}.
		   */
		  public static var ISSUE_NUMBER:ResultMetadataType = new ResultMetadataType("ISSUE_NUMBER");

		  /**
		   * For some products, indicates the suggested retail price in the barcode as a
		   * formatted {@link String}.
		   */
		  public static var SUGGESTED_PRICE:ResultMetadataType = new ResultMetadataType("SUGGESTED_PRICE");

		  /**
		   * For some products, the possible country of manufacture as a {@link String} denoting the
		   * ISO country code. Some map to multiple possible countries, like "US/CA".
		   */
		  public static var POSSIBLE_COUNTRY:ResultMetadataType = new ResultMetadataType("POSSIBLE_COUNTRY");

		  private var name:String ;
		  
		  private static var  VALUES:HashTable = new HashTable();
		
		  public function ResultMetadataType(name:String = "") {
		    this.name = name;
		    //VALUES.put(name, this);
		  }

		  public function getName():String {
		    return name;
		  }

		  public function toString():String {
		    return name;
		  }

		  public static function valueOf(name:String):ResultMetadataType 
		  {
		    if (name == null || name.length == 0) 
		    {
		      throw new IllegalArgumentException();
		    }
		   
		   var format:ResultMetadataType = ResultMetadataType(VALUES._get(name));
		    if (format == null) 
		    {
		      throw new IllegalArgumentException();
		    }
		    return format;
		  }

	}
}