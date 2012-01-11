package com.google.zxing.qrcode.encoder
{
	
	
	public class MatrixUtil 
    { 
    	import com.google.zxing.common.ByteMatrix;
    	import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
    	import com.google.zxing.WriterException;
    
          public function MatrixUtil() {
            // do nothing
          }

         private static var POSITION_DETECTION_PATTERN:Array =  [
              [1, 1, 1, 1, 1, 1, 1],
              [1, 0, 0, 0, 0, 0, 1],
              [1, 0, 1, 1, 1, 0, 1],
              [1, 0, 1, 1, 1, 0, 1],
              [1, 0, 1, 1, 1, 0, 1],
              [1, 0, 0, 0, 0, 0, 1],
              [1, 1, 1, 1, 1, 1, 1]
          ];

          private static  var HORIZONTAL_SEPARATION_PATTERN:Array = [
              [0, 0, 0, 0, 0, 0, 0, 0]
          ];

          private static var VERTICAL_SEPARATION_PATTERN:Array = [
              [0], [0], [0], [0], [0], [0], [0]
          ];

          private static var POSITION_ADJUSTMENT_PATTERN:Array = [
              [1, 1, 1, 1, 1],
              [1, 0, 0, 0, 1],
              [1, 0, 1, 0, 1],
              [1, 0, 0, 0, 1],
              [1, 1, 1, 1, 1]
          ];

          // From Appendix E. Table 1, JIS0510X:2004 (p 71). The table was double-checked by komatsu.
          private static var POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE:Array= [
              [-1, -1, -1, -1,  -1,  -1,  -1],  // Version 1
              [ 6, 18, -1, -1,  -1,  -1,  -1],  // Version 2
              [ 6, 22, -1, -1,  -1,  -1,  -1],  // Version 3
              [ 6, 26, -1, -1,  -1,  -1,  -1],  // Version 4
              [ 6, 30, -1, -1,  -1,  -1,  -1],  // Version 5
              [ 6, 34, -1, -1,  -1,  -1,  -1],  // Version 6
              [ 6, 22, 38, -1,  -1,  -1,  -1],  // Version 7
              [ 6, 24, 42, -1,  -1,  -1,  -1],  // Version 8
              [ 6, 26, 46, -1,  -1,  -1,  -1],  // Version 9
              [ 6, 28, 50, -1,  -1,  -1,  -1],  // Version 10
              [ 6, 30, 54, -1,  -1,  -1,  -1],  // Version 11
              [ 6, 32, 58, -1,  -1,  -1,  -1],  // Version 12
              [ 6, 34, 62, -1,  -1,  -1,  -1],  // Version 13
              [ 6, 26, 46, 66,  -1,  -1,  -1],  // Version 14
              [ 6, 26, 48, 70,  -1,  -1,  -1],  // Version 15
              [ 6, 26, 50, 74,  -1,  -1,  -1],  // Version 16
              [ 6, 30, 54, 78,  -1,  -1,  -1],  // Version 17
              [ 6, 30, 56, 82,  -1,  -1,  -1],  // Version 18
              [ 6, 30, 58, 86,  -1,  -1,  -1],  // Version 19
              [ 6, 34, 62, 90,  -1,  -1,  -1],  // Version 20
              [ 6, 28, 50, 72,  94,  -1,  -1],  // Version 21
              [ 6, 26, 50, 74,  98,  -1,  -1],  // Version 22
              [ 6, 30, 54, 78, 102,  -1,  -1],  // Version 23
              [ 6, 28, 54, 80, 106,  -1,  -1],  // Version 24
              [ 6, 32, 58, 84, 110,  -1,  -1],  // Version 25
              [ 6, 30, 58, 86, 114,  -1,  -1],  // Version 26
              [ 6, 34, 62, 90, 118,  -1,  -1],  // Version 27
              [ 6, 26, 50, 74,  98, 122,  -1],  // Version 28
              [ 6, 30, 54, 78, 102, 126,  -1],  // Version 29
              [ 6, 26, 52, 78, 104, 130,  -1],  // Version 30
              [ 6, 30, 56, 82, 108, 134,  -1],  // Version 31
              [ 6, 34, 60, 86, 112, 138,  -1],  // Version 32
              [ 6, 30, 58, 86, 114, 142,  -1],  // Version 33
              [ 6, 34, 62, 90, 118, 146,  -1],  // Version 34
              [ 6, 30, 54, 78, 102, 126, 150],  // Version 35
              [ 6, 24, 50, 76, 102, 128, 154],  // Version 36
              [ 6, 28, 54, 80, 106, 132, 158],  // Version 37
              [ 6, 32, 58, 84, 110, 136, 162],  // Version 38
              [ 6, 26, 54, 82, 110, 138, 166],  // Version 39
              [ 6, 30, 58, 86, 114, 142, 170]  // Version 40
          ];

          // Type info cells at the left top corner.
          private static var TYPE_INFO_COORDINATES:Array = [
              [8, 0],
              [8, 1],
              [8, 2],
              [8, 3],
              [8, 4],
              [8, 5],
              [8, 7],
              [8, 8],
              [7, 8],
              [5, 8],
              [4, 8],
              [3, 8],
              [2, 8],
              [1, 8],
              [0, 8]
          ];

          // From Appendix D in JISX0510:2004 (p. 67)
          private static  var VERSION_INFO_POLY:int = 0x1f25;  // 1 1111 0010 0101

          // From Appendix C in JISX0510:2004 (p.65).
          private static  var TYPE_INFO_POLY:int = 0x537;
          private static  var TYPE_INFO_MASK_PATTERN:int = 0x5412;

          // Set all cells to -1.  -1 means that the cell is empty (not set yet).
          //
          // JAVAPORT: We shouldn't need to do this at all. The code should be rewritten to begin encoding
          // with the ByteMatrix initialized all to zero.
          public static function clearMatrix( matrix:ByteMatrix):void {
              matrix.clear(-1);
          }

          // Build 2D matrix of QR Code from "dataBits" with "ecLevel", "version" and "getMaskPattern". On
          // success, store the result in "matrix" and return true.
          public static function buildMatrix( dataBits:BitVector,  ecLevel:ErrorCorrectionLevel,  version:int, maskPattern:int,  matrix:ByteMatrix):void 
          {
                    clearMatrix(matrix);
				    var result:String = matrix.toString2(); 
                    embedBasicPatterns(version, matrix);
                    result = matrix.toString2();
                    // Type information appear with any version.
                    embedTypeInfo(ecLevel, maskPattern, matrix);
                    result = matrix.toString2(); 
                    // Version info appear if version >= 7.
                    maybeEmbedVersionInfo(version, matrix);
                    result = matrix.toString2();
                    // Data should be embedded at end.
                    embedDataBits(dataBits, maskPattern, matrix);
                    result = matrix.toString2();
                    
          }

          // Embed basic patterns. On success, modify the matrix and return true.
          // The basic patterns are:
          // - Position detection patterns
          // - Timing patterns
          // - Dark dot at the left bottom corner
          // - Position adjustment patterns, if need be
          public static function embedBasicPatterns( version:int,  matrix:ByteMatrix):void
          {
              try
              {
                    // Let's get started with embedding big squares at corners.
                    embedPositionDetectionPatternsAndSeparators(matrix);
                    // Then, embed the dark dot at the left bottom corner.
                    embedDarkDotAtLeftBottomCorner(matrix);
                    
                    // Position adjustment patterns appear if version >= 2.
                    maybeEmbedPositionAdjustmentPatterns(version, matrix);
                    
                    // Timing patterns should be embedded after position adj. patterns.
                    embedTimingPatterns(matrix);
                    
              }catch(e:Error){
                throw new WriterException (e.message);
              }
          }

          // Embed type information. On success, modify the matrix.
          public static function embedTypeInfo(ecLevel:ErrorCorrectionLevel, maskPattern:int,  matrix:ByteMatrix):void
          {
            var typeInfoBits:BitVector  = new BitVector();
            makeTypeInfoBits(ecLevel, maskPattern, typeInfoBits);

            for (var i:int = 0; i < typeInfoBits.size(); ++i) {
              // Place bits in LSB to MSB order.  LSB (least significant bit) is the last value in
              // "typeInfoBits".
              var bit:int = typeInfoBits.at(typeInfoBits.size() - 1 - i);

              // Type info bits at the left top corner. See 8.9 of JISX0510:2004 (p.46).
              var x1:int = TYPE_INFO_COORDINATES[i][0];
              var y1:int = TYPE_INFO_COORDINATES[i][1];
              matrix._set(x1, y1, bit);

              if (i < 8) {
                // Right top corner.
                var x2:int = matrix.width() - i - 1;
                var y2:int = 8;
                matrix._set(x2, y2, bit);
              } else {
                // Left bottom corner.
                var x3:int = 8;
                var y3:int = matrix.height() - 7 + (i - 8);
                matrix._set(x3, y3, bit);
              }
            }
          }

          // Embed version information if need be. On success, modify the matrix and return true.
          // See 8.10 of JISX0510:2004 (p.47) for how to embed version information.
          public static function maybeEmbedVersionInfo(version:int,  matrix:ByteMatrix):void{
            if (version < 7) {  // Version info is necessary if version >= 7.
              return;  // Don't need version info.
            }
            var versionInfoBits:BitVector  = new BitVector();
            makeVersionInfoBits(version, versionInfoBits);

            var bitIndex:int = 6 * 3 - 1;  // It will decrease from 17 to 0.
            for (var i:int = 0; i < 6; ++i) {
              for (var j:int = 0; j < 3; ++j) {
                // Place bits in LSB (least significant bit) to MSB order.
                var bit:int = versionInfoBits.at(bitIndex);
                bitIndex--;
                // Left bottom corner.
                matrix._set(i,matrix.height() - 11 + j,  bit);
                // Right bottom corner.
                matrix._set( matrix.height() - 11 + j,i, bit);
              }
            }
          }

          // Embed "dataBits" using "getMaskPattern". On success, modify the matrix and return true.
          // For debugging purposes, it skips masking process if "getMaskPattern" is -1.
          // See 8.7 of JISX0510:2004 (p.38) for how to embed data bits.
          public static function embedDataBits(dataBits:BitVector,  maskPattern:int,  matrix:ByteMatrix):void
          {
            var bitIndex:int = 0;
            var direction:int = -1;
            // Start from the right bottom cell.
            var x:int = matrix.width() - 1;
            var y:int = matrix.height() - 1;
            while (x > 0) 
            {
              // Skip the vertical timing pattern.
              if (x == 6) 
              {
                x -= 1;
              }
              
              while (y >= 0 && y < matrix.height()) 
              {
              	var n:int = 0;
                for (var i:int = 0; i < 2; ++i) 
                {
                  var xx:int = x - i;
                  // Skip the cell if it's not empty.
                  var cellval:int = matrix._get(xx, y);
                  if (!isEmpty(cellval)) 
                  {
                    continue;
                  }
                  var bit:int;
                  if (bitIndex < dataBits.size()) 
                  {
                    bit = dataBits.at(bitIndex);
                    ++bitIndex;
                  } 
                  else 
                  {
                    // Padding bit. If there is no bit left, we'll fill the left cells with 0, as described
                    // in 8.4.9 of JISX0510:2004 (p. 24).
                    bit = 0;
                  }

                  // Skip masking if mask_pattern is -1.
                  if (maskPattern != -1) 
                  {
		            if (MaskUtil.getDataMaskBit(maskPattern, xx, y)) 
					{
		              bit ^= 0x1;
		            }
                  }
                  matrix._set(xx, y, bit);
                }
                y += direction;
              }
              
              direction = -direction;  // Reverse the direction.
              y += direction;
              x -= 2;  // Move to the left.
            }
            // All bits should be consumed.
            if (bitIndex != dataBits.size()) {
              throw new WriterException("Not all bits consumed: " + bitIndex + '/' + dataBits.size());
            }
          }

          // Return the position of the most significant bit set (to one) in the "value". The most
          // significant bit is position 32. If there is no bit set, return 0. Examples:
          // - findMSBSet(0) => 0
          // - findMSBSet(1) => 1
          // - findMSBSet(255) => 8
          public static function findMSBSet(value:int):int {
            var numDigits:int = 0;
            while (value != 0) {
              value >>= 1;
              ++numDigits;
            }
            return numDigits;
          }

          // Calculate BCH (Bose-Chaudhuri-Hocquenghem) code for "value" using polynomial "poly". The BCH
          // code is used for encoding type information and version information.
          // Example: Calculation of version information of 7.
          // f(x) is created from 7.
          //   - 7 = 000111 in 6 bits
          //   - f(x) = x^2 + x^2 + x^1
          // g(x) is given by the standard (p. 67)
          //   - g(x) = x^12 + x^11 + x^10 + x^9 + x^8 + x^5 + x^2 + 1
          // Multiply f(x) by x^(18 - 6)
          //   - f'(x) = f(x) * x^(18 - 6)
          //   - f'(x) = x^14 + x^13 + x^12
          // Calculate the remainder of f'(x) / g(x)
          //         x^2
          //         __________________________________________________
          //   g(x) )x^14 + x^13 + x^12
          //         x^14 + x^13 + x^12 + x^11 + x^10 + x^7 + x^4 + x^2
          //         --------------------------------------------------
          //                              x^11 + x^10 + x^7 + x^4 + x^2
          //
          // The remainder is x^11 + x^10 + x^7 + x^4 + x^2
          // Encode it in binary: 110010010100
          // The return value is 0xc94 (1100 1001 0100)
          //
          // Since all coefficients in the polynomials are 1 or 0, we can do the calculation by bit
          // operations. We don't care if cofficients are positive or negative.
          public static function calculateBCHCode(value:int, poly:int):int {
            // If poly is "1 1111 0010 0101" (version info poly), msbSetInPoly is 13. We'll subtract 1
            // from 13 to make it 12.
            var msbSetInPoly:int = findMSBSet(poly);
            value <<= msbSetInPoly - 1;
            // Do the division business using exclusive-or operations.
            while (findMSBSet(value) >= msbSetInPoly) {
              value ^= poly << (findMSBSet(value) - msbSetInPoly);
            }
            // Now the "value" is the remainder (i.e. the BCH code)
            return value;
          }

          // Make bit vector of type information. On success, store the result in "bits" and return true.
          // Encode error correction level and mask pattern. See 8.9 of
          // JISX0510:2004 (p.45) for details.
          public static function makeTypeInfoBits(ecLevel:ErrorCorrectionLevel , maskPattern:int, bits:BitVector ):void
          {
            if (!QRCode.isValidMaskPattern(maskPattern)) {
              throw new WriterException("Invalid mask pattern");
            }
            var typeInfo:int = (ecLevel.getBits() << 3) | maskPattern;
            bits.appendBits(typeInfo, 5);

            var bchCode:int = calculateBCHCode(typeInfo, TYPE_INFO_POLY);
            bits.appendBits(bchCode, 10);

            var maskBits:BitVector = new BitVector();
            maskBits.appendBits(TYPE_INFO_MASK_PATTERN, 15);
            bits.xor(maskBits);

            if (bits.size() != 15) {  // Just in case.
              throw new WriterException("should not happen but we got: " + bits.size());
            }
          }

          // Make bit vector of version information. On success, store the result in "bits" and return true.
          // See 8.10 of JISX0510:2004 (p.45) for details.
          public static function makeVersionInfoBits( version:int, bits:BitVector ):void{
            bits.appendBits(version, 6);
            var bchCode:int = calculateBCHCode(version, VERSION_INFO_POLY);
            bits.appendBits(bchCode, 12);

            if (bits.size() != 18) {  // Just in case.
              throw new WriterException("should not happen but we got: " + bits.size());
            }
          }

          // Check if "value" is empty.
          private static function isEmpty(value:int):Boolean {
            return value == -1;
          }

          // Check if "value" is valid.
          private static function isValidValue(value:int):Boolean {
            return (value == -1 ||  // Empty.
                value == 0 ||  // Light (white).
                value == 1);  // Dark (black).
          }

          private static function embedTimingPatterns(matrix:ByteMatrix ):void 
          {
            // -8 is for skipping position detection patterns (size 7), and two horizontal/vertical
            // separation patterns (size 1). Thus, 8 = 7 + 1.
            for (var i:int = 8; i < matrix.width() - 8; ++i) {
              var bit:int = (i + 1) % 2;
              // Horizontal line.
      			if (!isValidValue(matrix._get(i, 6))) 
      			{
        			throw new WriterException();
      			}
      			if (isEmpty(matrix._get(i, 6))) 
      			{
        			matrix._set(i, 6, bit);
      			}
      			// Vertical line.
      			if (!isValidValue(matrix._get(6, i))) 
      			{
        			throw new WriterException();
      			}
      			if (isEmpty(matrix._get(6, i))) 
      			{
        			matrix._set(6, i, bit);
      			}
            }
          }

          // Embed the lonely dark dot at left bottom corner. JISX0510:2004 (p.46)
          private static function embedDarkDotAtLeftBottomCorner(matrix:ByteMatrix ):void{
            if (matrix._get(8,matrix.height() - 8) == 0) {
              throw new WriterException("MatrixUtil : WriterException 3");
            }
            matrix._set(8,matrix.height()-8, 1);
          }

          private static function embedHorizontalSeparationPattern(xStart:int, yStart:int, matrix:ByteMatrix):void {
            // We know the width and height.
            if (HORIZONTAL_SEPARATION_PATTERN[0].length != 8 || HORIZONTAL_SEPARATION_PATTERN.length != 1) {
              throw new WriterException("Bad horizontal separation pattern");
            }
            for (var x:int = 0; x < 8; ++x) {
              if (!isEmpty(matrix._get( xStart + x,yStart))) {
                throw new WriterException("MatrixUtil : WriterException 4");
              }
              matrix._set(xStart + x, yStart, HORIZONTAL_SEPARATION_PATTERN[0][x]);
            }
          }

          private static function embedVerticalSeparationPattern(xStart:int, yStart:int,matrix:ByteMatrix ):void{
            // We know the width and height.
            if (VERTICAL_SEPARATION_PATTERN[0].length != 1 || VERTICAL_SEPARATION_PATTERN.length != 7) {
              throw new WriterException("Bad vertical separation pattern");
            }
            for (var y:int = 0; y < 7; ++y) {
              if (!isEmpty(matrix._get( xStart,yStart + y))) {
                throw new WriterException("MatrixUtil : WriterException 5");
              }
              matrix._set(xStart, yStart + y, VERTICAL_SEPARATION_PATTERN[y][0]);
            }
          }

          // Note that we cannot unify the function with embedPositionDetectionPattern() despite they are
          // almost identical, since we cannot write a function that takes 2D arrays in different sizes in
          // C/C++. We should live with the fact.
          private static function embedPositionAdjustmentPattern(xStart:int , yStart:int ,matrix:ByteMatrix ):void
          {
            // We know the width and height.
            if (POSITION_ADJUSTMENT_PATTERN[0].length != 5 || POSITION_ADJUSTMENT_PATTERN.length != 5) {
              throw new WriterException("Bad position adjustment");
            }
            for (var y:int = 0; y < 5; ++y) {
              for (var x:int = 0; x < 5; ++x) {
                if (!isEmpty(matrix._get( xStart + x,yStart + y))) {
                  throw new WriterException("MatrixUtil : WriterException 6");
                }
                matrix._set(xStart + x,yStart + y,  POSITION_ADJUSTMENT_PATTERN[y][x]);
              }
            }
          }

          private static function embedPositionDetectionPattern(xStart:int , yStart:int ,matrix:ByteMatrix ):void
          {
            // We know the width and height.
            if (POSITION_DETECTION_PATTERN[0].length != 7 || POSITION_DETECTION_PATTERN.length != 7) {
              throw new WriterException("Bad position detection pattern");
            }
            for (var y:int = 0; y < 7; ++y) {
              for (var x:int = 0; x < 7; ++x) {
                if (!isEmpty(matrix._get(xStart + x,yStart + y))) {
                  throw new WriterException("MatrixUtil : WriterException 7");
                }
                matrix._set(xStart + x,yStart + y,  POSITION_DETECTION_PATTERN[y][x]);
              }
            }
          }

          // Embed position detection patterns and surrounding vertical/horizontal separators.
          private static function embedPositionDetectionPatternsAndSeparators(matrix:ByteMatrix ):void 
          {
            // Embed three big squares at corners.
            var pdpWidth:int = POSITION_DETECTION_PATTERN[0].length;
            // Left top corner.
            embedPositionDetectionPattern(0, 0, matrix);
            // Right top corner.
            embedPositionDetectionPattern(matrix.width() - pdpWidth, 0, matrix);
            // Left bottom corner.
            embedPositionDetectionPattern(0, matrix.width() - pdpWidth, matrix);

            // Embed horizontal separation patterns around the squares.
            var hspWidth:int = HORIZONTAL_SEPARATION_PATTERN[0].length;
            // Left top corner.
            embedHorizontalSeparationPattern(0, hspWidth - 1, matrix);
            // Right top corner.
            embedHorizontalSeparationPattern(matrix.width() - hspWidth,
                hspWidth - 1, matrix);
            // Left bottom corner.
            embedHorizontalSeparationPattern(0, matrix.width() - hspWidth, matrix);

            // Embed vertical separation patterns around the squares.
            var vspSize:int = VERTICAL_SEPARATION_PATTERN.length;
            // Left top corner.
            embedVerticalSeparationPattern(vspSize, 0, matrix);
            // Right top corner.
            embedVerticalSeparationPattern(matrix.height() - vspSize - 1, 0, matrix);
            // Left bottom corner.
            embedVerticalSeparationPattern(vspSize, matrix.height() - vspSize,
                matrix);
          }

          // Embed position adjustment patterns if need be.
          private static function maybeEmbedPositionAdjustmentPatterns( version:int, matrix:ByteMatrix ):void
          {
            if (version < 2) {  // The patterns appear if version >= 2
              return;
            }
            var index:int = version - 1;
            var coordinates:Array = POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[index];
            var numCoordinates:int = POSITION_ADJUSTMENT_PATTERN_COORDINATE_TABLE[index].length;
            for (var i:int = 0; i < numCoordinates; ++i) {
              for (var j:int = 0; j < numCoordinates; ++j) {
                var y:int = coordinates[i];
                var x:int = coordinates[j];
                if (x == -1 || y == -1) {
                  continue;
                }
                // If the cell is unset, we embed the position adjustment pattern here.
                if (isEmpty(matrix._get(x,y))) {
                  // -2 is necessary since the x/y coordinates point to the center of the pattern, not the
                  // left top corner.
                  embedPositionAdjustmentPattern(x - 2, y - 2, matrix);
                }
              }
            }
          }

    
    
    }

}