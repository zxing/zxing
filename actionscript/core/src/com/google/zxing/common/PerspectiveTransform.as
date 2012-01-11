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
package com.google.zxing.common
{
/**
 * <p>This class implements a perspective transform in two dimensions. Given four source and four
 * destination points, it will compute the transformation implied between them. The code is based
 * directly upon section 3.4.2 of George Wolberg's "Digital Image Warping"; see pages 54-56.</p>
 *
 * @author Sean Owen
 */
    public class PerspectiveTransform
    {
	    private var a11:Number;
        private var a12:Number;
        private var a13:Number;
        private var a21:Number;
        private var a22:Number;
        private var a23:Number;
        private var a31:Number;
        private var a32:Number;
        private var a33:Number;

        public function PerspectiveTransform( a11:Number, a21:Number,a31:Number, a12:Number, a22:Number, a32:Number, a13:Number,  a23:Number,  a33:Number)
        {
            this.a11 = a11;
            this.a12 = a12;
            this.a13 = a13;
            this.a21 = a21;
            this.a22 = a22;
            this.a23 = a23;
            this.a31 = a31;
            this.a32 = a32;
            this.a33 = a33;
        }

        public static function  quadrilateralToQuadrilateral(x0:Number,y0:Number,x1:Number,y1:Number,x2:Number,y2:Number,x3:Number,y3:Number,x0p:Number,y0p:Number,x1p:Number,y1p:Number,x2p:Number, y2p:Number,x3p:Number,y3p:Number):PerspectiveTransform
        {

            var qToS:PerspectiveTransform = quadrilateralToSquare(x0, y0, x1, y1, x2, y2, x3, y3);
            var sToQ:PerspectiveTransform = squareToQuadrilateral(x0p, y0p, x1p, y1p, x2p, y2p, x3p, y3p);
            return sToQ.times(qToS);
        }
        public function transformPoints(points:Array):Array
        {
            var max:int = points.length;
            var a11:Number = this.a11;
            var a12:Number = this.a12;
            var a13:Number = this.a13;
            var a21:Number = this.a21;
            var a22:Number = this.a22;
            var a23:Number = this.a23;
            var a31:Number = this.a31;
            var a32:Number = this.a32;
            var a33:Number = this.a33;
            for (var i:int = 0; i < max; i += 2)
            {
                var x:Number = points[i];
                var y:Number = points[i + 1];
                var denominator:Number = a13 * x + a23 * y + a33;
                points[i] = (a11 * x + a21 * y + a31) / denominator;
                points[i + 1] = (a12 * x + a22 * y + a32) / denominator;
            }
            return points;
        }

        public static function  squareToQuadrilateral(x0:Number,y0:Number,x1:Number,y1:Number,x2:Number,y2:Number,x3:Number,y3:Number):PerspectiveTransform
        {
            var dy2:Number = y3 - y2;
            var dy3:Number = y0 - y1 + y2 - y3;
            if (dy2 == 0 && dy3 == 0)
            {
                return new PerspectiveTransform(x1 - x0, x2 - x1, x0, y1 - y0, y2 - y1, y0, 0, 0, 1);
            }
            else
            {
                var dx1:Number = x1 - x2;
                var dx2:Number = x3 - x2;
                var dx3:Number = x0 - x1 + x2 - x3;
                var dy1:Number = y1 - y2;
                var denominator:Number = dx1 * dy2 - dx2 * dy1;
                var a13:Number = (dx3 * dy2 - dx2 * dy3) / denominator;
                var a23:Number = (dx1 * dy3 - dx3 * dy1) / denominator;
                return new PerspectiveTransform(x1 - x0 + a13 * x1, x3 - x0 + a23 * x3, x0, y1 - y0 + a13 * y1, y3 - y0 + a23 * y3, y0, a13, a23, 1);
            }
        }

        public static function quadrilateralToSquare(x0:Number,y0:Number,x1:Number,y1:Number,x2:Number,y2:Number,x3:Number,y3:Number):PerspectiveTransform
        {
            // Here, the adjoint serves as the inverse:
            return squareToQuadrilateral(x0, y0, x1, y1, x2, y2, x3, y3).buildAdjoint();
        }

        public function  buildAdjoint():PerspectiveTransform
        {
            // Adjoint is the transpose of the cofactor matrix:
            return new PerspectiveTransform(a22 * a33 - a23 * a32, 
            								a23 * a31 - a21 * a33, 
            								a21 * a32 - a22 * a31, 
            								a13 * a32 - a12 * a33, 
            								a11 * a33 - a13 * a31, 
            								a12 * a31 - a11 * a32, 
            								a12 * a23 - a13 * a22, 
            								a13 * a21 - a11 * a23, 
            								a11 * a22 - a12 * a21);
        }

        private function  times(other:PerspectiveTransform ):PerspectiveTransform
        {
            return new PerspectiveTransform(a11 * other.a11 + a21 * other.a12 + a31 * other.a13, 
            								a11 * other.a21 + a21 * other.a22 + a31 * other.a23, 
            								a11 * other.a31 + a21 * other.a32 + a31 * other.a33,
            								a12 * other.a11 + a22 * other.a12 + a32 * other.a13, 
            								a12 * other.a21 + a22 * other.a22 + a32 * other.a23, 
            								a12 * other.a31 + a22 * other.a32 + a32 * other.a33, 
            								a13 * other.a11 + a23 * other.a12 + a33 * other.a13, 
            								a13 * other.a21 + a23 * other.a22 + a33 * other.a23, 
            								a13 * other.a31 + a23 * other.a32 + a33 * other.a33);
        }
    }

}