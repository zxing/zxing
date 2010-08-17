//
//  Decoder.m
//  ZXing
//
//  Created by Christian Brunschen on 31/03/2008.
//
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

#import "Decoder.h"
#import "TwoDDecoderResult.h"
#import "FormatReader.h"

#include <zxing/BinaryBitmap.h>
#include <zxing/ReaderException.h>
#include <zxing/common/IllegalArgumentException.h>
#include <zxing/common/HybridBinarizer.h>
#include <zxing/common/GreyscaleLuminanceSource.h>

using namespace zxing;

class ZXingWidgetControllerCallback : public zxing::ResultPointCallback {
private:
  Decoder* decoder;
public:
  ZXingWidgetControllerCallback(Decoder* _decoder) : decoder(_decoder) {}
  void foundPossibleResultPoint(ResultPoint const& result) {
    CGPoint point;
    point.x = result.getX();
    point.y = result.getY();
    [decoder resultPointCallback:point];
  }
};

@implementation Decoder

@synthesize image;
@synthesize cropRect;
@synthesize subsetImage;
@synthesize subsetData;
@synthesize subsetWidth;
@synthesize subsetHeight;
@synthesize subsetBytesPerRow;
@synthesize delegate;
@synthesize readers;


- (void)willDecodeImage {
  if ([self.delegate respondsToSelector:@selector(decoder:willDecodeImage:usingSubset:)]) {
    [self.delegate decoder:self willDecodeImage:self.image usingSubset:self.subsetImage];
  }
}

- (void)didDecodeImage:(TwoDDecoderResult *)result {
  if ([self.delegate respondsToSelector:@selector(decoder:didDecodeImage:usingSubset:withResult:)]) {
    [self.delegate decoder:self didDecodeImage:self.image usingSubset:self.subsetImage withResult:result];
  }
	
  [result release];
}

- (void)failedToDecodeImage:(NSString *)reason {
  if ([self.delegate respondsToSelector:@selector(decoder:failedToDecodeImage:usingSubset:reason:)]) {
    [self.delegate decoder:self failedToDecodeImage:self.image usingSubset:self.subsetImage reason:reason];
  }
}

- (void)resultPointCallback:(CGPoint)point {
  if ([self.delegate respondsToSelector:@selector(decoder:foundPossibleResultPoint:)]) {
    [self.delegate decoder:self foundPossibleResultPoint:point];
  }
}

