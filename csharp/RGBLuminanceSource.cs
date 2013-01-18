using System;
using System.Drawing;

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

namespace com.google.zxing
{

	/// <summary>
	/// This class is used to help decode images from files which arrive as RGB data from
	/// an ARGB pixel array. It does not support rotation.
	/// 
	/// @author dswitkin@google.com (Daniel Switkin)
	/// @author Betaminos
	/// </summary>
	public sealed class RGBLuminanceSource : LuminanceSource
	{

	  private readonly sbyte[] luminances;
	  private readonly int dataWidth;
	  private readonly int dataHeight;
	  private readonly int left;
	  private readonly int top;

	  public RGBLuminanceSource(int width, int height, int[] pixels) : base(width, height)
	  {

		dataWidth = width;
		dataHeight = height;
		left = 0;
		top = 0;

		// In order to measure pure decoding speed, we convert the entire image to a greyscale array
		// up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
		luminances = new sbyte[width * height];
		for (int y = 0; y < height; y++)
		{
		  int offset = y * width;
		  for (int x = 0; x < width; x++)
		  {
			int pixel = pixels[offset + x];
			int r = (pixel >> 16) & 0xff;
			int g = (pixel >> 8) & 0xff;
			int b = pixel & 0xff;
			if (r == g && g == b)
			{
			  // Image is already greyscale, so pick any channel.
			  luminances[offset + x] = (sbyte) r;
			}
			else
			{
			  // Calculate luminance cheaply, favoring green.
			  luminances[offset + x] = (sbyte)((r + g + g + b) >> 2);
			}
		  }
		}
	  }

	  private RGBLuminanceSource(sbyte[] pixels, int dataWidth, int dataHeight, int left, int top, int width, int height) : base(width, height)
	  {
		if (left + width > dataWidth || top + height > dataHeight)
		{
		  throw new System.ArgumentException("Crop rectangle does not fit within image data.");
		}
		this.luminances = pixels;
		this.dataWidth = dataWidth;
		this.dataHeight = dataHeight;
		this.left = left;
		this.top = top;
	  }

      public RGBLuminanceSource(Bitmap d, int W, int H)
          : base(W, H)
      {
          int width = dataWidth = W;
          int height = dataHeight = H;
          // In order to measure pure decoding speed, we convert the entire image to a greyscale array
          // up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
          luminances = new sbyte[width * height];
          //if (format == PixelFormat.Format8bppIndexed)
          {
              Color c;
              for (int y = 0; y < height; y++)
              {
                  int offset = y * width;
                  for (int x = 0; x < width; x++)
                  {
                      c = d.GetPixel(x, y);
                      luminances[offset + x] = (sbyte)(((int)c.R) << 16 | ((int)c.G) << 8 | ((int)c.B));
                  }
              }
          }
      }

	  public override sbyte[] getRow(int y, sbyte[] row)
	  {
		if (y < 0 || y >= Height)
		{
		  throw new System.ArgumentException("Requested row is outside the image: " + y);
		}
		int width = Width;
		if (row == null || row.Length < width)
		{
		  row = new sbyte[width];
		}
		int offset = (y + top) * dataWidth + left;
		Array.Copy(luminances, offset, row, 0, width);
		return row;
	  }

	  public override sbyte[] Matrix
	  {
		  get
		  {
			int width = Width;
			int height = Height;
    
			// If the caller asks for the entire underlying image, save the copy and give them the
			// original data. The docs specifically warn that result.length must be ignored.
			if (width == dataWidth && height == dataHeight)
			{
			  return luminances;
			}
    
			int area = width * height;
			sbyte[] matrix = new sbyte[area];
			int inputOffset = top * dataWidth + left;
    
			// If the width matches the full width of the underlying data, perform a single copy.
			if (width == dataWidth)
			{
			  Array.Copy(luminances, inputOffset, matrix, 0, area);
			  return matrix;
			}
    
			// Otherwise copy one cropped row at a time.
			sbyte[] rgb = luminances;
			for (int y = 0; y < height; y++)
			{
			  int outputOffset = y * width;
			  Array.Copy(rgb, inputOffset, matrix, outputOffset, width);
			  inputOffset += dataWidth;
			}
			return matrix;
		  }
	  }

	  public override bool CropSupported
	  {
		  get
		  {
			return true;
		  }
	  }

	  public override LuminanceSource crop(int left, int top, int width, int height)
	  {
		return new RGBLuminanceSource(luminances, dataWidth, dataHeight, this.left + left, this.top + top, width, height);
	  }

	}

}