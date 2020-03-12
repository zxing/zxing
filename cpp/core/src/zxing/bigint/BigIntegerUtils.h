/*
 *  Author: Matt McCutchen, https://mattmccutchen.net/bigint
 *  Copyright 2008/2010/2012 ZXing authors All rights reserved.
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
 *
 * The BigInteger library was included in the ZXing C++ library by Hartmut
 * Neubauer with the permission of Matt McCutchen because PDF417 uses
 * BigIntegers.
 */

#pragma once

#include <string>

#include "BigInteger.h"
#include "zxing/bigint/BigUnsigned.h"

namespace bigInteger {

/* This file provides:
 * - Convenient std::string <-> BigUnsigned/BigInteger conversion routines */

// std::string conversion routines.  Base 10 only.
std::string bigUnsignedToString(const BigUnsigned &x);
std::string bigIntegerToString(const BigInteger &x);
BigUnsigned stringToBigUnsigned(const std::string &s);
BigInteger stringToBigInteger(const std::string &s);

// Creates a BigInteger from data such as `char's; read below for details.
template <class T>
BigInteger dataToBigInteger(const T* data, BigInteger::Index length, BigInteger::Sign sign);

// BEGIN TEMPLATE DEFINITIONS.

/*
 * Converts binary data to a BigInteger.
 * Pass an array `data', its length, and the desired sign.
 *
 * Elements of `data' may be of any type `T' that has the following
 * two properties (this includes almost all integral types):
 *
 * (1) `sizeof(T)' correctly gives the amount of binary data in one
 * value of `T' and is a factor of `sizeof(Blk)'.
 *
 * (2) When a value of `T' is casted to a `Blk', the low bytes of
 * the result contain the desired binary data.
 */
template <class T>
BigInteger dataToBigInteger(const T* data, BigInteger::Index length, BigInteger::Sign sign) {
    // really ceiling(numBytes / sizeof(BigInteger::Blk))
    unsigned int pieceSizeInBits = 8 * sizeof(T);
    unsigned int piecesPerBlock = sizeof(BigInteger::Blk) / sizeof(T);
    unsigned int numBlocks = (length + piecesPerBlock - 1) / piecesPerBlock;

    // Allocate our block array
    BigInteger::Blk *blocks = new BigInteger::Blk[numBlocks];

    BigInteger::Index blockNum, pieceNum, pieceNumHere;

    // Convert
    for (blockNum = 0, pieceNum = 0; blockNum < numBlocks; blockNum++) {
        BigInteger::Blk curBlock = 0;
        for (pieceNumHere = 0; pieceNumHere < piecesPerBlock && pieceNum < length;
            pieceNumHere++, pieceNum++)
            curBlock |= (BigInteger::Blk(data[pieceNum]) << (pieceSizeInBits * pieceNumHere));
        blocks[blockNum] = curBlock;
    }

    // Create the BigInteger.
    BigInteger x(blocks, numBlocks, sign);

    delete [] blocks;
    return x;
}

}

