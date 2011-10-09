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

#import <ZXing/ZXCaptureDelegate.h>

#if !TARGET_IPHONE_SIMULATOR
#if TARGET_OS_EMBEDDED
#include <AVFoundation/AVFoundation.h>
#define ZX(x) x
#define ZXAV(x) x
#define ZXAVC(x) ,x
#define ZXQT(x)
#define ZXCaptureSession AVCaptureSession
#define ZXCaptureVideoPreviewLayer AVCaptureVideoPreviewLayer
#define ZXCaptureDeviceInput AVCaptureDeviceInput
#define ZXCaptureVideoOutput AVCaptureVideoDataOutput
#else
#import <QTKit/QTKit.h>
#define ZX(x) x
#define ZXAV(x)
#define ZXAVC(x)
#define ZXQT(x) x
#define ZXCaptureSession QTCaptureSession
#define ZXCaptureVideoPreviewLayer QTCaptureLayer
#define ZXCaptureDeviceInput QTCaptureDeviceInput
#define ZXCaptureVideoOutput QTCaptureDecompressedVideoOutput
#endif

@interface ZXCapture
  : NSObject
ZX(<CAAction ZXAVC(AVCaptureVideoDataOutputSampleBufferDelegate)>) {
  ZX(
    ZXCaptureSession* session;
    ZXCaptureVideoPreviewLayer* layer;
    ZXCaptureVideoOutput* output;
    ZXCaptureDeviceInput* input;
    id<ZXCaptureDelegate> delegate;
    )
    
  int order_in_skip;
  int order_out_skip;
  BOOL running;
  BOOL on_screen;
  CALayer* luminance;
  CALayer* binary;
  size_t width;
  size_t height;
  size_t reported_width;
  size_t reported_height;
  NSString* captureToFilename;
  BOOL hard_stop;
  int camera;
  BOOL torch;
  int device;
  CGAffineTransform transform;
}

@property (nonatomic, assign) id<ZXCaptureDelegate> delegate;
@property (nonatomic, copy) NSString* captureToFilename;
@property (nonatomic) CGAffineTransform transform;
@property (nonatomic, readonly) ZXCaptureVideoOutput* output;
@property (nonatomic, readonly) CALayer* layer;

- (id)init;
- (CALayer*)luminance;
- (void)setLuminance:(BOOL)on_off;
- (CALayer*)binary;
- (void)setBinary:(BOOL)on_off;
- (void)start;
- (void)stop;
- (void)hard_stop;
- (void)order_skip;

@property (nonatomic, readonly) BOOL hasFront;
@property (nonatomic, readonly) BOOL hasBack;
@property (nonatomic, readonly) BOOL hasTorch;

@property (nonatomic, readonly) int front;
@property (nonatomic, readonly) int back;

@property (nonatomic) int camera;
@property (nonatomic) BOOL torch;

@end

#else

@interface ZXCapture : NSObject {
}

@property (nonatomic,assign) id<ZXCaptureDelegate> delegate;
@property (nonatomic,copy) NSString* captureToFilename;
@property (nonatomic) CGAffineTransform transform;
@property (nonatomic, readonly) void* output;
@property (nonatomic, readonly) CALayer* layer;

- (id)init;
- (CALayer*)luminance;
- (void)setLuminance:(BOOL)on_off;
- (CALayer*)binary;
- (void)setBinary:(BOOL)on_off;
- (void)start;
- (void)stop;
- (void)hard_stop;
- (void)order_skip;

@property (nonatomic,readonly) BOOL hasFront;
@property (nonatomic,readonly) BOOL hasBack;
@property (nonatomic,readonly) BOOL hasTorch;

@property (nonatomic,readonly) int front;
@property (nonatomic,readonly) int back;

@property (nonatomic) int camera;
@property (nonatomic) BOOL torch;

@end

#endif
