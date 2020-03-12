// -*- mode:c++; tab-width:2; indent-tabs-mode:nil; c-basic-offset:2 -*-
#pragma once

#include <cstdint>

/*
 *  BarcodeFormat.h
 *  zxing
 *
 *  Copyright 2010 ZXing authors All rights reserved.
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

namespace pping {
    
enum class BarcodeFormat : std::uint8_t {
    None = 0,
    QR_CODE,
    DATA_MATRIX,
    UPC_E,
    UPC_A,
    EAN_8,
    EAN_13,
    CODE_128,
    CODE_39,
    ITF,
    AZTEC_BARCODE,
    PDF417_BARCODE
};

/* if you update the enum, please update the name in BarcodeFormat.cpp */
extern const char *barcodeFormatNames[];
}

