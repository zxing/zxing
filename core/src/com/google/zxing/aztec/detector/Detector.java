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

package com.google.zxing.aztec.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.aztec.AztecDetectorResult;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GridSampler;
import com.google.zxing.common.detector.MathUtils;
import com.google.zxing.common.detector.WhiteRectangleDetector;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonDecoder;
import com.google.zxing.common.reedsolomon.ReedSolomonException;

/**
 * <p>Encapsulates logic that can detect an Aztec Code in an image, even if the Aztec Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author David Olivier
 */
public final class Detector {

  private final BitMatrix image;

  private boolean compact;
  private int nbLayers;
  private int nbDataBlocks;
  private int nbCenterLayers;
  private int shift;

  public Detector(BitMatrix image) {
    this.image = image;
  }

  /**
   * <p>Detects an Aztec Code in an image.</p>
   *
   * @return {@link AztecDetectorResult} encapsulating results of detecting an Aztec Code
   * @throws NotFoundException if no Aztec Code can be found
   */
  public AztecDetectorResult detect() throws NotFoundException {

    // 1. Get the center of the aztec matrix
     Point pCenter = getMatrixCenter();

     // 2. Get the corners of the center bull's eye
     Point[] bullEyeCornerPoints = getBullEyeCornerPoints(pCenter);

     // 3. Get the size of the matrix from the bull's eye
    extractParameters(bullEyeCornerPoints);
    
    // 4. Get the corners of the matrix
    ResultPoint[] corners = getMatrixCornerPoints(bullEyeCornerPoints);
    
    // 5. Sample the grid
    BitMatrix bits = sampleGrid(image, corners[shift%4], corners[(shift+3)%4], corners[(shift+2)%4], corners[(shift+1)%4]);
    
    return new AztecDetectorResult(bits, corners, compact, nbDataBlocks, nbLayers);
  }

