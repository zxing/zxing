package com.google.zxing.qrcode.encoder
{
//	todo : check array datastructures 

	public class MaskUtil
	{
		import com.google.zxing.common.ByteMatrix;
		import com.google.zxing.common.flexdatatypes.IllegalArgumentException;
		
        // Apply mask penalty rule 1 and return the penalty. Find repetitive cells with the same color and
        // give penalty to them. Example: 00000 or 11111.
        public static function applyMaskPenaltyRule1(matrix:ByteMatrix):int
        {
            return applyMaskPenaltyRule1Internal(matrix, true) + applyMaskPenaltyRule1Internal(matrix, false);
        }

        // Apply mask penalty rule 2 and return the penalty. Find 2x2 blocks with the same color and give
        // penalty to them.
        public static function applyMaskPenaltyRule2(matrix:ByteMatrix):int
        {
            var penalty:int = 0;
            var array:Array = matrix.getArray(); //sbyte[][]
            var width:int = matrix.width();
            var height:int = matrix.height();
            for (var y:int = 0; y < height - 1; ++y)
            {
                for (var x:int = 0; x < width - 1; ++x)
                {
                    var value:int = array[y][x];
                    if (value == array[y][x + 1] && value == array[y + 1][x] && value == array[y + 1][x + 1])
                    {
                        penalty += 3;
                    }
                }
            }
            return penalty;
        }

        // Apply mask penalty rule 3 and return the penalty. Find consecutive cells of 00001011101 or
        // 10111010000, and give penalty to them.  If we find patterns like 000010111010000, we give
        // penalties twice (i.e. 40 * 2).
        public static function applyMaskPenaltyRule3(matrix:ByteMatrix):int
        {
            var penalty:int = 0;
            var array:Array = matrix.getArray();
            var width:int = matrix.width();
            var height:int = matrix.height();
            for (var y:int = 0; y < height; ++y)
            {
                for (var x:int = 0; x < width; ++x)
                {
                    // Tried to simplify following conditions but failed.
                    if (x + 6 < width &&
                        array[y][x] == 1 &&
                        array[y][x + 1] == 0 &&
                        array[y][x + 2] == 1 &&
                        array[y][x + 3] == 1 &&
                        array[y][x + 4] == 1 &&
                        array[y][x + 5] == 0 &&
                        array[y][x + 6] == 1 &&
                        ((x + 10 < width &&
                            array[y][x + 7] == 0 &&
                            array[y][x + 8] == 0 &&
                            array[y][x + 9] == 0 &&
                            array[y][x + 10] == 0) ||
                            (x - 4 >= 0 &&
                                array[y][x - 1] == 0 &&
                                array[y][x - 2] == 0 &&
                                array[y][x - 3] == 0 &&
                                array[y][x - 4] == 0)))
                    {
                        penalty += 40;
                    }
                    if (y + 6 < height &&
                        array[y][x] == 1 &&
                        array[y + 1][x] == 0 &&
                        array[y + 2][x] == 1 &&
                        array[y + 3][x] == 1 &&
                        array[y + 4][x] == 1 &&
                        array[y + 5][x] == 0 &&
                        array[y + 6][x] == 1 &&
                        ((y + 10 < height &&
                            array[y + 7][x] == 0 &&
                            array[y + 8][x] == 0 &&
                            array[y + 9][x] == 0 &&
                            array[y + 10][x] == 0) ||
                            (y - 4 >= 0 &&
                                array[y - 1][x] == 0 &&
                                array[y - 2][x] == 0 &&
                                array[y - 3][x] == 0 &&
                                array[y - 4][x] == 0)))
                    {
                        penalty += 40;
                    }
                }
            }
            return penalty;
        }

        // Apply mask penalty rule 4 and return the penalty. Calculate the ratio of dark cells and give
        // penalty if the ratio is far from 50%. It gives 10 penalty for 5% distance. Examples:
        // -   0% => 100
        // -  40% =>  20
        // -  45% =>  10
        // -  50% =>   0
        // -  55% =>  10
        // -  55% =>  20
        // - 100% => 100
        public static function applyMaskPenaltyRule4(matrix:ByteMatrix):int
        {
            var numDarkCells:int = 0;
            var array:Array = matrix.getArray();
            var width:int = matrix.width();
            var height:int = matrix.height();
            for (var y:int = 0; y < height; ++y)
            {
                for (var x:int = 0; x < width; ++x)
                {
                    if (array[y][x] == 1)
                    {
                        numDarkCells += 1;
                    }
                }
            }
            var numTotalCells:int = matrix.height() * matrix.width();
            var darkRatio:Number = numDarkCells / numTotalCells;
            return int(Math.abs(int(darkRatio * 100 - 50)) / 5) * 10;
        }

        // Return the mask bit for "getMaskPattern" at "x" and "y". See 8.8 of JISX0510:2004 for mask
        // pattern conditions.
        public static function getDataMaskBit(maskPattern:int,x:int,y:int):int
        {
            if (!QRCode.isValidMaskPattern(maskPattern))
            {
                throw new IllegalArgumentException("Invalid mask pattern");
            }
            switch (maskPattern)
            {
                case 0:
                    return ((y + x) % 2 == 0) ? 1 : 0;
                case 1:
                    return (y % 2 == 0) ? 1 : 0;
                case 2:
                    return (x % 3 == 0) ? 1 : 0;
                case 3:
                    return ((y + x) % 3 == 0) ? 1 : 0;
                case 4:
                    return ((int(y / 2) + int(x / 3)) % 2 == 0) ? 1 : 0; // bas : fixed here
                case 5:
                    return (((y * x) % 2) + ((y * x) % 3) == 0) ? 1 : 0;
                case 6:
                    return ((((y * x) % 2) + ((y * x) % 3)) % 2 == 0) ? 1 : 0;
                case 7:
                    return ((((y * x) % 3) + ((y + x) % 2)) % 2 == 0) ? 1 : 0;
            }
            throw new IllegalArgumentException("invalid mask pattern: " + maskPattern);
        }

        // Helper function for applyMaskPenaltyRule1. We need this for doing this calculation in both
        // vertical and horizontal orders respectively.
        private static function applyMaskPenaltyRule1Internal(matrix:ByteMatrix,isHorizontal:Boolean):int
        {
            var penalty:int = 0;
            var numSameBitCells:int = 0;
            var prevBit:int = -1;
            // Horizontal mode:
            //   for (int i = 0; i < matrix.height(); ++i) {
            //     for (int j = 0; j < matrix.width(); ++j) {
            //       int bit = matrix._get(i, j);
            // Vertical mode:
            //   for (int i = 0; i < matrix.width(); ++i) {
            //     for (int j = 0; j < matrix.height(); ++j) {
            //       int bit = matrix._get(j, i);
            var iLimit:int = isHorizontal ? matrix.height() : matrix.width();
            var jLimit:int = isHorizontal ? matrix.width() : matrix.height();
            var array:Array = matrix.getArray(); // sbyte[][]
            for (var i:int = 0; i < iLimit; ++i)
            {
                for (var j:int = 0; j < jLimit; ++j)
                {
                    var bit:int = isHorizontal ? array[i][j] : array[j][i];
                    if (bit == prevBit)
                    {
                        numSameBitCells += 1;
                        // Found five repetitive cells with the same color (bit).
                        // We'll give penalty of 3.
                        if (numSameBitCells == 5)
                        {
                            penalty += 3;
                        }
                        else if (numSameBitCells > 5)
                        {
                            // After five repetitive cells, we'll add the penalty one
                            // by one.
                            penalty += 1;
                        }
                    }
                    else
                    {
                        numSameBitCells = 1;  // Include the cell itself.
                        prevBit = bit;
                    }
                }
                numSameBitCells = 0;  // Clear at each row/column.
            }
            return penalty;
        }

	}
}