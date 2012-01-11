/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.zxing
{
	/**
 * <p>Encapsulates the result of decoding a barcode within an image.</p>
 *
 * @author Sean Owen
 */
	public class Result
	{
		import com.google.zxing.common.flexdatatypes.HashTable;
		import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
		import com.google.zxing.common.flexdatatypes.Enumeration;
		
		  protected var text:String;
          protected var rawBytes:Array;
          protected var resultPoints:Array;
          protected var format:BarcodeFormat;
          protected var resultMetadata:HashTable;
          protected var timestamp:Number;

          public function Result( text:String,
                         		  	rawBytes:Array,
                        			resultPoints:Array,
                        			format:BarcodeFormat,
                        			timestamp:Number = 0) 
          {
            if (text == null && rawBytes == null) {
              throw new IllegalArgumentException("Result : Text and bytes are both null");
            }
            

            if (timestamp == 0) { timestamp = Math.round((new Date()).getTime()/1000); }
            this.text = text;
            this.rawBytes = rawBytes;
            this.resultPoints = resultPoints;
            this.format = format;
            this.resultMetadata = null;
             this.timestamp = timestamp;
          }

          /**
           * @return raw text encoded by the barcode, if applicable, otherwise <code>null</code>
           */
          public function getText():String {
            return text;
          }

          /**
           * @return raw bytes encoded by the barcode, if applicable, otherwise <code>null</code>
           */
          public function getRawBytes():Array {
            return rawBytes;
          }

          /**
           * @return points related to the barcode in the image. These are typically points
           *         identifying finder patterns or the corners of the barcode. The exact meaning is
           *         specific to the type of barcode that was decoded.
           */
          public function getResultPoints():Array {
            return resultPoints;
          }

          /**
           * @return {@link BarcodeFormat} representing the format of the barcode that was recognized and decoded
           */
          public function getBarcodeFormat():BarcodeFormat {
            return format;
          }

          /**
           * @return {@link HashTable} mapping {@link ResultMetadataType} keys to values. May be <code>null</code>.
           *  This contains optional metadata about what was detected about the barcode, like orientation.
           */
          public function getResultMetadata():HashTable {
            return resultMetadata;
          }

          public function putMetadata(type:ResultMetadataType, value:Object ):void {
            if (resultMetadata == null) {
              resultMetadata = new HashTable(3);
            }
            resultMetadata.Add(type, value);
          }

          public function toString():String {
            if (text == null) {
              return "[" + rawBytes.length + " bytes]";
            } else {
              return text;
            }
          }
          
          public function putAllMetadata(metadata:HashTable ):void  
          {
    		if (metadata != null) {
     			if (resultMetadata == null) {
       			 resultMetadata = metadata;
   				   } else {
    		    var e:Enumeration  = new Enumeration(metadata.keys());
        			while (e.hasMoreElement()) {
		          var key:ResultMetadataType  =  (e.nextElement() as ResultMetadataType);
		          var value:Object  = metadata._get(key);
		          resultMetadata._put(key, value);
		        }
		      }
		    }
		  }
		  
		 public function addResultPoints(newPoints:Array):void 
		 {
    		if (resultPoints == null) {
      			resultPoints = newPoints;
    			} else if (newPoints != null && newPoints.length > 0) {
		      var allPoints:Array = new Array(resultPoints.length + newPoints.length);
		      //System.arraycopy(resultPoints, 0, allPoints, 0, resultPoints.length);
		      for (var i:int=0;i<resultPoints.length;i++)
		      {
		      	allPoints[i] = resultPoints[i];
		      }
		      for (var j:int=0;j<newPoints.length;j++)
		      {
		      	allPoints[i + resultPoints.length] = newPoints[i];
		      }
		      
		      //System.arraycopy(newPoints, 0, allPoints, resultPoints.length, newPoints.length);     
		      resultPoints = allPoints;
		    }
		  }
		  
	    public function  getTimestamp():Number 
	    {
			return timestamp;
  		}
		  
		  
		    
 
	}
}