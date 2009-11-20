/*
 * Copyright 2009 ZXing authors
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

	import com.google.zxing.LuminanceSource;
	import mx.controls.Image;
	import flash.display.BitmapData;

/**
 * This LuminanceSource implementation is meant for J2SE clients and our blackbox unit tests.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class BufferedImageLuminanceSource extends LuminanceSource 
{

  private var image:BitmapData ;
  private var left:int;
  private var top:int;
  private var rgbData:Array;


 public function getRGB(left:int, top:int, width:int, height:int, rgb:Array, value:int, numPixels:int):void
 {
 	var cntr:int = 0;
 	for (var i:int=top;i<(top+height);i++)
 	{
 		for (var j:int=left;j<(left+width);j++)
 		{
 			rgb[cntr] = image.getPixel(j,i);
 			cntr++;
 		}
 	}
 }

  public function BufferedImageLuminanceSource(image:BitmapData, left:int=0, top:int=0, width:int=0, height:int=0) 
  {
  	if ((width==0) && (height==0))
  	{
  		width = image.width;
  		height = image.height;
  	}
  	super(width,height);

    var sourceWidth:int = image.width;
    var sourceHeight:int = image.height;
    if (left + width > sourceWidth || top + height > sourceHeight) {
      throw new Error("Crop rectangle does not fit within image data.");
    }

    this.image = image;
    this.left = left;
    this.top = top;
  }

  // These methods use an integer calculation for luminance derived from:
  // <code>Y = 0.299R + 0.587G + 0.114B</code>
  public override function getRow(y:int, row:Array):Array {
    if (y < 0 || y >= image.height) {
      throw new Error("Requested row is outside the image: " + y);
    }
    var width:int = image.width;
    if (row == null || row.length < width) {
      row = new Array(width);
    }

    if (rgbData == null || rgbData.length < width) 
    {
      rgbData = new Array(width);
    }
    this.getRGB(left, top + y, width, 1, rgbData, 0, image.width);
    for (var x:int = 0; x < width; x++) {
      var pixel:int = rgbData[x];
      var luminance:int = (306 * ((pixel >> 16) & 0xFF) +
          601 * ((pixel >> 8) & 0xFF) +
          117 * (pixel & 0xFF)) >> 10;
      row[x] = luminance;
    }
    return row;
  }

  public override function getMatrix():Array {
    var width:int = image.width;
    var height:int = image.height;
    var area:int = width * height;
    var matrix:Array = new Array(area);

    var rgb:Array = new Array(area);
    this.getRGB(left, top, width, height, rgb, 0, image.width);
    for (var y:int = 0; y < height; y++) {
      var offset:int = y * width;
      for (var x:int = 0; x < width; x++) {
        var pixel:int = rgb[offset + x];
        var luminance:int = (306 * ((pixel >> 16) & 0xFF) +
            601 * ((pixel >> 8) & 0xFF) +
            117 * (pixel & 0xFF)) >> 10;
        matrix[offset + x] = luminance;
      }
    }
    return matrix;
  }

  public override function isCropSupported():Boolean
  {
    return false;
  }

  public override function crop(left:int, top:int, width:int, height:int):LuminanceSource 
  {
  	// BAS : todo
    return new BufferedImageLuminanceSource(image, left, top, width, height);
  }



  // Can't run AffineTransforms on images of unknown format.
  public override function isRotateSupported():Boolean 
  {
  	return false;
  	//Bas : TOO 
    //return image.getType() != BufferedImage.TYPE_CUSTOM;
  }


  public override function rotateCounterClockwise():LuminanceSource 
  {
    if (!isRotateSupported()) 
    {
      throw new Error("Rotate not supported");
    }
    // Bas : todo
    return null; 
    /*
    var sourceWidth:int = image.getWidth();
    var sourceHeight:int = image.getHeight();

    // Rotate 90 degrees counterclockwise.
    var transform:AffineTransform = new AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, sourceWidth);
    var op:BufferedImageOp  = new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

    // Note width/height are flipped since we are rotating 90 degrees.
    var rotatedImage:BufferedImage = new BufferedImage(sourceHeight, sourceWidth, image.getType());
    op.filter(image, rotatedImage);

    // Maintain the cropped region, but rotate it too.
    var width:int = getWidth();
    return new BufferedImageLuminanceSource(rotatedImage, top, sourceWidth - (left + width),
        getHeight(), width);
        */
  }

}
}