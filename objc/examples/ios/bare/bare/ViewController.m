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

#import "ViewController.h"
#import <QuartzCore/QuartzCore.h> // seems not included by default in the sim
#import <ZXing/ZXCapture.h>
#import <ZXing/ZXResult.h>

@interface View : UIView @end
@implementation View

- (UIView*)swap_view {
  if ([[self subviews] count]) {
    return [[self subviews] objectAtIndex:0];
  } else {
    return nil;
  }
}

- (void)layoutSubviews {
  [super layoutSubviews];
  CGRect bounds = self.bounds;
  for(int i=0; i < [[[self layer] sublayers] count]; ++i) {
    CALayer* layer = (CALayer*)[[self.layer sublayers] objectAtIndex:i];
    if (layer != [self swap_view].layer) {
      layer.frame = bounds;
    }
  }
  CGRect frame = self.swap_view.bounds;
  frame.origin.x = bounds.size.width - frame.size.width - 10;
  frame.origin.y = 10;
  self.swap_view.frame = frame;
}

@end

@implementation ViewController

- (void)dealloc {
  [swap release];
  [capture release];
  [super dealloc];
}

- (ZXCapture*)capture {
  if (!capture) {
    capture = [[ZXCapture alloc] init];
    capture.delegate = self;
  }
  return capture;
}

- (void)loadView {
  self.view = [[[View alloc] init] autorelease];
  [self.view.layer addSublayer:self.capture.layer];

  // self.capture.luminance = true;
  // [self.view.layer addSublayer:self.capture.luminance];

  // self.capture.binary = true;
  //[ self.view.layer addSublayer:self.capture.binary];

  if (!swap && self.capture.hasFront && self.capture.hasBack) {
    swap = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [swap setTitle:@"swap" forState:UIControlStateNormal];
    [swap sizeToFit];
    [swap addTarget:self action:@selector(swap) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:swap];
  }
  [self swap];
  [self.capture start];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  return YES;
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration {
  CGAffineTransform t = CGAffineTransformIdentity;
  CGSize size = self.capture.layer.bounds.size;
  (void)size;
  switch(toInterfaceOrientation) {
  case UIInterfaceOrientationPortraitUpsideDown:
    t = CGAffineTransformMakeRotation(M_PI);
    break;

  case UIInterfaceOrientationLandscapeLeft:
    t = CGAffineTransformMakeRotation(M_PI/2);
    break;

  case UIInterfaceOrientationLandscapeRight:
    t = CGAffineTransformMakeRotation(-M_PI/2);
    break;

  default:
    break;
  }
  if (self.capture.camera == self.capture.front) {
    t.a = -t.a;
  }
  self.capture.layer.affineTransform = t;
  [self.view setNeedsLayout];
}

- (void)didReceiveMemoryWarning {
  if (swap && !swap.window) {
    [swap release];
    swap = nil;
  }
}

- (void)swap {
  self.capture.camera =
    self.capture.camera == self.capture.front ?
    self.capture.back : self.capture.front;
}

- (void)captureResult:(ZXCapture*)capture result:(ZXResult*)result {
  // NSLog(@"%@", result.text);
  UILabel* label = [[[UILabel alloc] init] autorelease];
  label.font = [UIFont systemFontOfSize:36];
  label.text = result.text;
  label.textColor = [UIColor yellowColor];
  label.backgroundColor =
    [[UIColor blackColor] colorWithAlphaComponent:0.5];
  [label sizeToFit];
  label.center = CGPointMake(self.view.bounds.size.width/2,
                             self.view.bounds.size.height/2);
  [self.view addSubview:label];
  [UIView beginAnimations:nil context:NULL];
  [UIView setAnimationDuration:1.0];
  [UIView setAnimationDelegate:label];
  [UIView setAnimationDidStopSelector:@selector(removeFromSuperview)];
  label.alpha = 0;
  [UIView commitAnimations];
}

@end
