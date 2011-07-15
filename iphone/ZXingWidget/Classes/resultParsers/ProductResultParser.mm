//
//  ProductResultParser.m
//  ZXing
//
//  Ported to Objective-C by George Nachman on 7/7/2011.
/*
 * Copyright 2011 ZXing authors
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

#import "ProductResultParser.h"
#import "ProductParsedResult.h"
#import "CBarcodeFormat.h"
#include "../../../../cpp/core/src/zxing/oned/UPCEReader.h"

@implementation ProductResultParser

+ (void)load {
    [ResultParser registerResultParserClass:self];
}

+ (ParsedResult *)parsedResultForString:(NSString *)s format:(BarcodeFormat)format {
    // Treat all UPC and EAN variants as UPCs, in the sense that they are all
    // product barcodes.
    if (format != BarcodeFormat_UPC_E &&
        format != BarcodeFormat_UPC_A &&
        format != BarcodeFormat_EAN_8 &&
        format != BarcodeFormat_EAN_13) {
        return nil;
    }

    // Barcode must be all digits.
    for (unsigned int i = 0; i < [s length]; i++) {
        unichar c = [s characterAtIndex:i];
        if (c < '0' || c > '9') {
            return nil;
        }
    }

    NSString *normalizedProductID;
    // Expand UPC-E for purposes of searching
    if (format == BarcodeFormat_UPC_E) {
        std::string textStr = std::string([s UTF8String]);
        std::string normal = zxing::oned::UPCEReader::convertUPCEtoUPCA(textStr);
        normalizedProductID = [NSString stringWithUTF8String:normal.c_str()];
    } else {
        normalizedProductID = s;
    }

    return [[[ProductParsedResult alloc] initWithProductID:s
                                       normalizedProductID:normalizedProductID]
               autorelease];
}

@end
