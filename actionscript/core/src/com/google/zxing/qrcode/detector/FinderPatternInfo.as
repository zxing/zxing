package com.google.zxing.qrcode.detector
{
   public class FinderPatternInfo
    { 
    	
          private  var bottomLeft:FinderPattern;
          private  var topLeft:FinderPattern;
          private  var topRight:FinderPattern;

          public function FinderPatternInfo( patternCenters:Array) {
            this.bottomLeft = patternCenters[0];
            this.topLeft = patternCenters[1];
            this.topRight = patternCenters[2];
          }

          public function getBottomLeft():FinderPattern
          {
            return bottomLeft;
          }

          public function getTopLeft():FinderPattern
          {
            return topLeft;
          }

          public function getTopRight():FinderPattern
          {
            return topRight;
          }
    
    }


}