//
//  MultiFormatOneDReader.mm
//  ZXingWidget
//
//  Created by Romain Pechayre on 6/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "MultiFormatOneDReader.h"
#import <zxing/oned/MultiFormatOneDReader.h>
#include <zxing/DecodeHints.h>
@implementation MultiFormatOneDReader


- (id) init {
  zxing::oned::MultiFormatOneDReader *reader = new   zxing::oned::MultiFormatOneDReader(zxing::DecodeHints::DEFAULT_HINT);
  return [super initWithReader:reader];
}
@end
