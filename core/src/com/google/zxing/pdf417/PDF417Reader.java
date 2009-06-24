package com.google.zxing.pdf417;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.pdf417.decoder.Decoder;
import com.google.zxing.pdf417.detector.Detector;

import java.util.Hashtable;

/**
 * This implementation can detect and decode PDF417 codes in an image.
 *
 * @author SITA Lab (kevin.osullivan@sita.aero)
 */
public final class PDF417Reader implements Reader {
	private static final ResultPoint[] NO_POINTS = new ResultPoint[0];

	private final Decoder decoder = new Decoder();

	/**
	 * Locates and decodes a PDF417 code in an image.
	 *
	 * @return a String representing the content encoded by the PDF417 code
	 * @throws ReaderException if a PDF417 code cannot be found, or cannot be decoded
	 */
	public Result decode(MonochromeBitmapSource image) throws ReaderException {
		return decode(image, null);
	}

	public Result decode(MonochromeBitmapSource image, Hashtable hints)
			throws ReaderException {
		DecoderResult decoderResult;
		ResultPoint[] points;
		if (hints != null && hints.containsKey(DecodeHintType.PURE_BARCODE)) {
	        BitMatrix bits = extractPureBits(image);
	        decoderResult = decoder.decode(bits);
	        points = NO_POINTS;
	    } else {
	        DetectorResult detectorResult = new Detector(image).detect();
	        decoderResult = decoder.decode(detectorResult.getBits());
	        points = detectorResult.getPoints();
	    }
	    Result result = new Result(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat.PDF417);
	    return result;
	}

	/**
	 * This method detects a barcode in a "pure" image -- that is, pure monochrome image
	 * which contains only an unrotated, unskewed, image of a barcode, with some white border
	 * around it. This is a specialized method that works exceptionally fast in this special
	 * case.
	 */
	private static BitMatrix extractPureBits(MonochromeBitmapSource image) throws ReaderException {
		// Now need to determine module size in pixels
		
	    int height = image.getHeight();
	    int width = image.getWidth();
	    int minDimension = Math.min(height, width);

	    // First, skip white border by tracking diagonally from the top left down and to the right:
	    int borderWidth = 0;
	    while (borderWidth < minDimension && !image.isBlack(borderWidth, borderWidth)) {
	      borderWidth++;
	    }
	    if (borderWidth == minDimension) {
	      throw ReaderException.getInstance();
	    }

	    // And then keep tracking across the top-left black module to determine module size
	    int moduleEnd = borderWidth;
	    while (moduleEnd < minDimension && image.isBlack(moduleEnd, moduleEnd)) {
	      moduleEnd++;
	    }
	    if (moduleEnd == minDimension) {
	      throw ReaderException.getInstance();
	    }

	    int moduleSize = moduleEnd - borderWidth;

	    // And now find where the rightmost black module on the first row ends
	    int rowEndOfSymbol = width - 1;
	    while (rowEndOfSymbol >= 0 && !image.isBlack(rowEndOfSymbol, borderWidth)) {
	      rowEndOfSymbol--;
	    }
	    if (rowEndOfSymbol < 0) {
	      throw ReaderException.getInstance();
	    }
	    rowEndOfSymbol++;

	    // Make sure width of barcode is a multiple of module size
	    if ((rowEndOfSymbol - borderWidth) % moduleSize != 0) {
	      throw ReaderException.getInstance();
	    }
	    int dimension = (rowEndOfSymbol - borderWidth) / moduleSize;

	    // Push in the "border" by half the module width so that we start
	    // sampling in the middle of the module. Just in case the image is a
	    // little off, this will help recover.
	    borderWidth += moduleSize >> 1;

	    int sampleDimension = borderWidth + (dimension - 1) * moduleSize;
	    if (sampleDimension >= width || sampleDimension >= height) {
	      throw ReaderException.getInstance();
	    }

	    // Now just read off the bits
	    BitMatrix bits = new BitMatrix(dimension);
	    for (int i = 0; i < dimension; i++) {
	      int iOffset = borderWidth + i * moduleSize;
	      for (int j = 0; j < dimension; j++) {
	        if (image.isBlack(borderWidth + j * moduleSize, iOffset)) {
	          bits.set(i, j);
	        }
	      }
	    }
	    return bits;
	}
}

