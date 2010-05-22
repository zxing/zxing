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
#include <zxing/common/GlobalHistogramBinarizer.h>
#include "GrayBytesMonochromeBitmapSource.h"

using namespace zxing;

@implementation Decoder

@synthesize image;
@synthesize cropRect;
@synthesize subsetImage;
@synthesize subsetData;
@synthesize subsetWidth;
@synthesize subsetHeight;
@synthesize subsetBytesPerRow;
@synthesize delegate;

- (void)willDecodeImage {
  if ([self.delegate respondsToSelector:@selector(decoder:willDecodeImage:usingSubset:)]) {
    [self.delegate decoder:self willDecodeImage:self.image usingSubset:self.subsetImage];
  }
}

- (void)progressDecodingImage:(NSString *)progress {
  if ([self.delegate respondsToSelector:@selector(decoder:decodingImage:usingSubset:progress:)]) {
    [self.delegate decoder:self decodingImage:self.image usingSubset:self.subsetImage progress:progress];
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

#define SUBSET_SIZE 320.0
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

- (void)decode:(id)arg {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  { 

    NSSet *formatReaders = [FormatReader formatReaders];
    
    Ref<LuminanceSource> source (new GrayBytesMonochromeBitmapSource(subsetData, subsetWidth, subsetHeight, subsetBytesPerRow));
    
    Ref<Binarizer> binarizer (new GlobalHistogramBinarizer(source));
    Ref<BinaryBitmap> grayImage (new BinaryBitmap(binarizer));
#ifdef DEBUG
    NSLog(@"created GrayBytesMonochromeBitmapSource", subsetWidth, subsetHeight);
    NSLog(@"grayImage count = %d", grayImage->count());
#endif
    
    TwoDDecoderResult *decoderResult = nil;
    
#ifdef TRY_ROTATIONS
    for (int i = 0; !decoderResult && i < 4; i++) {
#endif
      for (FormatReader *reader in formatReaders) {
        try {
  #ifdef DEBUG
          NSLog(@"decoding gray image");
  #endif
          Ref<Result> result([reader decode:grayImage]);
  #ifdef DEBUG
          NSLog(@"gray image decoded");
  #endif
          
          Ref<String> resultText(result->getText());
          const char *cString = resultText->getText().c_str();
          const std::vector<Ref<ResultPoint> > &resultPoints = result->getResultPoints();
          NSMutableArray *points = 
            [NSMutableArray arrayWithCapacity:resultPoints.size()];
          
          for (size_t i = 0; i < resultPoints.size(); i++) {
            const Ref<ResultPoint> &rp = resultPoints[i];
            CGPoint p = CGPointMake(rp->getX(), rp->getY());
            [points addObject:[NSValue valueWithCGPoint:p]];
          }
          
          NSString *resultString = [NSString stringWithCString:cString
                                encoding:NSUTF8StringEncoding];
          
          decoderResult = [[TwoDDecoderResult resultWithText:resultString
                                                     points:points] retain];
        } catch (ReaderException &rex) {
          NSLog(@"failed to decode, caught ReaderException '%s'",
              rex.what());
        } catch (IllegalArgumentException &iex) {
          NSLog(@"failed to decode, caught IllegalArgumentException '%s'", 
              iex.what());
        } catch (...) {
          NSLog(@"Caught unknown exception!");
        }
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
  [pool drain];
#ifdef DEBUG
  NSLog(@"finished decoding.");
#endif
  
  // if this is not the main thread, then we end it
  if (![NSThread isMainThread]) {
    [NSThread exit];
  }
}

- (void) decodeImage:(UIImage *)i {
  [self decodeImage:i cropRect:CGRectMake(0.0f, 0.0f, i.size.width, i.size.height)];
}

- (void) decodeImage:(UIImage *)i cropRect:(CGRect)cr {
  self.image = i;
  self.cropRect = cr;
  
  [self prepareSubset];
  [self willDecodeImage];
  [self performSelectorOnMainThread:@selector(progressDecodingImage:)
               withObject:NSLocalizedString(@"Decoder MessageWhileDecoding", @"Decoding ...")
            waitUntilDone:NO];  
  
  [NSThread detachNewThreadSelector:@selector(decode:) 
               toTarget:self 
               withObject:nil];
}

- (void) dealloc {
  [image release];
  [subsetImage release];
  if (subsetData) free(subsetData);
  [super dealloc];
}

@end
