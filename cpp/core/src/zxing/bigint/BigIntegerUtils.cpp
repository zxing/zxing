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

#include "BigIntegerUtils.h"

#include "BigUnsignedInABase.h"       // for BigUnsignedInABase, BigUnsignedInABase::Base
#include "zxing/bigint/BigInteger.h"  // for BigInteger, BigInteger::Sign::negative

namespace bigInteger {

std::string bigUnsignedToString(const BigUnsigned &x) {
    return std::string(BigUnsignedInABase(x, 10));
}

std::string bigIntegerToString(const BigInteger &x) {
    return (x.getSign() == BigInteger::negative)
        ? (std::string("-") + bigUnsignedToString(x.getMagnitude()))
        : (bigUnsignedToString(x.getMagnitude()));
}

BigUnsigned stringToBigUnsigned(const std::string &s) {
    return BigUnsigned(BigUnsignedInABase(s, 10));
}

BigInteger stringToBigInteger(const std::string &s) {
    // Recognize a sign followed by a BigUnsigned.
    return (s[0] == '-') ? BigInteger(stringToBigUnsigned(s.substr(1, s.length() - 1)), BigInteger::negative)
        : (s[0] == '+') ? BigInteger(stringToBigUnsigned(s.substr(1, s.length() - 1)))
        : BigInteger(stringToBigUnsigned(s));
}

}
