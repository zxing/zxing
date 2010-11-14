//
//  ScannedImageView.m
//  ZXing
//
//  Created by Christian Brunschen on 01/07/2008.
/*
 * Copyright 2008 ZXing authors
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

#import "ScannedImageView.h"
#import <math.h>

@implementation ScannedImageView

- (id)initWithFrame:(CGRect)frame {
	if ((self = [super initWithFrame:frame])) {
    resultPoints = [[NSMutableArray alloc] initWithCapacity:10];
	}
	return self;
}

- (id)initWithCoder:(NSCoder *)decoder {
  if ((self = [super initWithCoder:decoder]) != nil) {
    resultPoints = [[NSMutableArray alloc] initWithCapacity:10];
  }
  return self;
}

- (void)drawRect:(CGRect)rect {
  [super drawRect:rect];
  
  if (image) {
    // draw the image, scaled to fit, top and center
    CGSize imageSize = image.size;
    CGRect bounds = [self bounds];
    double imageScale = fminf(bounds.size.width / imageSize.width,
                              bounds.size.height / imageSize.height);
    double dx = (bounds.size.width - imageSize.width * imageScale) / 2.0;
    double dy = 0.0;
    
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    CGContextSetInterpolationQuality(ctx, kCGInterpolationDefault);
    CGRect imageRect = CGRectMake(dx, dy, 
                                  imageSize.width * imageScale,
                                  imageSize.height * imageScale);
    [image drawInRect:imageRect];
    
    [[UIColor greenColor] set];

    if (resultPoints && [resultPoints count]) {
#define R 4.0
      if ([resultPoints count] == 2) {
        CGPoint p0 = [[resultPoints objectAtIndex:0] CGPointValue];
        CGPoint p1 = [[resultPoints objectAtIndex:1] CGPointValue];
        CGContextMoveToPoint(ctx, dx + p0.x * imageScale, dy + p0.y * imageScale);
        CGContextAddLineToPoint(ctx, dx + p1.x * imageScale, dy + p1.y * imageScale);
        CGContextSetLineWidth(ctx, 4.0);
        CGContextSetLineCap(ctx, kCGLineCapSquare);
        CGContextStrokePath(ctx);
      } else {
        // for each resultPoint, draw it
        for (NSValue *pointValue in resultPoints) {
          CGPoint resultPoint = [pointValue CGPointValue];
          float px = dx + resultPoint.x * imageScale;
          float py = dy + resultPoint.y * imageScale;
          CGContextAddRect(ctx,
                           CGRectMake(px - R, py - R, 2 * R, 2 * R));
        }
        CGContextFillPath(ctx);
      }
      CGContextFlush(ctx);
#undef R
    }
  }
}

- (void) addResultPoint:(CGPoint)p {
  [resultPoints addObject:[NSValue valueWithCGPoint:p]];
  [self setNeedsDisplay];
}

- (void) clearResultPoints {
  [resultPoints removeAllObjects];
}

- (void) setImage:(UIImage *)newImage {
  [newImage retain];
  [image release];
  image = newImage;
  [self clearResultPoints];
  [self setNeedsDisplay];
}

- (UIImage *)image {
  return image;
}


- (void)dealloc {
  [image release];
  [resultPoints release];
	[super dealloc];
}


@end