  /**
   * <p> Extracts the number of data layers and data blocks from the layer around the bull's eye </p>
   *
   * @param bullEyeCornerPoints the array of bull's eye corners
   * @throws NotFoundException in case of too many errors or invalid parameters
   */
  private void extractParameters(Point[] bullEyeCornerPoints)
      throws NotFoundException {

    // Get the bits around the bull's eye
    boolean[] resab = sampleLine(bullEyeCornerPoints[0], bullEyeCornerPoints[1], 2*nbCenterLayers+1);
    boolean[] resbc = sampleLine(bullEyeCornerPoints[1], bullEyeCornerPoints[2], 2*nbCenterLayers+1);
    boolean[] rescd = sampleLine(bullEyeCornerPoints[2], bullEyeCornerPoints[3], 2*nbCenterLayers+1);
    boolean[] resda = sampleLine(bullEyeCornerPoints[3], bullEyeCornerPoints[0], 2*nbCenterLayers+1);

    // Determine the orientation of the matrix
    if (resab[0] && resab[2 * nbCenterLayers]) {
      shift = 0;
    } else if (resbc[0] && resbc[2 * nbCenterLayers]) {
      shift = 1;
    } else if (rescd[0] && rescd[2 * nbCenterLayers]) {
      shift = 2;
    } else if (resda[0] && resda[2 * nbCenterLayers]) {
      shift = 3;
    } else {
      throw NotFoundException.getNotFoundInstance();
    }
    
    //d      a
    //
    //c      b
    
    // Flatten the bits in a single array
    boolean[] parameterData;
    boolean[] shiftedParameterData;
    if (compact) {
      shiftedParameterData = new boolean[28];
      for (int i = 0; i < 7; i++) {
        shiftedParameterData[i] = resab[2+i];
        shiftedParameterData[i+7] = resbc[2+i];
        shiftedParameterData[i+14] = rescd[2+i];
        shiftedParameterData[i+21] = resda[2+i];
      }
        
      parameterData = new boolean[28];
        for (int i = 0; i < 28; i++) {
          parameterData[i] = shiftedParameterData[(i+shift*7)%28];
        }
    } else {
      shiftedParameterData = new boolean[40];
      for (int i = 0; i < 11; i++) {
        if (i < 5) {
          shiftedParameterData[i] = resab[2+i];
          shiftedParameterData[i+10] = resbc[2+i];
          shiftedParameterData[i+20] = rescd[2+i];
          shiftedParameterData[i+30] = resda[2+i];
        }
        if (i > 5) {
          shiftedParameterData[i-1] = resab[2+i];
          shiftedParameterData[i+10-1] = resbc[2+i];
          shiftedParameterData[i+20-1] = rescd[2+i];
          shiftedParameterData[i+30-1] = resda[2+i];
        }
      }
        
      parameterData = new boolean[40];
        for (int i = 0; i < 40; i++) {
          parameterData[i] = shiftedParameterData[(i+shift*10)%40];
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
  private ResultPoint[] getMatrixCornerPoints(Point[] bullEyeCornerPoints) throws NotFoundException {

    float ratio = (2 * nbLayers + (nbLayers > 4 ? 1 : 0) + (nbLayers - 4) / 8)
        / (2.0f * nbCenterLayers);

    int dx = bullEyeCornerPoints[0].x-bullEyeCornerPoints[2].x;
    dx+=dx>0?1:-1;
    int dy = bullEyeCornerPoints[0].y-bullEyeCornerPoints[2].y;
    dy+=dy>0?1:-1;
    
    int targetcx = MathUtils.round(bullEyeCornerPoints[2].x - ratio * dx);
    int targetcy = MathUtils.round(bullEyeCornerPoints[2].y - ratio * dy);
    
    int targetax = MathUtils.round(bullEyeCornerPoints[0].x + ratio * dx);
    int targetay = MathUtils.round(bullEyeCornerPoints[0].y + ratio * dy);
    
    dx = bullEyeCornerPoints[1].x-bullEyeCornerPoints[3].x;
    dx+=dx>0?1:-1;
    dy = bullEyeCornerPoints[1].y-bullEyeCornerPoints[3].y;
    dy+=dy>0?1:-1;
    
    int targetdx = MathUtils.round(bullEyeCornerPoints[3].x - ratio * dx);
    int targetdy = MathUtils.round(bullEyeCornerPoints[3].y - ratio * dy);
    int targetbx = MathUtils.round(bullEyeCornerPoints[1].x + ratio * dx);
    int targetby = MathUtils.round(bullEyeCornerPoints[1].y+ratio*dy);
    
    if (!isValid(targetax, targetay) || !isValid(targetbx, targetby) || !isValid(targetcx, targetcy) || !isValid(targetdx, targetdy)) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    return new ResultPoint[]{new ResultPoint(targetax, targetay), new ResultPoint(targetbx, targetby), new ResultPoint(targetcx, targetcy), new ResultPoint(targetdx, targetdy)}; 
  }

  /**
   *
   * <p> Corrects the parameter bits using Reed-Solomon algorithm </p>
   *
   * @param parameterData paremeter bits
   * @param compact true if this is a compact Aztec code
   * @throws NotFoundException if the array contains too many errors
   */
  private static void correctParameterData(boolean[] parameterData, boolean compact) throws NotFoundException {

    int numCodewords;
    int numDataCodewords;

    if (compact) {
      numCodewords = 7;
      numDataCodewords = 2;
    } else {
      numCodewords = 10;
      numDataCodewords = 4;
    }

    int numECCodewords = numCodewords - numDataCodewords;
    int[] parameterWords = new int[numCodewords];

    int codewordSize = 4;
    for (int i = 0; i < numCodewords; i++) {
      int flag = 1;
      for (int j = 1; j <= codewordSize; j++) {
        if (parameterData[codewordSize*i + codewordSize - j]) {
          parameterWords[i] += flag;
        }
        flag <<= 1;
      }
    }

    try {
      ReedSolomonDecoder rsDecoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
      rsDecoder.decode(parameterWords, numECCodewords);
    } catch (ReedSolomonException rse) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    for (int i = 0; i < numDataCodewords; i ++) {
        int flag = 1;
        for (int j = 1; j <= codewordSize; j++) {
          parameterData[i*codewordSize+codewordSize-j] = (parameterWords[i] & flag) == flag;
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
  private Point[] getBullEyeCornerPoints(Point pCenter) throws NotFoundException {
    
    Point pina = pCenter;
    Point pinb = pCenter;
    Point pinc = pCenter;
    Point pind = pCenter;

    boolean color = true;
    
    for (nbCenterLayers = 1; nbCenterLayers < 9; nbCenterLayers++) {
      Point pouta = getFirstDifferent(pina, color, 1, -1);
      Point poutb = getFirstDifferent(pinb, color, 1, 1);
      Point poutc = getFirstDifferent(pinc, color, -1, 1);
      Point poutd = getFirstDifferent(pind, color, -1, -1);

      //d      a
      //
      //c      b

      if (nbCenterLayers>2) {
        float q = distance(poutd, pouta)*nbCenterLayers/(distance(pind, pina)*(nbCenterLayers+2));
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

    if (nbCenterLayers != 5 && nbCenterLayers != 7) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    compact = nbCenterLayers==5;
    
    float ratio = 0.75f*2/(2*nbCenterLayers-3);
    
    int dx = pina.x-pinc.x;
    int dy = pina.y-pinc.y;
    int targetcx = MathUtils.round(pinc.x-ratio*dx);
    int targetcy = MathUtils.round(pinc.y-ratio*dy);
    int targetax = MathUtils.round(pina.x+ratio*dx);
    int targetay = MathUtils.round(pina.y+ratio*dy);
    
    dx = pinb.x-pind.x;
    dy = pinb.y-pind.y;
    
    int targetdx = MathUtils.round(pind.x-ratio*dx);
    int targetdy = MathUtils.round(pind.y-ratio*dy);
    int targetbx = MathUtils.round(pinb.x+ratio*dx);
    int targetby = MathUtils.round(pinb.y+ratio*dy);
    
    if (!isValid(targetax, targetay) || !isValid(targetbx, targetby)
        || !isValid(targetcx, targetcy) || !isValid(targetdx, targetdy)) {
      throw NotFoundException.getNotFoundInstance();
    }
    
    Point pa = new Point(targetax,targetay);
    Point pb = new Point(targetbx,targetby);
    Point pc = new Point(targetcx,targetcy);
    Point pd = new Point(targetdx,targetdy);
    
    return new Point[]{pa, pb, pc, pd};
  }

  /**
   *
   * Finds a candidate center point of an Aztec code from an image
   *
   * @return the center point
   */
  private Point getMatrixCenter() {

    ResultPoint pointA;
    ResultPoint pointB;
    ResultPoint pointC;
    ResultPoint pointD;

    //Get a white rectangle that can be the border of the matrix in center bull's eye or
    try {

      ResultPoint[] cornerPoints = new WhiteRectangleDetector(image).detect();
      pointA = cornerPoints[0];
      pointB = cornerPoints[1];
      pointC = cornerPoints[2];
      pointD = cornerPoints[3];

    } catch (NotFoundException e) {

      // This exception can be in case the initial rectangle is white
      // In that case, surely in the bull's eye, we try to expand the rectangle.
      int cx = image.getWidth()/2;
      int cy = image.getHeight()/2;
      pointA = getFirstDifferent(new Point(cx+15/2, cy-15/2), false, 1, -1).toResultPoint();
      pointB = getFirstDifferent(new Point(cx+15/2, cy+15/2), false, 1, 1).toResultPoint();
      pointC = getFirstDifferent(new Point(cx-15/2, cy+15/2), false, -1, 1).toResultPoint();
      pointD = getFirstDifferent(new Point(cx-15/2, cy-15/2), false, -1, -1).toResultPoint();

    }
    
    //Compute the center of the rectangle
    int cx = MathUtils.round((pointA.getX() + pointD.getX() + pointB.getX() + pointC.getX())/4);
    int cy = MathUtils.round((pointA.getY() + pointD.getY() + pointB.getY() + pointC.getY())/4);

    // Redetermine the white rectangle starting from previously computed center.
    // This will ensure that we end up with a white rectangle in center bull's eye
    // in order to compute a more accurate center.
    try {
      ResultPoint[] cornerPoints = new WhiteRectangleDetector(image, 15, cx, cy).detect();
      pointA = cornerPoints[0];
      pointB = cornerPoints[1];
      pointC = cornerPoints[2];
      pointD = cornerPoints[3];
    } catch (NotFoundException e) {

      // This exception can be in case the initial rectangle is white
      // In that case we try to expand the rectangle.
      pointA = getFirstDifferent(new Point(cx+15/2, cy-15/2), false, 1, -1).toResultPoint();
      pointB = getFirstDifferent(new Point(cx+15/2, cy+15/2), false, 1, 1).toResultPoint();
      pointC = getFirstDifferent(new Point(cx-15/2, cy+15/2), false, -1, 1).toResultPoint();
      pointD = getFirstDifferent(new Point(cx-15/2, cy-15/2), false, -1, -1).toResultPoint();

    }
    
    // Recompute the center of the rectangle
    cx = MathUtils.round((pointA.getX() + pointD.getX() + pointB.getX() + pointC.getX())/4);
    cy = MathUtils.round((pointA.getY() + pointD.getY() + pointB.getY() + pointC.getY())/4);

    return new Point(cx, cy);
  }

  /**
   * Samples an Aztec matrix from an image
   */
  private BitMatrix sampleGrid(BitMatrix image,
                               ResultPoint topLeft,
                               ResultPoint bottomLeft,
                               ResultPoint bottomRight,
                               ResultPoint topRight) throws NotFoundException {

    int dimension;
    if (compact) {
      dimension = 4*nbLayers+11;
    } else {
      if (nbLayers <= 4) {
        dimension = 4*nbLayers + 15;
      } else {
        dimension = 4*nbLayers + 2*((nbLayers-4)/8 + 1) + 15 ;
      }
    }

    GridSampler sampler = GridSampler.getInstance();

    return sampler.sampleGrid(image,
      dimension,
      dimension,
      0.5f,
      0.5f,
      dimension - 0.5f,
      0.5f,
      dimension - 0.5f,
      dimension - 0.5f,
      0.5f,
      dimension - 0.5f,
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
  private void getParameters(boolean[] parameterData) {

    int nbBitsForNbLayers;
    int nbBitsForNbDatablocks;

    if (compact) {
      nbBitsForNbLayers = 2;
      nbBitsForNbDatablocks = 6;
    } else {
      nbBitsForNbLayers = 5;
      nbBitsForNbDatablocks = 11;
    }

    for (int i = 0; i < nbBitsForNbLayers; i++) {
      nbLayers <<= 1;
      if (parameterData[i]) {
        nbLayers += 1;
      }
    }

    for (int i = nbBitsForNbLayers; i < nbBitsForNbLayers + nbBitsForNbDatablocks; i++) {
      nbDataBlocks <<= 1;
      if (parameterData[i]) {
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
  private boolean[] sampleLine(Point p1, Point p2, int size) {

    boolean[] res = new boolean[size];
    float d = distance(p1,p2);
    float moduleSize = d/(size-1);
    float dx = moduleSize*(p2.x - p1.x)/d;
    float dy = moduleSize*(p2.y - p1.y)/d;

    float px = p1.x;
    float py = p1.y;

    for (int i = 0; i < size; i++) {
      res[i] = image.get(MathUtils.round(px), MathUtils.round(py));
      px+=dx;
      py+=dy;
    }

    return res;
  }

  /**
   * @return true if the border of the rectangle passed in parameter is compound of white points only
   * or black points only
   */
  private boolean isWhiteOrBlackRectangle(Point p1,
                                          Point p2,
                                          Point p3,
                                          Point p4) {

    int corr = 3;

    p1 = new Point(p1.x-corr, p1.y+corr);
    p2 = new Point(p2.x-corr, p2.y-corr);
    p3 = new Point(p3.x+corr, p3.y-corr);
    p4 = new Point(p4.x+corr, p4.y+corr);

    int cInit = getColor(p4, p1);

    if (cInit == 0) {
      return false;
    }

    int c = getColor(p1, p2);

    if (c != cInit) {
      return false;
    }

    c = getColor(p2, p3);

    if (c != cInit) {
      return false;
    }

    c = getColor(p3, p4);

    return c == cInit;

  }

  /**
   * Gets the color of a segment
   *
   * @return 1 if segment more than 90% black, -1 if segment is more than 90% white, 0 else
   */
  private int getColor(Point p1, Point p2) {
    float d = distance(p1,p2);
    float dx = (p2.x - p1.x)/d;
    float dy = (p2.y - p1.y)/d;
    int error = 0;

    float px = p1.x;
    float py = p1.y;

    boolean colorModel = image.get(p1.x, p1.y);

    for (int i = 0; i < d; i++) {
      px+=dx;
      py+=dy;
      if (image.get(MathUtils.round(px), MathUtils.round(py)) != colorModel) {
        error++;
      }
    }

    float errRatio = (float)error/d;

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
  private Point getFirstDifferent(Point init, boolean color, int dx, int dy) {
    int x = init.x+dx;
    int y = init.y+dy;

    while(isValid(x,y) && image.get(x,y) == color) {
      x+=dx;
      y+=dy;
    }

    x-=dx;
    y-=dy;

    while(isValid(x,y) && image.get(x, y) == color) {
      x+=dx;
    }
    x-=dx;

    while(isValid(x,y) && image.get(x, y) == color) {
      y+=dy;
    }
    y-=dy;

    return new Point(x,y);
  }
  
  private static final class Point {
    public final int x;
    public final int y;

    public ResultPoint toResultPoint() {
      return new ResultPoint(x, y);
    }

    private Point(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  private boolean isValid(int x, int y) {
    return x >= 0 && x < image.getWidth() && y > 0 && y < image.getHeight();
  }

  private static float distance(Point a, Point b) {
    return MathUtils.distance(a.x, a.y, b.x, b.y);
  }

}
