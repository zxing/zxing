// -*- mode:objc; c-basic-offset:2; indent-tabs-mode:nil -*-
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

#include <ZXing/ZXCapture.h>

#if !TARGET_IPHONE_SIMULATOR
#include <ZXing/ZXCGImageLuminanceSource.h>
#include <ZXing/ZXHybridBinarizer.h>
#include <ZXing/ZXBinaryBitmap.h>
#include <ZXing/ZXQRCodeReader.h>
#include <ZXing/ZXDecodeHints.h>
#include <ZXing/ZXResult.h>
#include <ZXing/ZXReaderException.h>
#include <ZXing/ZXIllegalArgumentException.h>

#if TARGET_OS_EMBEDDED || TARGET_IPHONE_SIMULATOR
#define ZXCaptureDevice AVCaptureDevice
#define ZXCaptureOutput AVCaptureOutput
#define ZXMediaTypeVideo AVMediaTypeVideo
#define ZXCaptureConnection AVCaptureConnection
#else
#define ZXCaptureOutput QTCaptureOutput
#define ZXCaptureConnection QTCaptureConnection
#define ZXCaptureDevice QTCaptureDevice
#define ZXMediaTypeVideo QTMediaTypeVideo
#endif

@implementation ZXCapture

@synthesize delegate;
@synthesize transform;
@synthesize captureToFilename;

- (ZXCapture*)init {
  if ((self = [super init])) {
    on_screen = running = NO;
    reported_width = 0;
    reported_height = 0;
    width = 1920;
    height = 1080;
    hard_stop = false;
    device = -1;
    order_in_skip = 0;
    order_out_skip = 0;
    transform = CGAffineTransformIdentity;
    ZXQT({
        transform.a = -1;
      });
  }
  return self;
}

- (void)order_skip {
  order_out_skip = order_in_skip = 1;
}

- (ZXCaptureDevice*)device {
  ZXCaptureDevice* zxd = nil;

#if ZXAV(1)+0
  NSArray* devices = 
    [ZXCaptureDevice
        ZXAV(devicesWithMediaType:)
      ZXQT(inputDevicesWithMediaType:) ZXMediaTypeVideo];

  if ([devices count] > 0) {
    if (device == -1) {
      AVCaptureDevicePosition position = AVCaptureDevicePositionBack;
      if (camera == self.front) {
        position = AVCaptureDevicePositionFront;
      }

      for(unsigned int i=0; i < [devices count]; ++i) {
        ZXCaptureDevice* dev = [devices objectAtIndex:i];
        if (dev.position == position) {
          device = i;
          zxd = dev;
          break;
        }
      }
    }
    
    if (!zxd && device != -1) {
      zxd = [devices objectAtIndex:device];
    }
  }
#endif

  if (!zxd) {
    zxd = 
      [ZXCaptureDevice
          ZXAV(defaultDeviceWithMediaType:)
        ZXQT(defaultInputDeviceWithMediaType:) ZXMediaTypeVideo];
  }

  return zxd;
}

- (void)replaceInput {
  if (session && input) {
    [session removeInput:input];
    [input release];
    input = nil;
  }

  ZXCaptureDevice* zxd = [self device];
  ZXQT([zxd open:nil]);

  if (zxd) {
    input =
      [ZXCaptureDeviceInput deviceInputWithDevice:zxd
                                       ZXAV(error:nil)];
    [input retain];
  }
  
  if (input) {
    [session addInput:input ZXQT(error:nil)];
  }
}

- (ZXCaptureSession*)session {
  if (session == 0) {
    session = [[ZXCaptureSession alloc] init];
    ZXAV({session.sessionPreset = AVCaptureSessionPresetMedium;});
    [self replaceInput];
  }
  return session;
}

- (void)stop {
  // NSLog(@"stop");

  if (!running) {
    return;
  }

  if (true ZXAV(&& self.session.running)) {
    // NSLog(@"stop running");
    [self.session stopRunning];
  } else {
    // NSLog(@"already stopped");
  }
  running = false;
}