#define SUBSET_SIZE 360
- (void) prepareSubset {
  CGSize size = [image size];
#ifdef DEBUG
  NSLog(@"decoding: image is (%.1f x %.1f), cropRect is (%.1f,%.1f)x(%.1f,%.1f)", size.width, size.height,
      cropRect.origin.x, cropRect.origin.y, cropRect.size.width, cropRect.size.height);
#endif
  float scale = fminf(1.0f, fmaxf(SUBSET_SIZE / cropRect.size.width, SUBSET_SIZE / cropRect.size.height));
  CGPoint offset = CGPointMake(-cropRect.origin.x, -cropRect.origin.y);
#ifdef DEBUG
  NSLog(@"  offset = (%.1f, %.1f), scale = %.3f", offset.x, offset.y, scale);
#endif
  
  subsetWidth = cropRect.size.width * scale;
  subsetHeight = cropRect.size.height * scale;
  
  subsetBytesPerRow = ((subsetWidth + 0xf) >> 4) << 4;
#ifdef DEBUG
  NSLog(@"decoding: image to decode is (%d x %d) (%d bytes/row)", subsetWidth, subsetHeight, subsetBytesPerRow);
#endif
  
  subsetData = (unsigned char *)malloc(subsetBytesPerRow * subsetHeight);
#ifdef DEBUG
  NSLog(@"allocated %d bytes of memory", subsetBytesPerRow * subsetHeight);
#endif
  
  CGColorSpaceRef grayColorSpace = CGColorSpaceCreateDeviceGray();
  
  CGContextRef ctx = 
  CGBitmapContextCreate(subsetData, subsetWidth, subsetHeight, 
              8, subsetBytesPerRow, grayColorSpace, 
              kCGImageAlphaNone);
  CGColorSpaceRelease(grayColorSpace);
  CGContextSetInterpolationQuality(ctx, kCGInterpolationNone);
  CGContextSetAllowsAntialiasing(ctx, false);
  // adjust the coordinate system
  CGContextTranslateCTM(ctx, 0.0, subsetHeight);
  CGContextScaleCTM(ctx, 1.0, -1.0);  
  
#ifdef DEBUG
  NSLog(@"created %dx%d bitmap context", subsetWidth, subsetHeight);
#endif
  
  UIGraphicsPushContext(ctx);
  CGRect rect = CGRectMake(offset.x * scale, offset.y * scale, scale * size.width, scale * size.height);
#ifdef DEBUG
  NSLog(@"rect for image = (%.1f,%.1f)x(%.1f,%.1f)", rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
#endif
  [image drawInRect:rect];
  UIGraphicsPopContext();
  
#ifdef DEBUG
  NSLog(@"drew image into %d(%d)x%d  bitmap context", subsetWidth, subsetBytesPerRow, subsetHeight);
#endif
  CGContextFlush(ctx);
#ifdef DEBUG
  NSLog(@"flushed context");
#endif
    
  CGImageRef subsetImageRef = CGBitmapContextCreateImage(ctx);
#ifdef DEBUG
  NSLog(@"created CGImage from context");
#endif
  
  self.subsetImage = [UIImage imageWithCGImage:subsetImageRef];
  CGImageRelease(subsetImageRef);
  
  CGContextRelease(ctx);
#ifdef DEBUG
  NSLog(@"released context");  
#endif
}  

- (BOOL)decode {
  NSAutoreleasePool* mainpool = [[NSAutoreleasePool alloc] init];
  TwoDDecoderResult *decoderResult = nil;
    
  { 
    //NSSet *formatReaders = [FormatReader formatReaders];
    NSSet *formatReaders = self.readers;
    Ref<LuminanceSource> source 
        (new GreyscaleLuminanceSource(subsetData, subsetBytesPerRow, subsetHeight, 0, 0, subsetWidth, subsetHeight));

    Ref<Binarizer> binarizer (new HybridBinarizer(source));
    source = 0;
    Ref<BinaryBitmap> grayImage (new BinaryBitmap(binarizer));
    binarizer = 0;
#ifdef DEBUG
    NSLog(@"created GreyscaleLuminanceSource(%p,%d,%d,%d,%d,%d,%d)",
          subsetData, subsetBytesPerRow, subsetHeight, 0, 0, subsetWidth, subsetHeight);
    NSLog(@"grayImage count = %d", grayImage->count());
#endif
    
#ifdef TRY_ROTATIONS
    for (int i = 0; !decoderResult && i < 4; i++) {
#endif
      for (FormatReader *reader in formatReaders) {
        NSAutoreleasePool *secondarypool = [[NSAutoreleasePool alloc] init];
        try {
  #ifdef DEBUG
          NSLog(@"decoding gray image");
  #endif  
          ResultPointCallback* callback_pointer(new ZXingWidgetControllerCallback(self));
          Ref<ResultPointCallback> callback(callback_pointer);
          Ref<Result> result([reader decode:grayImage andCallback:callback]);
  #ifdef DEBUG
          NSLog(@"gray image decoded");
  #endif
          
          Ref<String> resultText(result->getText());
          const char *cString = resultText->getText().c_str();
          const std::vector<Ref<ResultPoint> > &resultPoints = result->getResultPoints();
          NSMutableArray *points = 
            [[NSMutableArray alloc ] initWithCapacity:resultPoints.size()];
          
          for (size_t i = 0; i < resultPoints.size(); i++) {
            const Ref<ResultPoint> &rp = resultPoints[i];
            CGPoint p = CGPointMake(rp->getX(), rp->getY());
            [points addObject:[NSValue valueWithCGPoint:p]];
          }
          
          NSString *resultString = [NSString stringWithCString:cString
                                                        encoding:NSUTF8StringEncoding];

          decoderResult = [[TwoDDecoderResult resultWithText:resultString points:points] retain];
          [points release];
        } catch (ReaderException &rex) {
          NSLog(@"failed to decode, caught ReaderException '%s'",
              rex.what());
        } catch (IllegalArgumentException &iex) {
          NSLog(@"failed to decode, caught IllegalArgumentException '%s'", 
              iex.what());
        } catch (...) {
          NSLog(@"Caught unknown exception!");
        }
        [secondarypool release];
      }
      
#ifdef TRY_ROTATIONS
      if (!decoderResult) {
#ifdef DEBUG
        NSLog(@"rotating gray image");
#endif
        grayImage = grayImage->rotateCounterClockwise();
#ifdef DEBUG
        NSLog(@"gray image rotated");
#endif
      }
    }
#endif
	  
	free(subsetData);
	self.subsetData = NULL;
	  
        // DONT COMMIT
        // [decoderResult release];
        // decoderResult = nil;
        

    if (decoderResult) {
      [self performSelectorOnMainThread:@selector(didDecodeImage:)
                   withObject:decoderResult
                waitUntilDone:NO];
    } else {
      [self performSelectorOnMainThread:@selector(failedToDecodeImage:)
                   withObject:NSLocalizedString(@"Decoder BarcodeDetectionFailure", @"No barcode detected.")
                waitUntilDone:NO];
    }
  }
  
  
#ifdef DEBUG
  NSLog(@"finished decoding.");
#endif
  [mainpool release];

  return decoderResult == nil ? NO : YES;
}

- (BOOL) decodeImage:(UIImage *)i {
  return [self decodeImage:i cropRect:CGRectMake(0.0f, 0.0f, i.size.width, i.size.height)];
}

- (BOOL) decodeImage:(UIImage *)i cropRect:(CGRect)cr {
  self.image = i;
  self.cropRect = cr;
  [self prepareSubset];
  [self willDecodeImage];
  return [self decode];
}

- (void) dealloc {
  [image release];
  [subsetImage release];
  free(subsetData);
  [readers release];
  [super dealloc];
}

@end
