/*
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

using System;
using System.Drawing;
using MonochromeBitmapSource = com.google.zxing.MonochromeBitmapSource;
using BlackPointEstimationMethod = com.google.zxing.BlackPointEstimationMethod;
using BitArray = com.google.zxing.common.BitArray;
using BlackPointEstimator = com.google.zxing.common.BlackPointEstimator;


namespace com.google.zxing.client.j2se
{
	
	/// <summary> <p>An implementation based upon {@link BufferedImage}. This provides access to the
	/// underlying image as if it were a monochrome image. Behind the scenes, it is evaluating
	/// the luminance of the underlying image by retrieving its pixels' RGB values.</p>
	/// 
	/// </summary>
	/// <author>  srowen@google.com (Sean Owen), Daniel Switkin (dswitkin@google.com)
	/// </author>
	public sealed class BufferedImageMonochromeBitmapSource : MonochromeBitmapSource
	{
		public bool iRotateSupported = false;

        public bool isRotateSupported() {
            return iRotateSupported;
        }

        public int getWidth() {
            return (iRotateSupported ? image.Height : image.Width);
        }

        public BlackPointEstimationMethod getLastEstimationMethod() {
            return lastMethod;
        }

        public int getHeight()
        {
            return (iRotateSupported ? image.Width : image.Height);
        }


        public MonochromeBitmapSource rotateCounterClockwise() {
            return null;
        }

        public BitArray getBlackColumn(int x, BitArray column, int startY, int getHeight) {
            return null;
        }
		
		//UPGRADE_NOTE: Final was removed from the declaration of 'image '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private System.Drawing.Bitmap image;
		private int blackPoint;
		private BlackPointEstimationMethod lastMethod;
		private int lastArgument;
		
		private const int LUMINANCE_BITS = 5;
		//UPGRADE_NOTE: Final was removed from the declaration of 'LUMINANCE_SHIFT '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int LUMINANCE_SHIFT = 8 - LUMINANCE_BITS;
		//UPGRADE_NOTE: Final was removed from the declaration of 'LUMINANCE_BUCKETS '. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1003'"
		private static readonly int LUMINANCE_BUCKETS = 1 << LUMINANCE_BITS;
		
		public BufferedImageMonochromeBitmapSource(System.Drawing.Bitmap image, bool rotated)
		{
			this.image = image;
			blackPoint = 0x7F;
			lastMethod = null;
			lastArgument = 0;
			iRotateSupported = rotated;
		}
		
		public bool isBlack(int x, int y)
		{
			return (iRotateSupported ? computeRGBLuminance(image.GetPixel(y, x).ToArgb()) < blackPoint : computeRGBLuminance(image.GetPixel(x, y).ToArgb()) < blackPoint);
		}

		int[] getRGB(int startx, int starty, int width)
		{
			int[] pixels = new int[width];
			for (int k = 0; k < width; k++)
			{
				Color c = (iRotateSupported ? image.GetPixel(starty, startx + k) : image.GetPixel(startx + k, starty));
				pixels[k] = ((int)c.R) << 16 | ((int)c.G) << 8 | ((int)c.B);
			}

			return pixels;
		}

		public BitArray getBlackRow(int y, BitArray row, int startX, int getWidth)
		{
			if (row == null)
			{
				row = new BitArray(getWidth);
			}
			else
			{
				row.clear();
			}
			//UPGRADE_ISSUE: Method 'java.awt.image.BufferedImage.getRGB' was not converted. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1000_javaawtimageBufferedImagegetRGB_int_int_int_int_int[]_int_int'"
			int[] pixelRow = getRGB(startX, y, getWidth);
			for (int i = 0; i < getWidth; i++)
			{
				if (computeRGBLuminance(pixelRow[i]) < blackPoint)
				{
					row.set(i);
				}
			}
			return row;
		}
		
		public void  estimateBlackPoint(BlackPointEstimationMethod method, int argument)
		{
			if (!method.Equals(lastMethod) || argument != lastArgument)
			{
                int width = getWidth();
                int height = getHeight();
				int[] histogram = new int[LUMINANCE_BUCKETS];
				float biasTowardsWhite = 1.0f;
				if (method.Equals(BlackPointEstimationMethod.TWO_D_SAMPLING))
				{
					int minDimension = width < height?width:height;
					int startI = height == minDimension?0:(height - width) >> 1;
					int startJ = width == minDimension?0:(width - height) >> 1;
					for (int n = 0; n < minDimension; n++)
					{
						int pixel = (iRotateSupported ? image.GetPixel(startI + n, startJ + n).ToArgb() : image.GetPixel(startJ + n, startI + n).ToArgb());
						histogram[computeRGBLuminance(pixel) >> LUMINANCE_SHIFT]++;
					}
				}
				else if (method.Equals(BlackPointEstimationMethod.ROW_SAMPLING))
				{
					if (argument < 0 || argument >= height)
					{
						throw new System.ArgumentException("Row is not within the image: " + argument);
					}
					biasTowardsWhite = 2.0f;
					int[] rgbArray = getRGB(0, argument, width);
					for (int x = 0; x < width; x++)
					{
						int l = computeRGBLuminance(rgbArray[x]);
						histogram[l >> LUMINANCE_SHIFT]++;
					}
				}
				else
				{
					//UPGRADE_TODO: The equivalent in .NET for method 'java.lang.Object.toString' may return a different value. "ms-help://MS.VSCC.v80/dv_commoner/local/redirect.htm?index='!DefaultContextWindowIndex'&keyword='jlca1043'"
					throw new System.ArgumentException("Unknown method: " + method);
				}
				blackPoint = BlackPointEstimator.estimate(histogram) << LUMINANCE_SHIFT;
				lastMethod = method;
				lastArgument = argument;
			}
		}
		
		/// <summary> Extracts luminance from a pixel from this source. By default, the source is assumed to use RGB,
		/// so this implementation computes luminance is a function of a red, green and blue components as
		/// follows:
		/// 
		/// <code>Y = 0.299R + 0.587G + 0.114B</code>
		/// 
		/// where R, G, and B are values in [0,1].
		/// </summary>
		private static int computeRGBLuminance(int pixel)
		{
			// Coefficients add up to 1024 to make the divide into a fast shift
			return (306 * ((pixel >> 16) & 0xFF) + 601 * ((pixel >> 8) & 0xFF) + 117 * (pixel & 0xFF)) >> 10;
		}
	}
}