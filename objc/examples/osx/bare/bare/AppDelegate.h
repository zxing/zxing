// -*- mode:objc; c-basic-offset:2; indent-tabs-mode:nil -*-
/*
 * Copyright 2012 ZXing authors
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

@class ZXCapture;

@interface AppDelegate : NSObject <NSApplicationDelegate,
                                     NSWindowDelegate,
                                     ZXCaptureDelegate>

@property (retain) NSWindow* window;
@property (retain) NSUserDefaults* prefs;
@property (retain) ZXCapture* capture;
@property (retain) CALayer* layer;
@property (retain) NSTextView* text;
@property (assign) CGRect text_frame;
@property (assign) double width;
@property (assign) double height;
@property (readonly) BOOL show_luminance;
@property (readonly) BOOL show_binary;

- (void)cancel;

- (void)captureResult:(ZXCapture*)capture result:(ZXResult*)result;
- (void)captureSize:(ZXCapture*)capture
              width:(NSNumber*)width
             height:(NSNumber*)height;

@end
