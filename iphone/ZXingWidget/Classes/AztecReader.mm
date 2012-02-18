//
//  AztecReader.m
//  ZXingWidget
//
//  Created by Lukas Stabe on 08.02.12.
//  Copyright (c) 2012 EOS UPTRADE GmbH. All rights reserved.
//

#import "AztecReader.h"
#import <zxing/aztec/AztecReader.h>

@implementation AztecReader

- (id)init {
    zxing::aztec::AztecReader *reader = new zxing::aztec::AztecReader();
    
    return [super initWithReader:reader];
}

- (zxing::Ref<zxing::Result>)decode:(zxing::Ref<zxing::BinaryBitmap>)grayImage andCallback:(zxing::Ref<zxing::ResultPointCallback>)callback {
    //NSLog(@"no callbacks supported for aztec");
    return [self decode:grayImage];
}

@end
