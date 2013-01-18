using System.Text;

/*
 * Copyright 2008 ZXing authors
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

namespace com.google.zxing.qrcode.encoder
{

	using ErrorCorrectionLevel = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
	using Mode = com.google.zxing.qrcode.decoder.Mode;
	using Version = com.google.zxing.qrcode.decoder.Version;

	/// <summary>
	/// @author satorux@google.com (Satoru Takabayashi) - creator
	/// @author dswitkin@google.com (Daniel Switkin) - ported from C++
	/// </summary>
	public sealed class QRCode
	{

	  public const int NUM_MASK_PATTERNS = 8;

	  private Mode mode;
	  private ErrorCorrectionLevel ecLevel;
	  private Version version;
	  private int maskPattern;
	  private ByteMatrix matrix;

	  public QRCode()
	  {
		maskPattern = -1;
	  }

	  public Mode Mode
	  {
		  get
		  {
			return mode;
		  }
		  set
		  {
			mode = value;
		  }
	  }

	  public ErrorCorrectionLevel ECLevel
	  {
		  get
		  {
			return ecLevel;
		  }
		  set
		  {
			ecLevel = value;
		  }
	  }

	  public Version Version
	  {
		  get
		  {
			return version;
		  }
		  set
		  {
			this.version = value;
		  }
	  }

	  public int MaskPattern
	  {
		  get
		  {
			return maskPattern;
		  }
		  set
		  {
			maskPattern = value;
		  }
	  }

	  public ByteMatrix Matrix
	  {
		  get
		  {
			return matrix;
		  }
		  set
		  {
			matrix = value;
		  }
	  }

	  public override string ToString()
	  {
		StringBuilder result = new StringBuilder(200);
		result.Append("<<\n");
		result.Append(" mode: ");
		result.Append(mode);
		result.Append("\n ecLevel: ");
		result.Append(ecLevel);
		result.Append("\n version: ");
		result.Append(version);
		result.Append("\n maskPattern: ");
		result.Append(maskPattern);
		if (matrix == null)
		{
		  result.Append("\n matrix: null\n");
		}
		else
		{
		  result.Append("\n matrix:\n");
		  result.Append(matrix.ToString());
		}
		result.Append(">>\n");
		return result.ToString();
	  }






	  // Check if "mask_pattern" is valid.
	  public static bool isValidMaskPattern(int maskPattern)
	  {
		return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
	  }

	}

}