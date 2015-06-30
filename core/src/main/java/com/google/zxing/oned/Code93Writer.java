package com.google.zxing.oned;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.OneDimensionalCodeWriter;
import java.util.Map;

/**
 * This object renders a CODE93 code as a {@link BitMatrix}.
 * 
 * @author codewell4@gmail.com (Damjan Kejžar)
 */
public final class Code93Writer extends OneDimensionalCodeWriter
{
	// Note that 'abcd' are dummy characters in place of control characters.
	private static final String ALPHABET_STRING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%abcd*";
	private static final String ASTERISK = "*";

	/**
	* These represent the encodings of characters, as patterns of wide and narrow bars.
	* The 9 least-significant bits of each int correspond to the pattern of wide and narrow.
	*/
	private static final int[] CHARACTER_ENCODINGS =
	{
		0x114, 0x148, 0x144, 0x142, 0x128, 0x124, 0x122, 0x150, 0x112, 0x10A, // 0-9
		0x1A8, 0x1A4, 0x1A2, 0x194, 0x192, 0x18A, 0x168, 0x164, 0x162, 0x134, // A-J
		0x11A, 0x158, 0x14C, 0x146, 0x12C, 0x116, 0x1B4, 0x1B2, 0x1AC, 0x1A6, // K-T
		0x196, 0x19A, 0x16C, 0x166, 0x136, 0x13A, // U-Z
		0x12E, 0x1D4, 0x1D2, 0x1CA, 0x16E, 0x176, 0x1AE, // - - %
		0x126, 0x1DA, 0x1D6, 0x132, 0x15E, // Control chars? $-*
	};
	

	@Override
	public BitMatrix encode(String contents, BarcodeFormat format, int width, int height, Map<EncodeHintType,?> hints) throws WriterException 
	{
		if (format != BarcodeFormat.CODE_93)
		{
			throw new IllegalArgumentException("Can only encode CODE_93, but got " + format);
		}
		return super.encode(contents, format, width, height, hints);
	}
	
	@Override
	public boolean[] encode(String contents)
	{
		//Calculate and add the 2 checksum symbols
		for (int mod = 20; mod >= 15; mod -= 5)
		{
			int checksum = 0;
			for (int i = 0; i < contents.length(); i++)
			{
				int code = ALPHABET_STRING.indexOf(contents.charAt(contents.length() - 1 - i));
				int weight = i % mod + 1;
				checksum = (checksum + code * weight) % 47;
			}
			contents += ALPHABET_STRING.charAt(checksum);
		}
		//Add the beggining and end symbol - asterisk
		contents = ASTERISK + contents + ASTERISK;

		int length = contents.length();
		int[] widths = new int[9];
		int codeWidth = length;
		for (int i = 0; i < length; i++)
		{
			int indexInString = ALPHABET_STRING.indexOf(contents.charAt(i));
			if (indexInString < 0)
			{
				throw new IllegalArgumentException("Bad contents: " + contents);
			}

			toIntArray(CHARACTER_ENCODINGS[indexInString], widths);
			for (int width : widths)
			{
				codeWidth += width;
			}
		}

		boolean[] result = new boolean[codeWidth];
		int pos = 0;
		for (int cnt = 0; cnt < contents.length(); cnt++)
		{
			int indexInString = ALPHABET_STRING.indexOf(contents.charAt(cnt));
			toIntArray(CHARACTER_ENCODINGS[indexInString], widths);
			pos += appendPattern(result, pos, widths, true);
		}
		int[] narrowBlack = {2};
		pos += appendPattern(result, pos, narrowBlack, true);
		return result;
	}
	
	/**
	* @param target encode black/white pattern into this array
	* @param pos position to start encoding at in {@code target}
	* @param pattern lengths of black/white runs to encode
	* @param startColor starting color - false for white, true for black
	* @return the number of elements added to target.
	*/
	protected static int appendPattern(boolean[] target, int pos, int[] pattern, boolean startColor) {
		boolean color = startColor;
		int numAdded = 0;
		for (int len : pattern)
		{
			//Write output as-is
			if (len==1) {color = false;}
			else {color = true;}

			for (int j = 0; j < len; j++)
			{
				target[pos++] = color;
			}
			numAdded += len;
		}
		return numAdded;
	}

	//Binary representation of number in array
	private static void toIntArray(int a, int[] toReturn)
	{
		for (int i = 0; i < 9; i++)
		{
			int temp = a & (1 << (8 - i));
			toReturn[i] = temp == 0 ? 1 : 2;
		}
	}
}
