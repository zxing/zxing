//
//  MultiFormatUPCEANReader.mm
//  ZXingWidget
//
//  Created by Romain Pechayre on 6/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "MultiFormatUPCEANReader.h"
#import <zxing/oned/MultiFormatUPCEANReader.h>
#include <zxing/DecodeHints.h>

@implementation MultiFormatUPCEANReader

- (id) init {
  zxing::oned::MultiFormatUPCEANReader *reader = new zxing::oned::MultiFormatUPCEANReader(zxing::DecodeHints::DEFAULT_HINT);
  return [super initWithReader:reader];
}
@end