- (void)setOutputAttributes {
    NSString* key = (NSString*)kCVPixelBufferPixelFormatTypeKey; 
    NSNumber* value = [NSNumber numberWithUnsignedInt:kCVPixelFormatType_32BGRA]; 
    NSMutableDictionary* attributes =
      [NSMutableDictionary dictionaryWithObject:value forKey:key]; 
    key = (NSString*)kCVPixelBufferWidthKey;
    value = [NSNumber numberWithUnsignedInt:width]; 
    [attributes setObject:value forKey:key]; 
    key = (NSString*)kCVPixelBufferHeightKey;
    value = [NSNumber numberWithUnsignedInt:height];
    [attributes setObject:value forKey:key]; 
    [output ZXQT(setPixelBufferAttributes:)ZXAV(setVideoSettings:)attributes];
}

- (ZXCaptureVideoOutput*)output {
  if (!output) {
    output = [[ZXCaptureVideoOutput alloc] init];
    [self setOutputAttributes];
    [output ZXQT(setAutomaticallyDropsLateVideoFrames:)
                ZXAV(setAlwaysDiscardsLateVideoFrames:)YES];
    [output ZXQT(setDelegate:)ZXAV(setSampleBufferDelegate:)self
                  ZXAV(queue:dispatch_get_main_queue())];
    [self.session addOutput:output ZXQT(error:nil)];
  }
  return output;
}

- (void)start {
  // NSLog(@"start %@ %d %@ %@", self.session, running, output, delegate);

  if (hard_stop) {
    return;
  }

  if (delegate || luminance || binary) {
    // for side effects
    [self output];
  }
    
  if (false ZXAV(|| self.session.running)) {
    // NSLog(@"already running");
  } else {

    static int i = 0;
    if (++i == -2) {
      abort();
    }

    // NSLog(@"start running");
    [self.session startRunning];
  }
  running = true;
}

- (void)start_stop {
  // NSLog(@"ss %d %@ %d %@ %@ %@", running, delegate, on_screen, output, luminanceLayer, binary);
  if ((!running && (delegate || on_screen)) ||
      (!output &&
       (delegate ||
        (on_screen && (luminance || binary))))) {
    [self start];
  }
  if (running && !delegate && !on_screen) {
    [self stop];
  }
}

- (void)setDelegate:(id<ZXCaptureDelegate>)_delegate {
  delegate = _delegate;
  if (delegate) {
    hard_stop = false;
  }
  [self start_stop];
}

- (void)hard_stop {
  hard_stop = true;
  if (running) {
    [self stop];
  }
}

- (void)setLuminance:(BOOL)on {
  if (on && !luminance) {
    [luminance release];
    luminance = [[CALayer layer] retain];
  } else if (!on && luminance) {
    [luminance release];
    luminance = nil;
  }
}

- (CALayer*)luminance {
  return luminance;
}

- (void)setBinary:(BOOL)on {
  if (on && !binary) {
    [binary release];
    binary = [[CALayer layer] retain];
  } else if (!on && binary) {
    [binary release];
    binary = nil;
  }
}

- (CALayer*)binary {
  return binary;
}

- (CALayer*)layer {
  if (!layer) {
    layer = [[ZXCaptureVideoPreviewLayer alloc] initWithSession:self.session];

    ZXAV(layer.videoGravity = AVLayerVideoGravityResizeAspect);
    ZXAV(layer.videoGravity = AVLayerVideoGravityResizeAspectFill);
    
    [layer setAffineTransform:transform];
    layer.delegate = self;

    ZXQT({
      ProcessSerialNumber psn;
      GetCurrentProcess(&psn);
      TransformProcessType(&psn, 1);
    });
  }
  return layer;
}

- (void)runActionForKey:(NSString *)key
                 object:(id)anObject
              arguments:(NSDictionary *)dict {
  // NSLog(@" rAFK %@ %@ %@", key, anObject, dict); 
  (void)anObject;
  (void)dict;
  if ([key isEqualToString:kCAOnOrderIn]) {
    
    if (order_in_skip) {
      --order_in_skip;
      // NSLog(@"order in skip");
      return;
    }

    // NSLog(@"order in");

    on_screen = true;
    if (luminance && luminance.superlayer != layer) {
      // [layer addSublayer:luminance];
    }
    if (binary && binary.superlayer != layer) {
      // [layer addSublayer:binary];
    }
    [self start_stop];
  } else if ([key isEqualToString:kCAOnOrderOut]) {
    if (order_out_skip) {
      --order_out_skip;
      // NSLog(@"order out skip");
      return;
    }

    on_screen = false;
    // NSLog(@"order out");
    [self start_stop];
  }
}

