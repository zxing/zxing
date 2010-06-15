//
//  DataMatrixReader.mm
//  ZXingWidget
//
//  Created by Romain Pechayre on 6/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "DataMatrixReader.h"
#import <zxing/datamatrix/DataMatrixReader.h>

@implementation DataMatrixReader


- (id) init {
  zxing::datamatrix::DataMatrixReader *reader = new   zxing::datamatrix::DataMatrixReader();
  return [super initWithReader:reader];
}
@end
