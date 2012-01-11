package com.google.zxing.qrcode.encoder
{
      public class BlockPair 
      {
		import com.google.zxing.common.zxingByteArray;
		
        protected var dataBytes:zxingByteArray;
        protected var errorCorrectionBytes:zxingByteArray ;

        public function BlockPair(data:zxingByteArray,  errorCorrection:zxingByteArray) {
          dataBytes = data;
          errorCorrectionBytes = errorCorrection;
        }

        public function getDataBytes():zxingByteArray {
          return dataBytes;
        }

        public function getErrorCorrectionBytes():zxingByteArray {
          return errorCorrectionBytes;
        }

      }
}