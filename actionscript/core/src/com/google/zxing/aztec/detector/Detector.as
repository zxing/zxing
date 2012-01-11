/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.aztec.detector
{
	import com.google.zxing.common.detector.WhiteRectangleDetector;
	
	public class Detector 
	{

		import com.google.zxing.NotFoundException;
		import com.google.zxing.ResultPoint;
		import com.google.zxing.aztec.AztecDetectorResult;
		import com.google.zxing.common.BitMatrix;
		import com.google.zxing.common.GridSampler;
		//import com.google.zxing.common.detector.WhiteRectangleDetector;
		import com.google.zxing.common.reedsolomon.GenericGF;
		import com.google.zxing.common.reedsolomon.GenericGFPoly;
		import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
		import com.google.zxing.common.reedsolomon.ReedSolomonException;
		import com.google.zxing.aztec.Point;

		/**
	 	* <p>Encapsulates logic that can detect an Aztec Code in an image, even if the Aztec Code
	 	* is rotated or skewed, or partially obscured.</p>
		 *
	 	* @author David Olivier
	 	*/


  		private var image:BitMatrix;

		private var compact:Boolean;
		private var nbLayers:int;
		private var nbDataBlocks:int;
		private var nbCenterLayers:int;
		private var shift:int;

  		public function Detector(image:BitMatrix) 
  		{
    		this.image = image;
  		}

  		/**
   		* <p>Detects an Aztec Code in an image.</p>
   		*
   		* @return {@link AztecDetectorResult} encapsulating results of detecting an Aztec Code
   		* @throws NotFoundException if no Aztec Code can be found
   		*/
  		public function detect():AztecDetectorResult 
  		{

    		// 1. Get the center of the aztec matrix
     		var pCenter:Point  = getMatrixCenter();

     		// 2. Get the corners of the center bull's eye
     		var bullEyeCornerPoints:Array = getBullEyeCornerPoints(pCenter);

     		// 3. Get the size of the matrix from the bull's eye
    		extractParameters(bullEyeCornerPoints);
    
    		// 4. Get the corners of the matrix
    		var corners:Array = getMatrixCornerPoints(bullEyeCornerPoints);
    
   			// 5. Sample the grid
     		var bits:BitMatrix = sampleGrid(image, corners[shift%4], corners[(shift+3)%4], corners[(shift+2)%4], corners[(shift+1)%4]);
//    tot hier correct
    		return new AztecDetectorResult(bits, corners, compact, nbDataBlocks, nbLayers);
  		}

  		/**
   		* <p> Extracts the number of data layers and data blocks from the layer around the bull's eye </p>
   		*
   		* @param bullEyeCornerPoints the array of bull's eye corners
   		* @throws NotFoundException in case of too many errors or invalid parameters
   		*/
  		public function extractParameters(bullEyeCornerPoints:Array ):void
  		{

    		// Get the bits around the bull's eye
		    var resab:Array = sampleLine(bullEyeCornerPoints[0], bullEyeCornerPoints[1], 2*nbCenterLayers+1);
		    var resbc:Array = sampleLine(bullEyeCornerPoints[1], bullEyeCornerPoints[2], 2*nbCenterLayers+1);
		    var rescd:Array = sampleLine(bullEyeCornerPoints[2], bullEyeCornerPoints[3], 2*nbCenterLayers+1);
		    var resda:Array = sampleLine(bullEyeCornerPoints[3], bullEyeCornerPoints[0], 2*nbCenterLayers+1);

    		// Determine the orientation of the matrix
    		if (resab[0] && resab[2 * nbCenterLayers]) 
    		{
      			shift = 0;
    		} 
    		else if (resbc[0] && resbc[2 * nbCenterLayers]) 
    		{
      			shift = 1;
    		} 
    		else if (rescd[0] && rescd[2 * nbCenterLayers]) 
    		{
      			shift = 2;
    		} 
    		else if (resda[0] && resda[2 * nbCenterLayers]) 
    		{
      			shift = 3;
    		} 
    		else 
    		{
      			throw NotFoundException.getNotFoundInstance();
    		}
    
		    //d      a
		    //
		    //c      b
    
    		// Flatten the bits in a single array
    		var parameterData:Array;
    		var shiftedParameterData:Array;
    		if (compact) 
    		{
      			shiftedParameterData = new Array();
      			for (var i:int = 0; i < 7; i++) 
      			{
        			shiftedParameterData[i] = resab[2+i];
        			shiftedParameterData[i+7] = resbc[2+i];
        			shiftedParameterData[i+14] = rescd[2+i];
        			shiftedParameterData[i+21] = resda[2+i];
      			}
        
      			parameterData = new Array();
        		for (var iiii:int = 0; iiii < 28; iiii++) 
        		{
          			parameterData[iiii] = shiftedParameterData[(iiii+shift*7)%28];
        		}
    		} 
    		else 
    		{
      			shiftedParameterData = new Array();
      			for (var iii:int = 0; iii < 11; iii++) 
      			{
        			if (iii < 5) 
        			{
          				shiftedParameterData[iii] = resab[2+iii];
          				shiftedParameterData[iii+10] = resbc[2+iii];
         				shiftedParameterData[iii+20] = rescd[2+iii];
          				shiftedParameterData[iii+30] = resda[2+iii];
        			}
        			if (iii > 5) 
        			{
          				shiftedParameterData[iii-1] = resab[2+iii];
          				shiftedParameterData[iii+10-1] = resbc[2+iii];
          				shiftedParameterData[iii+20-1] = rescd[2+iii];
          				shiftedParameterData[iii+30-1] = resda[2+iii];
        			}
      			}
        
      			parameterData = new Array();
        		for (var ij:int = 0; ij < 40; ij++) 
        		{
          			parameterData[ij] = shiftedParameterData[int((ij+shift*10)%40)];
        		}
    		}
    
    		// corrects the error using RS algorithm
    		correctParameterData(parameterData, compact);
    
    		// gets the parameters from the bit array
    		getParameters(parameterData);
  		}

  		/**
  	 	*
   		* <p>Gets the Aztec code corners from the bull's eye corners and the parameters </p>
   		*
   		* @param bullEyeCornerPoints the array of bull's eye corners
   		* @return the array of aztec code corners
   		* @throws NotFoundException if the corner points do not fit in the image
   		*/
  		private function getMatrixCornerPoints(bullEyeCornerPoints:Array):Array 
  		{

    		var ratio:Number = (2 * nbLayers + (nbLayers > 4 ? 1 : 0) + int((nbLayers - 4) / 8)) / (2.0 * nbCenterLayers);//BAS : added int cast in order to do the same calculation as Java

    		var dx:int = bullEyeCornerPoints[0].x-bullEyeCornerPoints[2].x;
    		dx+=dx>0?1:-1;
    		var dy:int = bullEyeCornerPoints[0].y-bullEyeCornerPoints[2].y;
    		dy+=dy>0?1:-1;
    
    		var targetcx:int = round(bullEyeCornerPoints[2].x-ratio*dx);
    		var targetcy:int = round(bullEyeCornerPoints[2].y-ratio*dy);
    
		    var targetax:int = round(bullEyeCornerPoints[0].x+ratio*dx);
		    var targetay:int = round(bullEyeCornerPoints[0].y+ratio*dy);
		    
		    dx = bullEyeCornerPoints[1].x-bullEyeCornerPoints[3].x;
		    dx+=dx>0?1:-1;
		    dy = bullEyeCornerPoints[1].y-bullEyeCornerPoints[3].y;
		    dy+=dy>0?1:-1;
		    
		    var targetdx:int = round(bullEyeCornerPoints[3].x-ratio*dx);
		    var targetdy:int = round(bullEyeCornerPoints[3].y-ratio*dy);
		    var targetbx:int = round(bullEyeCornerPoints[1].x+ratio*dx);
		    var targetby:int = round(bullEyeCornerPoints[1].y+ratio*dy);
		    
		    if (!isValid(targetax, targetay) || !isValid(targetbx, targetby) || !isValid(targetcx, targetcy) || !isValid(targetdx, targetdy)) 
		    {
	      		throw NotFoundException.getNotFoundInstance();
	    	}
	    
    return new Array(new ResultPoint(targetax, targetay), new ResultPoint(targetbx, targetby), new ResultPoint(targetcx, targetcy), new ResultPoint(targetdx, targetdy)); 
  }

  /**
   *
   * <p> Corrects the parameter bits using Reed-Solomon algorithm </p>
   *
   * @param parameterData paremeter bits
   * @param compact true if this is a compact Aztec code
   * @throws NotFoundException if the array contains too many errors
   */
  private static function correctParameterData(parameterData:Array, compact:Boolean):void 
  {

    var numCodewords:int;
    var numDataCodewords:int;

    if (compact) {
      numCodewords = 7;
      numDataCodewords = 2;
    } else {
      numCodewords = 10;
      numDataCodewords = 4;
    }

    var numECCodewords:int = numCodewords - numDataCodewords;
    var parameterWords:Array = new Array(numCodewords);//new int[numCodewords];
    for(var m:int=0;m<parameterWords.length;m++) { parameterWords[m] = 0; }

    var codewordSize:int = 4;
    for (var i:int = 0; i < numCodewords; i++) {
      var flag:int = 1;
      for (var j:int = 1; j <= codewordSize; j++) {
        if (parameterData[codewordSize*i + codewordSize - j]) {
          parameterWords[i] += flag;
        }
        flag <<= 1;
      }
    }

    try 
    {
      var rsDecoder:ReedSolomonDecoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
      rsDecoder.decode(parameterWords, numECCodewords);
    } 
    catch (rse:ReedSolomonException ) 
    {
      throw NotFoundException.getNotFoundInstance();
    }
    
    for (var i2:int = 0; i2 < numDataCodewords; i2 ++) {
        flag = 1;
        for (var jk:int = 1; jk <= codewordSize; jk++) {
          parameterData[i2*codewordSize+codewordSize-jk] = (parameterWords[i2] & flag) == flag;
          flag <<= 1;
        }
    }
  }
  
  /**
   * 
   * <p> Finds the corners of a bull-eye centered on the passed point </p>
   * 
   * @param pCenter Center point
   * @return The corners of the bull-eye
   * @throws NotFoundException If no valid bull-eye can be found
   */
  private function getBullEyeCornerPoints(pCenter:Point ):Array 
  {
    
    var pina:Point = pCenter;
    var pinb:Point = pCenter;
    var pinc:Point = pCenter;
    var pind:Point = pCenter;

    var color:Boolean = true;
    
    for (nbCenterLayers = 1; nbCenterLayers < 9; nbCenterLayers++) {
      var pouta:Point = getFirstDifferent(pina, color, 1, -1);
      var poutb:Point = getFirstDifferent(pinb, color, 1, 1);
      var poutc:Point = getFirstDifferent(pinc, color, -1, 1);
      var poutd:Point = getFirstDifferent(pind, color, -1, -1);

      //d      a
      //
      //c      b

      if (nbCenterLayers>2) {
        var q:Number = distance(poutd, pouta)*nbCenterLayers/(distance(pind, pina)*(nbCenterLayers+2));
        if ( q < 0.75 || q > 1.25 || !isWhiteOrBlackRectangle(pouta, poutb, poutc, poutd)) {
          break;
        }
      }

      pina = pouta;
      pinb = poutb;
      pinc = poutc;
      pind = poutd;

      color = !color;
    }

    if (nbCenterLayers != 5 && nbCenterLayers != 7) 
    {
      throw NotFoundException.getNotFoundInstance();
    }
    
    compact = nbCenterLayers==5;
    
    var ratio:Number = 0.75*2/(2*nbCenterLayers-3);
    
    var dx:int = pina.x-pinc.x;
    var dy:int = pina.y-pinc.y;
    var targetcx:int = round(pinc.x-ratio*dx);
    var targetcy:int = round(pinc.y-ratio*dy);
    var targetax:int = round(pina.x+ratio*dx);
    var targetay:int = round(pina.y+ratio*dy);
    
    dx = pinb.x-pind.x;
    dy = pinb.y-pind.y;
    
    var targetdx:int = round(pind.x-ratio*dx);
    var targetdy:int = round(pind.y-ratio*dy);
    var targetbx:int = round(pinb.x+ratio*dx);
    var targetby:int = round(pinb.y+ratio*dy);
    
    if (!isValid(targetax, targetay) || !isValid(targetbx, targetby)
        || !isValid(targetcx, targetcy) || !isValid(targetdx, targetdy)) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    var pa:Point = new Point(targetax,targetay);
    var pb:Point = new Point(targetbx,targetby);
    var pc:Point = new Point(targetcx,targetcy);
    var pd:Point = new Point(targetdx,targetdy);
    
    return new Array(pa, pb, pc, pd);
  }

  /**
   *
   * Finds a candidate center point of an Aztec code from an image
   *
   * @return the center point
   */
  private function getMatrixCenter():Point  {

    var pointA:ResultPoint ;
    var pointB:ResultPoint ;
    var pointC:ResultPoint ;
    var pointD:ResultPoint ;

    //Get a white rectangle that can be the border of the matrix in center bull's eye or
    try {

      var cornerPoints:Array = new WhiteRectangleDetector(image).detect();
      pointA = cornerPoints[0];
      pointB = cornerPoints[1];
      pointC = cornerPoints[2];
      pointD = cornerPoints[3];

    } catch (e:NotFoundException) {

      // This exception can be in case the initial rectangle is white
      // In that case, surely in the bull's eye, we try to expand the rectangle.
      var cx:int = int(image.getWidth()/2);
      var cy:int = int(image.getHeight()/2);
      pointA = getFirstDifferent(new Point(cx+int(15/2), cy-int(15/2)), false, 1, -1).toResultPoint();
      pointB = getFirstDifferent(new Point(cx+int(15/2), cy+int(15/2)), false, 1, 1).toResultPoint();
      pointC = getFirstDifferent(new Point(cx-int(15/2), cy+int(15/2)), false, -1, 1).toResultPoint();
      pointD = getFirstDifferent(new Point(cx-int(15/2), cy-int(15/2)), false, -1, -1).toResultPoint();

    }
    
    //Compute the center of the rectangle
    var cx2:int = round((pointA.getX() + pointD.getX() + pointB.getX() + pointC.getX())/4);
    var cy2:int = round((pointA.getY() + pointD.getY() + pointB.getY() + pointC.getY())/4);

    // Redetermine the white rectangle starting from previously computed center.
    // This will ensure that we end up with a white rectangle in center bull's eye
    // in order to compute a more accurate center.
    try {
      var cornerPoints2:Array = new WhiteRectangleDetector(image, 15, cx2, cy2).detect();
      pointA = cornerPoints2[0];
      pointB = cornerPoints2[1];
      pointC = cornerPoints2[2];
      pointD = cornerPoints2[3];
    } catch (e:NotFoundException) {

      // This exception can be in case the initial rectangle is white
      // In that case we try to expand the rectangle.
      pointA = getFirstDifferent(new Point(cx2+int(15/2), cy2-int(15/2)), false, 1, -1).toResultPoint();
      pointB = getFirstDifferent(new Point(cx2+int(15/2), cy2+int(15/2)), false, 1, 1).toResultPoint();
      pointC = getFirstDifferent(new Point(cx2-int(15/2), cy2+int(15/2)), false, -1, 1).toResultPoint();
      pointD = getFirstDifferent(new Point(cx2-int(15/2), cy2-int(15/2)), false, -1, -1).toResultPoint();

    }
    
    // Recompute the center of the rectangle
    cx = round((pointA.getX() + pointD.getX() + pointB.getX() + pointC.getX())/4);
    cy = round((pointA.getY() + pointD.getY() + pointB.getY() + pointC.getY())/4);

    return new Point(cx, cy);
  }

  /**
   * Samples an Aztec matrix from an image
   */
  private function sampleGrid(image:BitMatrix ,
          topLeft:ResultPoint ,
          bottomLeft:ResultPoint ,
          bottomRight:ResultPoint ,
          topRight:ResultPoint ):BitMatrix {

    var dimension:int;
    if (compact) {
      dimension = 4*nbLayers+11;
    } else {
      if (nbLayers <= 4) {
        dimension = 4*nbLayers + 15;
      } else {
        dimension = 4*nbLayers + 2*(int((nbLayers-4)/8) + 1) + 15 ;
      }
    }

    var sampler:GridSampler = GridSampler.getGridSamplerInstance();

    return sampler.sampleGrid2(image,
      dimension,
      dimension,
      0.5,
      0.5,
      dimension - 0.5,
      0.5,
      dimension - 0.5,
      dimension - 0.5,
      0.5,
      dimension - 0.5,
      topLeft.getX(),
      topLeft.getY(),
      topRight.getX(),
      topRight.getY(),
      bottomRight.getX(),
      bottomRight.getY(),
      bottomLeft.getX(),
      bottomLeft.getY());
  }
  
  /**
   * Sets number of layers and number of datablocks from parameter bits
   */
  private function  getParameters(parameterData:Array):void {

    var nbBitsForNbLayers:int;
    var nbBitsForNbDatablocks:int;

    if (compact) {
      nbBitsForNbLayers = 2;
      nbBitsForNbDatablocks = 6;
    } else {
      nbBitsForNbLayers = 5;
      nbBitsForNbDatablocks = 11;
    }

    for (var i:int = 0; i < nbBitsForNbLayers; i++) {
      nbLayers <<= 1;
      if (parameterData[i]) {
        nbLayers += 1;
      }
    }

    for (var i3:int = nbBitsForNbLayers; i3 < nbBitsForNbLayers + nbBitsForNbDatablocks; i3++) {
      nbDataBlocks <<= 1;
      if (parameterData[i3]) {
        nbDataBlocks += 1;
      }
    }

    nbLayers ++;
    nbDataBlocks ++;

  }

  /**
   *
   * Samples a line
   *
   * @param p1 first point
   * @param p2 second point
   * @param size number of bits
   * @return the array of bits
   */
  private function sampleLine(p1:Point,  p2:Point,size:int):Array {

    var res:Array = new Array(size);
    var d:Number = distance(p1,p2);
    var moduleSize:Number = d/(size-1);
    var dx:Number = moduleSize*(p2.x - p1.x)/d;
    var dy:Number = moduleSize*(p2.y - p1.y)/d;

    var px:Number = p1.x;
    var py:Number = p1.y;

    for (var i:int = 0; i < size; i++) {
      res[i] = image._get(round(px), round(py));
      px+=dx;
      py+=dy;
    }

    return res;
  }

  /**
   * @return true if the border of the rectangle passed in parameter is compound of white points only
   * or black points only
   */
  private function isWhiteOrBlackRectangle(p1:Point, p2:Point,  p3:Point,  p4:Point):Boolean {

    var corr:int = 3;

    p1 = new Point(p1.x-corr, p1.y+corr);
    p2 = new Point(p2.x-corr, p2.y-corr);
    p3 = new Point(p3.x+corr, p3.y-corr);
    p4 = new Point(p4.x+corr, p4.y+corr);

    var cInit:int = getColor(p4, p1);

    if (cInit == 0) {
      return false;
    }

    var c:int = getColor(p1, p2);

    if (c!=cInit || c == 0) {
      return false;
    }

    c = getColor(p2, p3);

    if (c!=cInit || c == 0) {
      return false;
    }

    c = getColor(p3, p4);

    return c == cInit && c != 0;

  }

  /**
   * Gets the color of a segment
   *
   * @return 1 if segment more than 90% black, -1 if segment is more than 90% white, 0 else
   */
  private function getColor( p1:Point,  p2:Point):int {
    var d:Number = distance(p1,p2);
    var dx:Number = (p2.x - p1.x)/d;
    var dy:Number = (p2.y - p1.y)/d;
    var error:int = 0;

    var px:Number = p1.x;
    var py:Number = p1.y;

    var colorModel:Boolean = image._get(p1.x, p1.y);

    for (var i:int = 0; i < d; i++) {
      px+=dx;
      py+=dy;
      if (image._get(round(px), round(py)) != colorModel) {
        error++;
      }
    }

    var errRatio:Number = Number(error/d);

    if (errRatio > 0.1 && errRatio < 0.9) {
      return 0;
    }

    if (errRatio <= 0.1) {
      return colorModel?1:-1;
    } else {
      return colorModel?-1:1;
    }
  }

  /**
   * Gets the coordinate of the first point with a different color in the given direction
   */
  private function getFirstDifferent(init:Point, color:Boolean, dx:int, dy:int):Point {
    var x:int = init.x+dx;
    var y:int = init.y+dy;

    while(isValid(x,y) && image._get(x,y) == color) {
      x+=dx;
      y+=dy;
    }

    x-=dx;
    y-=dy;

    while(isValid(x,y) && image._get(x, y) == color) {
      x+=dx;
    }
    x-=dx;

    while(isValid(x,y) && image._get(x, y) == color) {
      y+=dy;
    }
    y-=dy;

    return new Point(x,y);
  }
  


  private function isValid(x:int, y:int):Boolean {
    return x >= 0 && x < image.getWidth() && y > 0 && y < image.getHeight();
  }

  /**
   * Ends up being a bit faster than Math.round(). This merely rounds its
   * argument to the nearest int, where x.5 rounds up.
   */
  private static function round(d:Number):int {
    return (int) (d + 0.5);
  }

  // L2 distance
  private static function distance(a:Point, b:Point ):Number 
  {
     return Math.sqrt((a.x - b.x)
        * (a.x - b.x) + (a.y - b.y)
        * (a.y - b.y));
  }
	}
}
