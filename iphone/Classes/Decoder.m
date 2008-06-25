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

#include "QRCodeReader.h"
#include "ReaderException.h"
#include "IllegalArgumentException.h"
#include "GrayBytesMonochromeBitmapSource.h"

using namespace qrcode;

@implementation Decoder

@synthesize image;
@synthesize subsetImage;
@synthesize subsetData;
@synthesize subsetWidth;
@synthesize subsetHeight;
@synthesize subsetBytesPerRow;
@synthesize delegate;

- (void)willDecodeImage {
  [self.delegate decoder:self willDecodeImage:self.image];
}

- (void)progressDecodingImage:(NSString *)progress {
  [self.delegate decoder:self 
          decodingImage:self.image 
            usingSubset:self.subsetImage
               progress:progress];
}

- (void)didDecodeImage:(TwoDDecoderResult *)result {
  [self.delegate decoder:self didDecodeImage:self.image usingSubset:self.subsetImage withResult:result];
}

- (void)failedToDecodeImage:(NSString *)reason {
  [self.delegate decoder:self failedToDecodeImage:self.image usingSubset:self.subsetImage reason:reason];
}

- (void) prepareSubset {
  CGImageRef cgImage = self.image.CGImage;
  CGSize size = CGSizeMake(CGImageGetWidth(cgImage), CGImageGetHeight(cgImage));
#ifdef DEBUG
  NSLog(@"decoding: image is (%.1f x %.1f)", size.width, size.height);
#endif
  float scale = min(1.0f, max(0.25f, (float)max(400.0f / size.width, 400.0f / size.height)));
  subsetWidth = size.width * scale;
  subsetHeight = size.height * scale;
  
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
  
#ifdef DEBUG
  NSLog(@"created %dx%d bitmap context", subsetWidth, subsetHeight);
#endif
  CGRect rect = CGRectMake(0, 0, subsetWidth, subsetHeight);
  
  CGContextDrawImage(ctx, rect, cgImage);
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
    QRCodeReader reader;
#ifdef DEBUG
    NSLog(@"created QRCoreReader");
#endif
    
    Ref<MonochromeBitmapSource> grayImage
    (new GrayBytesMonochromeBitmapSource(subsetData, subsetWidth, subsetHeight, subsetBytesPerRow));
#ifdef DEBUG
    NSLog(@"created GrayBytesMonochromeBitmapSource", subsetWidth, subsetHeight);
    NSLog(@"grayImage count = %d", grayImage->count());
#endif
    
    TwoDDecoderResult *decoderResult = nil;
    
#ifdef TRY_ROTATIONS
    for (int i = 0; !decoderResult && i < 4; i++) {
#endif

    try {
#ifdef DEBUG
      NSLog(@"decoding gray image");
#endif
      Ref<Result> result(reader.decode(grayImage));
#ifdef DEBUG
      NSLog(@"gray image decoded");
#endif
      
      Ref<String> resultText(result->getText());
      const char *cString = resultText->getText().c_str();
      ArrayRef<Ref<ResultPoint> > resultPoints = result->getResultPoints();
      NSMutableArray *points = 
        [NSMutableArray arrayWithCapacity:resultPoints->size()];
      
      for (size_t i = 0; i < resultPoints->size(); i++) {
        Ref<ResultPoint> rp(resultPoints[i]);
        CGPoint p = CGPointMake(rp->getX(), rp->getY());
        [points addObject:[NSValue valueWithCGPoint:p]];
      }
      
      NSString *resultString = [NSString stringWithCString:cString
                                        encoding:NSUTF8StringEncoding];
      
      decoderResult = [TwoDDecoderResult resultWithText:resultString
                                             points:points];
    } catch (ReaderException *rex) {
      NSLog(@"failed to decode, caught ReaderException '%s'",
            rex->what());
      delete rex;
    } catch (IllegalArgumentException *iex) {
      NSLog(@"failed to decode, caught IllegalArgumentException '%s'", 
            iex->what());
      delete iex;
    } catch (...) {
      NSLog(@"Caught unknown exception!");
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
    
    if (decoderResult) {
      [self performSelectorOnMainThread:@selector(didDecodeImage:)
                             withObject:decoderResult
                          waitUntilDone:NO];
    } else {
      [self performSelectorOnMainThread:@selector(failedToDecodeImage:)
                             withObject:NSLocalizedString(@"No barcode detected.", @"No barcode detected.")
                          waitUntilDone:NO];
    }

    free(subsetData);
    self.subsetData = NULL;
  }
  [pool release];
#ifdef DEBUG
  NSLog(@"finished decoding.");
#endif
  
  // if this is not the main thread, then we end it
  if (![NSThread isMainThread]) {
    [NSThread exit];
  }
}

- (void) decodeImage:(UIImage *)i {
  CGRect rect = CGRectMake(0.0f, 0.0f, image.size.width, image.size.height);
  [self decodeImage:i cropRectangle:rect];
}

- (void) decodeImage:(UIImage *)i cropRectangle:(CGRect)cropRect {
	self.image = i;
	[self.delegate decoder:self willDecodeImage:i];
  
  [self prepareSubset];
  
  [self performSelectorOnMainThread:@selector(progressDecodingImage:)
                         withObject:@"Decoding ..."
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
