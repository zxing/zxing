// -*- Mode: ObjC; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*-
/*
 * Copyright 2008-2012 ZXing authors
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

@interface Decoder ()
@property(nonatomic, retain) UIImage *subsetImage;
@end

@implementation Decoder

@synthesize image;
@synthesize subsetImage;
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
  if (!self) return;
  if ([delegate respondsToSelector:@selector(decoder:failedToDecodeImage:usingSubset:reason:)]) {
    [delegate decoder:self failedToDecodeImage:self.image usingSubset:self.subsetImage reason:reason];
  }
}

- (void)resultPointCallback:(CGPoint)point {
  if ([self.delegate respondsToSelector:@selector(decoder:foundPossibleResultPoint:)]) {
    [self.delegate decoder:self foundPossibleResultPoint:point];
  }
}

#define SUBSET_SIZE 360

- (ArrayRef<char>) prepareSubset {
  CGSize size = [image size];
#if ZXING_DEBUG
  NSLog(@"decoding: image is (%.1f x %.1f), cropRect is (%.1f,%.1f)x(%.1f,%.1f)", size.width, size.height,
        cropRect.origin.x, cropRect.origin.y, cropRect.size.width, cropRect.size.height);
#endif
  float scale = fminf(1.0f, fmaxf(SUBSET_SIZE / cropRect.size.width, SUBSET_SIZE / cropRect.size.height));
  CGPoint offset = CGPointMake(-cropRect.origin.x, -cropRect.origin.y);
#if ZXING_DEBUG
  NSLog(@"  offset = (%.1f, %.1f), scale = %.3f", offset.x, offset.y, scale);
#endif
  
  subsetWidth = cropRect.size.width * scale;
  subsetHeight = cropRect.size.height * scale;
  
  subsetBytesPerRow = ((subsetWidth + 0xf) >> 4) << 4;
#if ZXING_DEBUG
  NSLog(@"decoding: image to decode is (%lu x %lu) (%lu bytes/row)", subsetWidth, subsetHeight, subsetBytesPerRow);
#endif
  
  ArrayRef<char> subsetData (subsetBytesPerRow * subsetHeight);
#if ZXING_DEBUG
  NSLog(@"allocated %lu bytes of memory", subsetBytesPerRow * subsetHeight);
#endif
  
  CGColorSpaceRef grayColorSpace = CGColorSpaceCreateDeviceGray();
  
  CGContextRef ctx = 
    CGBitmapContextCreate(&subsetData->values()[0], subsetWidth, subsetHeight, 
                          8, subsetBytesPerRow, grayColorSpace, 
                          kCGImageAlphaNone);
  CGColorSpaceRelease(grayColorSpace);
  CGContextSetInterpolationQuality(ctx, kCGInterpolationNone);
  CGContextSetAllowsAntialiasing(ctx, false);
  // adjust the coordinate system
  CGContextTranslateCTM(ctx, 0.0, subsetHeight);
  CGContextScaleCTM(ctx, 1.0, -1.0);  
  
#if ZXING_DEBUG
  NSLog(@"created %lux%lu bitmap context", subsetWidth, subsetHeight);
#endif
  
  UIGraphicsPushContext(ctx);
  CGRect rect = CGRectMake(offset.x * scale, offset.y * scale, scale * size.width, scale * size.height);
#if ZXING_DEBUG
  NSLog(@"rect for image = (%.1f,%.1f)x(%.1f,%.1f)", rect.origin.x, rect.origin.y, rect.size.width, rect.size.height);
#endif
  [image drawInRect:rect];
  UIGraphicsPopContext();
  
#if ZXING_DEBUG
  NSLog(@"drew image into %lu(%lu)x%lu  bitmap context", subsetWidth, subsetBytesPerRow, subsetHeight);
#endif
  CGContextFlush(ctx);
#if ZXING_DEBUG
  NSLog(@"flushed context");
#endif
    
  CGImageRef subsetImageRef = CGBitmapContextCreateImage(ctx);
#if ZXING_DEBUG
  NSLog(@"created CGImage from context");
#endif
  
  self.subsetImage = [UIImage imageWithCGImage:subsetImageRef];
  CGImageRelease(subsetImageRef);
  
  CGContextRelease(ctx);
#if ZXING_DEBUG
  NSLog(@"released context");  
#endif
  return subsetData;
}  

- (BOOL)decode:(ArrayRef<char>)subsetData {
  NSAutoreleasePool* mainpool = [[NSAutoreleasePool alloc] init];
  TwoDDecoderResult *decoderResult = nil;
  BOOL returnCode = NO;
  { 
    //NSSet *formatReaders = [FormatReader formatReaders];
    NSSet *formatReaders = self.readers;
    Ref<LuminanceSource> source 
      (new GreyscaleLuminanceSource(ArrayRef<char>(subsetData), subsetBytesPerRow, subsetHeight, 0, 0, subsetWidth, subsetHeight));
    subsetData = 0;

    Ref<Binarizer> binarizer (new HybridBinarizer(source));
    source = 0;
    Ref<BinaryBitmap> grayImage (new BinaryBitmap(binarizer));
    binarizer = 0;
    
#ifdef TRY_ROTATIONS
    for (int i = 0; !decoderResult && i < 4; i++) {
#endif
      for (FormatReader *reader in formatReaders) {
        NSAutoreleasePool *secondarypool = [[NSAutoreleasePool alloc] init];
        NSMutableArray *points = nil;
        NSString *resultString = nil;
        try {
#if ZXING_DEBUG
          NSLog(@"decoding gray image");
#endif  
          ResultPointCallback* callback_pointer(new ZXingWidgetControllerCallback(self));
          Ref<ResultPointCallback> callback(callback_pointer);
          Ref<Result> result([reader decode:grayImage andCallback:callback]);
#if ZXING_DEBUG
          NSLog(@"gray image decoded");
#endif
          
          Ref<String> resultText(result->getText());
          const char *cString = resultText->getText().c_str();
          const ArrayRef<Ref<ResultPoint> > &resultPoints = result->getResultPoints();
          points = [[NSMutableArray alloc ] initWithCapacity:resultPoints->size()];
          
          for (int i = 0; i < resultPoints->size(); i++) {
            const Ref<ResultPoint> &rp = resultPoints[i];
            CGPoint p = CGPointMake(rp->getX(), rp->getY());
            [points addObject:[NSValue valueWithCGPoint:p]];
          }
          
          resultString = [[NSString alloc] initWithCString:cString encoding:NSUTF8StringEncoding];
          if (decoderResult) [decoderResult release];
          decoderResult = [[TwoDDecoderResult alloc] initWithText:resultString points:points];
        } catch (ReaderException &rex) {
#if ZXING_DEBUG
          NSLog(@"failed to decode, caught ReaderException '%s'",
                rex.what());
#endif
        } catch (IllegalArgumentException &iex) {
#if ZXING_DEBUG
          NSLog(@"failed to decode, caught IllegalArgumentException '%s'", 
                iex.what());
#endif
        } catch (...) {
          NSLog(@"Caught unknown exception!");
        }
        [resultString release];
        [points release];
        [secondarypool release];
      }
      
#ifdef TRY_ROTATIONS
      if (!decoderResult) {
#if ZXING_DEBUG
        NSLog(@"rotating gray image");
#endif
        grayImage = grayImage->rotateCounterClockwise();
#if ZXING_DEBUG
        NSLog(@"gray image rotated");
#endif
      }
    }
#endif
	  
    if (decoderResult) {
      [self performSelectorOnMainThread:@selector(didDecodeImage:)
                             withObject:[decoderResult copy]
                          waitUntilDone:NO];
      [decoderResult release];
      returnCode = YES;
    } else {
      [self performSelectorOnMainThread:@selector(failedToDecodeImage:)
                             withObject:NSLocalizedString(@"Decoder BarcodeDetectionFailure", @"No barcode detected.")
                          waitUntilDone:NO];
    }
  }
  
  
#if ZXING_DEBUG
  NSLog(@"finished decoding.");
#endif
  [mainpool release];

  return returnCode;
}

- (BOOL) decodeImage:(UIImage *)i {
  return [self decodeImage:i cropRect:CGRectMake(0.0f, 0.0f, i.size.width, i.size.height)];
}

- (BOOL) decodeImage:(UIImage *)i cropRect:(CGRect)cr {
  self.image = i;
  cropRect = cr;
  ArrayRef<char> subsetData = [self prepareSubset];
  [self willDecodeImage];
  return [self decode:subsetData];
}

- (void) dealloc {
  delegate = nil;
  [image release];
  [subsetImage release];
  [readers release];
  [super dealloc];
}

@end
