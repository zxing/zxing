// ActionScript file
package com.google.zxing.aztec
{
	import com.google.zxing.ResultPoint;
	
  public class Point {
    public var x:int;
    public var y:int;

    public function  toResultPoint():ResultPoint {
      return new ResultPoint(x, y);
    }

    public function Point( x:int,  y:int) {
      this.x = x;
      this.y = y;
    }
  }
}