- (id<CAAction>)actionForLayer:(CALayer*)_layer forKey:(NSString*)event {
  (void)_layer;

  // NSLog(@"layer event %@", event);

  // never animate
  [CATransaction setValue:[NSNumber numberWithFloat:0.0f]
                   forKey:kCATransactionAnimationDuration];

  // NSLog(@"afl %@ %@", _layer, event);
  if ([event isEqualToString:kCAOnOrderIn]
      || [event isEqualToString:kCAOnOrderOut]
      // || ([event isEqualToString:@"bounds"] && (binary || luminance))
      // || ([event isEqualToString:@"onLayout"] && (binary || luminance))
    ) {
    return self;
  } else if ([event isEqualToString:@"contents"] ) {
  } else if ([event isEqualToString:@"sublayers"] ) {
  } else if ([event isEqualToString:@"onLayout"] ) {
  } else if ([event isEqualToString:@"position"] ) {
  } else if ([event isEqualToString:@"bounds"] ) {
  } else if ([event isEqualToString:@"layoutManager"] ) {
  } else if ([event isEqualToString:@"transform"] ) {
  } else {
    NSLog(@"afl %@ %@", _layer, event);
  }
  return nil;
}

- (void)dealloc {
  [captureToFilename release];
  [binary release];
  [luminance release];
  [output release];
  [input release];
  [layer release];
  [session release];
  [super dealloc];
}

- (void)captureOutput:(ZXCaptureOutput*)captureOutput
ZXQT(didOutputVideoFrame:(CVImageBufferRef)videoFrame
     withSampleBuffer:(QTSampleBuffer*)sampleBuffer)
ZXAV(didOutputSampleBuffer:(CMSampleBufferRef)sampleBuffer)
       fromConnection:(ZXCaptureConnection*)connection {
  
  if (!captureToFilename && !luminance && !binary && !delegate) {
    // NSLog(@"skipping capture");
    return;
  }

  // NSLog(@"received frame");

  ZXAV(CVImageBufferRef videoFrame = CMSampleBufferGetImageBuffer(sampleBuffer));

  // NSLog(@"%d %d", CVPixelBufferGetWidth(videoFrame), CVPixelBufferGetHeight(videoFrame));
  // NSLog(@"delegate %@", delegate);

  ZXQT({
  if (!reported_width || !reported_height) {
    NSSize size = 
      [[[[input.device.formatDescriptions objectAtIndex:0]
          formatDescriptionAttributes] objectForKey:@"videoEncodedPixelsSize"] sizeValue];
    width = size.width;
    height = size.height;
    [self performSelectorOnMainThread:@selector(setOutputAttributes) withObject:nil waitUntilDone:NO];
    reported_width = size.width;
    reported_height = size.height;
    [delegate captureSize:self
                    width:[NSNumber numberWithFloat:size.width]
                   height:[NSNumber numberWithFloat:size.height]];
  }});

  (void)sampleBuffer;

  NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
  
  (void)captureOutput;
  (void)connection;

#if !TARGET_OS_EMBEDDED
  // The routines don't exist in iOS. There are alternatives, but a good
  // solution would have to figure out a reasonable path and might be
  // better as a post to url

  if (captureToFilename) {
    CGImageRef image = 
      [ZXCGImageLuminanceSource createImageFromBuffer:videoFrame];
    NSURL* url = [NSURL fileURLWithPath:captureToFilename];
    CGImageDestinationRef dest =
      CGImageDestinationCreateWithURL((CFURLRef)url, kUTTypePNG, 1, nil);
    CGImageDestinationAddImage(dest, image, nil);
    CGImageDestinationFinalize(dest);
    CGImageRelease(image);
    CFRelease(dest);
    self.captureToFilename = nil;
  }
#endif

  ZXCGImageLuminanceSource* source
    = [[[ZXCGImageLuminanceSource alloc]
         initWithBuffer:videoFrame]
        autorelease];

  if (luminance) {
    CGImageRef image = source.image;
    CGImageRetain(image);
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0), dispatch_get_main_queue(), ^{
        luminance.contents = (id)image;
        CGImageRelease(image);
      });
  }

  if (binary || delegate) {

    // compiler issue?
    ZXHybridBinarizer* binarizer = [ZXHybridBinarizer alloc];
    [[binarizer initWithSource:source] autorelease];

    if (binary) {
      CGImageRef image = binarizer.createImage;
      dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0), dispatch_get_main_queue(), ^{
          binary.contents = (id)image;
          CGImageRelease(image);
        });
    }

    if (delegate) {

      ZXDecodeHints* hints = [[[ZXDecodeHints alloc] init] autorelease];
      ZXBinaryBitmap* bitmap = 
        [[[ZXBinaryBitmap alloc] initWithBinarizer:binarizer] autorelease];

      @try {
        ZXQRCodeReader* reader = [[[ZXQRCodeReader alloc] init] autorelease];
        // NSLog(@"started decode");
        ZXResult* result = [reader decode:bitmap hints:hints];
        // NSLog(@"finished decode");
        [delegate captureResult:self result:result];
      } @catch (ZXReaderException* rex) {
        if (![rex.reason isEqualToString:@"Could not find three finder patterns"]) {
          // NSLog(@"failed to decode, caught ReaderException '%@'", rex.reason);
        }
      } @catch (ZXIllegalArgumentException* iex) {
        // NSLog(@"failed to decode, caught IllegalArgumentException '%@'", iex.reason);
      } @catch (id ue) {
        NSLog(@"Caught unknown exception: %@", ue);
      }
    }
    // NSLog(@"finished frame");
  }

  [pool drain];
}

