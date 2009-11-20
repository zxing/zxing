package com.google.zxing.qrcode.encoder
{
    public class QRCode 
    { 
    
    	import com.google.zxing.common.zxingByteArray;
    	import com.google.zxing.qrcode.decoder.Mode;
    	import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
    	import com.google.zxing.common.ByteMatrix;
    	import com.google.zxing.common.flexdatatypes.StringBuilder;
    	
    	    	
          public static var NUM_MASK_PATTERNS:int = 8;

          private var mode:Mode;
          private var ecLevel:ErrorCorrectionLevel;
          private var version:int;
          private var matrixWidth:int;
          private var maskPattern:int;
          private var numTotalBytes:int;
          private var numDataBytes:int;
          private var numECBytes:int;
          private var numRSBlocks:int;
          private var matrix:ByteMatrix;

          public function QRCode() {
            mode = null;
            ecLevel = null;
            version = -1;
            matrixWidth = -1;
            maskPattern = -1;
            numTotalBytes = -1;
            numDataBytes = -1;
            numECBytes = -1;
            numRSBlocks = -1;
            matrix = null;
          }

          // Mode of the QR Code.
          public function getMode():Mode {
            return mode;
          }

          // Error correction level of the QR Code.
          public function getECLevel():ErrorCorrectionLevel {
            return ecLevel;
          }

          // Version of the QR Code.  The bigger size, the bigger version.
          public function getVersion():int {
            return version;
          }

          // ByteMatrix width of the QR Code.
          public function getMatrixWidth():int {
            return matrixWidth;
          }

          // Mask pattern of the QR Code.
          public function getMaskPattern():int {
            return maskPattern;
          }

          // Number of total bytes in the QR Code.
          public function getNumTotalBytes():int {
            return numTotalBytes;
          }

          // Number of data bytes in the QR Code.
          public function getNumDataBytes():int {
            return numDataBytes;
          }

          // Number of error correction bytes in the QR Code.
          public function getNumECBytes():int {
            return numECBytes;
          }

          // Number of Reedsolomon blocks in the QR Code.
          public function getNumRSBlocks():int {
            return numRSBlocks;
          }

          // ByteMatrix data of the QR Code.
          public function getMatrix():ByteMatrix {
            return matrix;
          }
          

          // Return the value of the module (cell) pointed by "x" and "y" in the matrix of the QR Code. They
          // call cells in the matrix "modules". 1 represents a black cell, and 0 represents a white cell.
          public function at(x:int, y:int):int {
            // The value must be zero or one.
            var value:int = matrix._get(x, y);
            if (!(value == 0 || value == 1)) {
              // this is really like an assert... not sure what better exception to use?
              throw new Error("QRCode : Bad value");
            }
            return value;
          }

          // Checks all the member variables are set properly. Returns true on success. Otherwise, returns
          // false.
          public function isValid():Boolean 
          {
            return mode != null && // First check if all version are not uninitialized.
                ecLevel != null &&
                version != -1 &&
                matrixWidth != -1 &&
                maskPattern != -1 &&
                numTotalBytes != -1 &&
                numDataBytes != -1 &&
                numECBytes != -1 &&
                numRSBlocks != -1 && // Then check them in other ways..
                isValidMaskPattern(maskPattern) &&
                (numTotalBytes == (numDataBytes + numECBytes)) &&
                matrix != null && // ByteMatrix stuff.
                matrixWidth == matrix.width() && // See 7.3.1 of JISX0510:2004 (p.5).
                matrix.width() == matrix.height(); // Must be square.
          }

          // Return debug String.
          public function toString():String {
                var result:StringBuilder = new StringBuilder(200);
                result.Append("<<\n");
                result.Append(" mode: ");
                result.Append(mode);
                result.Append("\n ecLevel: ");
                result.Append(ecLevel);
                result.Append("\n version: ");
                result.Append(version);
                result.Append("\n matrixWidth: ");
                result.Append(matrixWidth);
                result.Append("\n maskPattern: ");
                result.Append(maskPattern);
                result.Append("\n numTotalBytes: ");
                result.Append(numTotalBytes);
                result.Append("\n numDataBytes: ");
                result.Append(numDataBytes);
                result.Append("\n numECBytes: ");
                result.Append(numECBytes);
                result.Append("\n numRSBlocks: ");
                result.Append(numRSBlocks);
                if (matrix == null) {
                  result.Append("\n matrix: null\n");
                } else {
                  result.Append("\n matrix:\n");
                  result.Append(matrix.toString());
                }
                result.Append(">>\n");
                return result.ToString();
          }

          public function setMode(value:Mode):void {
            mode = value;
          }

          public function setECLevel(value:ErrorCorrectionLevel):void {
            ecLevel = value;
          }

          public function setVersion(value:int):void {
            version = value;
          }

          public function setMatrixWidth(value:int):void {
            matrixWidth = value;
          }

          public function setMaskPattern(value:int) :void{
            maskPattern = value;
          }

          public function setNumTotalBytes(value:int):void {
            numTotalBytes = value;
          }

          public function setNumDataBytes(value:int):void {
            numDataBytes = value;
          }

          public function setNumECBytes(value:int):void {
            numECBytes = value;
          }

          public function setNumRSBlocks(value:int):void {
            numRSBlocks = value;
          }

          // This takes ownership of the 2D array.
          public function setMatrix( value:ByteMatrix):void {
            matrix = value;
          }

          // Check if "mask_pattern" is valid.
          public static function isValidMaskPattern(maskPattern:int):Boolean {
            return maskPattern >= 0 && maskPattern < QRCode.NUM_MASK_PATTERNS;
          }

    	}
    }