- (BOOL)hasFront {
  NSArray* devices = 
    [ZXCaptureDevice
        ZXAV(devicesWithMediaType:)
      ZXQT(inputDevicesWithMediaType:) ZXMediaTypeVideo];
  return [devices count] > 1;
}

- (BOOL)hasBack {
  NSArray* devices = 
    [ZXCaptureDevice
        ZXAV(devicesWithMediaType:)
      ZXQT(inputDevicesWithMediaType:) ZXMediaTypeVideo];
  return [devices count] > 0;
}

- (BOOL)hasTorch {
  if ([self device]) {
    return false ZXAV(|| [self device].hasTorch);
  } else {
    return NO;
  }
}

- (int)front {
  return 0;
}

- (int)back {
  return 1;
}

- (int)camera {
  return camera;
}

- (BOOL)torch {
  return torch;
}

- (void)setCamera:(int)camera_ {
  if (camera  != camera_) {
    camera = camera_;
    device = -1;
    if (running) {
      [self replaceInput];
    }
  }
}

- (void)setTorch:(BOOL)torch_ {
  (void)torch_;
  ZXAV({
      [input.device lockForConfiguration:nil];
      switch(input.device.torchMode) {
      case AVCaptureTorchModeOff:
      case AVCaptureTorchModeAuto:
      default:
        input.device.torchMode = AVCaptureTorchModeOn;
        break;
      case AVCaptureTorchModeOn:
        input.device.torchMode = AVCaptureTorchModeOff;
        break;
      }
      [input.device unlockForConfiguration];
    });
}

- (void)setTransform:(CGAffineTransform)transform_ {
  transform = transform_;
  [layer setAffineTransform:transform];
}

@end

#else

@implementation ZXCapture

@synthesize delegate;
@synthesize transform;
@synthesize captureToFilename;

- (id)init {
  if ((self = [super init])) {
    [self release];
  }
  return 0;
}

- (CALayer*)layer {
  return 0;
}

- (CALayer*)luminance {
  return 0;
}

- (CALayer*)binary {
  return 0;
}

- (void)setLuminance:(BOOL)on {}
- (void)setBinary:(BOOL)on {}

- (void)hard_stop {
}

- (BOOL)hasFront {
  return YES;
}

- (BOOL)hasBack {
  return NO;
}

- (BOOL)hasTorch {
  return NO;
}

- (int)front {
  return 0;
}

- (int)back {
  return 1;
}

- (int)camera {
  return self.front;
}

- (BOOL)torch {
  return NO;
}

- (void)setCamera:(int)camera_ {}
- (void)setTorch:(BOOL)torch {}
- (void)order_skip {}
- (void)start {}
- (void)stop {}
- (void*)output {return 0;}

@end

#endif
