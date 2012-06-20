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

#import "AppDelegate.h"
#import <ZXing/ZXCapture.h>
#import <ZXing/ZXResult.h>

#if DEBUG
#include <iostream>
using std::cerr;
using std::endl;
using std::ostream;
ostream& operator << (ostream& os, id object) {
  os << [[NSString stringWithFormat:@"%@", object] UTF8String];
  return os; 
}
ostream& operator << (ostream& os, CGPoint const& p) {
  os << '{' << p.x << "," << p.y << '}';
  return os; 
}
ostream& operator << (ostream& os, CGSize const& s) {
  os << '{' << s.width << "," << s.height << '}';
  return os; 
}
ostream& operator << (ostream& os, CGRect const& r) {
  os << '{' << r.origin << "," << r.size << '}';
  return os;
}
#endif

namespace {
  double height(NSString* string, NSFont* font, double width) {
    NSTextStorage* ts =
      [[[NSTextStorage alloc] initWithString:string] autorelease];
    NSTextContainer* tc = 
      [[[NSTextContainer alloc]
         initWithContainerSize:CGSizeMake(width, 9999999)] autorelease];
    NSLayoutManager* lm = [[[NSLayoutManager alloc] init] autorelease];
    [lm addTextContainer:tc];
    [ts addLayoutManager:lm];
    [ts addAttribute:NSFontAttributeName
               value:font
               range:NSMakeRange(0, ts.length)];
    [tc setLineFragmentPadding:0];
    [lm glyphRangeForTextContainer:tc];
    return [lm usedRectForTextContainer:tc].size.height;
  }
}

@interface WindowView : NSView @end
@interface Menu : NSMenu
- (void)items:(id)items;
@end
@interface MainMenu : Menu @end
@interface Item : NSMenuItem
- (id)initWithMenu:(Menu*)parent arg:(id)arg;
@end

@implementation AppDelegate

@synthesize window = window_;
@synthesize prefs = prefs_;
@synthesize capture = capture_;
@synthesize layer = layer_;
@synthesize text = text_;
@synthesize text_frame = text_frame_;
@synthesize height = height_;
@synthesize width = width_;

- (void)dealloc {
  self.window = nil;
  self.prefs = nil;
  self.capture = nil;
  self.layer = nil;
  self.text = nil;
  [super dealloc];
}

-(void)preferences {
  self.prefs = [NSUserDefaults standardUserDefaults];
  [self.prefs registerDefaults:
         [NSDictionary dictionaryWithObjects:
                   [NSArray arrayWithObjects:
                              [NSNumber numberWithBool:NO],
                            [NSNumber numberWithBool:NO],
                            [NSNumber numberWithBool:NO],
                            [NSNumber numberWithBool:YES],
                            nil]
                  forKeys:
                   [NSArray arrayWithObjects:
                              @"show_luminance",
                            @"show_binary",
                            @"fullscreen",
                            @"continuous",
                            nil]]];
}

- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication *)theApplication {
  return YES;
}

- (CGSize)resize:(CGSize)size {
  CGRect frame = CGRectMake(0, 0, size.width, size.height);
  frame = self.layer.bounds;
  
  double window_ar = frame.size.width/frame.size.height;
  double video_ar = 1.0*self.width/self.height;

  if (fabs(video_ar-window_ar) > 0.001) {
    if (video_ar > window_ar) {
      frame.origin.y = (frame.size.height-frame.size.width/video_ar)/2;
      frame.size.height = frame.size.width/video_ar;
    } else {
      frame.origin.x = (frame.size.width-frame.size.height*video_ar)/2;
      frame.size.width = frame.size.height*video_ar;
    }
  }

  self.capture.layer.frame = frame;

  frame = CGRectMake(0, 0, size.width, size.height);
  frame = self.layer.bounds;
  self.text_frame = self.text.frame = CGRectMake(0.1*frame.size.width,
                                                 0.05*frame.size.height,
                                                 0.8*frame.size.width,
                                                 0.45*frame.size.height);

  if ([self.prefs boolForKey:@"show_luminance"]) {
    frame = CGRectMake(0, 0, size.width, size.height);
    frame = [self.window contentRectForFrameRect:frame];
    frame = self.layer.bounds;
    double width = frame.size.width;
    frame.size.height *= 1/3.0;
    frame.size.width *= 1/3.0;

    double window_ar = frame.size.width/frame.size.height;
    double video_ar = 1.0*self.width/self.height;

    if (fabs(video_ar-window_ar) > 0.001) {
      if (video_ar > window_ar) {
        frame.size.height = frame.size.width/video_ar;
      } else {
        frame.size.width = frame.size.height*video_ar;
      }

      frame.origin.x = width - frame.size.width;
      
      self.capture.luminance = true;
      self.capture.luminance.frame = frame;
    }
  }
    
  if ([self.prefs boolForKey:@"show_binary"]) {
    frame = CGRectMake(0, 0, size.width, size.height);
    frame = [self.window contentRectForFrameRect:frame];
    frame = self.layer.bounds;
    frame.size.height *= 1/3.0;
    frame.size.width *= 1/3.0;
      
    window_ar = frame.size.width/frame.size.height;
    video_ar = 1.0*self.width/self.height;

    if (fabs(video_ar-window_ar) > 0.001) {
      if (video_ar > window_ar) {
        frame.size.height = frame.size.width/video_ar;
      } else {
        frame.size.width = frame.size.height*video_ar;
      }
    }

      self.capture.binary = true;
      self.capture.binary.frame = frame;
    }
  
  return size;
}

- (void)quit:(id)item {
  [NSApp terminate:self];
}

- (BOOL)show_luminance {
  return [self.prefs boolForKey:@"show_luminance"];
}

- (BOOL)show_binary {
  return [self.prefs boolForKey:@"show_binary"];
}

- (void)cancel {
  [self.prefs setBool:NO forKey:@"fullscreen"];
  [self.window.contentView exitFullScreenModeWithOptions:nil];
}

- (void)luminance:(id)item {
  [self.prefs setBool:![self.prefs boolForKey:@"show_luminance"]
               forKey:@"show_luminance"];
  [item setState:[self.prefs boolForKey:@"show_luminance"]];
  if ([self.prefs boolForKey:@"show_luminance"]) {
    self.capture.luminance = true;
    [self.layer addSublayer:self.capture.luminance];
    [self resize:self.window.frame.size];
  } else {
    [self.capture.luminance removeFromSuperlayer];
    self.capture.luminance = false;
  }
}

- (void)binary:(id)item {
  [self.prefs setBool:![self.prefs boolForKey:@"show_binary"]
               forKey:@"show_binary"];
  [item setState:[self.prefs boolForKey:@"show_binary"]];
  if ([self.prefs boolForKey:@"show_binary"]) {
    self.capture.binary = true;
    [self.layer addSublayer:self.capture.binary];
    [self resize:self.window.frame.size];
  } else {
    [self.capture.binary removeFromSuperlayer];
    self.capture.binary = false;
  }
}

- (void)capture:(id)item {
  NSDate* date = [NSDate date];
  time_t time = [date timeIntervalSince1970];
  struct tm timeStruct;
  localtime_r(&time, &timeStruct);
  char buffer[80];
  strftime(buffer, 80, "%Y-%m-%d at %I.%M.%S %p", &timeStruct);
  NSString* now =
    [NSString stringWithCString:buffer encoding:NSASCIIStringEncoding];
  self.capture.captureToFilename =
    [NSString stringWithFormat:@"%s/Desktop/ZXing capture %@.png",
              getenv("HOME"), now];
}

- (void)fullscreen:(id)item {
  [self.prefs setBool:![self.prefs boolForKey:@"fullscreen"]
               forKey:@"fullscreen"];
  if ([self.prefs boolForKey:@"fullscreen"]) {
    NSDictionary* options =
      [NSDictionary dictionaryWithObject:[NSNumber numberWithBool:NO]
                                  forKey:NSFullScreenModeAllScreens];
    [self.window.contentView
        enterFullScreenMode:self.window.screen
        withOptions:options];
  } else {
    [self.window.contentView exitFullScreenModeWithOptions:nil];
  }
}

- (void)resizeNotification:(NSNotification*)notification {
  WindowView* wv = (WindowView*)notification.object;
  [self resize:[self.window frameRectForContentRect:wv.bounds].size];
}

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification {
  [self preferences];
  NSUInteger mask =
    NSTitledWindowMask|
    NSClosableWindowMask|
    NSMiniaturizableWindowMask|
    NSResizableWindowMask;
  CGRect frame = 
    [NSWindow frameRectForContentRect:CGRectMake(0, 0, 640, 480)
                            styleMask:mask];
  self.window = 
    [[[NSWindow alloc]
       initWithContentRect:frame
                 styleMask:mask
                   backing:NSBackingStoreBuffered
                     defer:NO] autorelease];
  
  [self.window center];

  [self.window setFrameAutosaveName:@"SomeWindow"];
  [self.window setFrameUsingName:@"SomeWindow"];

  [NSApp setMainMenu:[[[Menu alloc] init] autorelease]];

  self.capture = [[[ZXCapture alloc] init] autorelease];
  
  self.window.title = @"ZXing";
  self.window.level = NSNormalWindowLevel;
  self.window.delegate = self;
    
  self.window.contentView = [[[WindowView alloc] init] autorelease];

  [NSNotificationCenter.defaultCenter
      addObserver:self
      selector:@selector(resizeNotification:)
      name:NSViewFrameDidChangeNotification
      object:self.window.contentView];

  self.layer = CALayer.layer;
  self.layer.frame = [self.window contentRectForFrameRect:self.window.frame];
  self.layer.backgroundColor = CGColorGetConstantColor(kCGColorBlack);

  [self.layer addSublayer:self.capture.layer];

  if ([self.prefs boolForKey:@"show_luminance"]) {
    self.capture.luminance = true;
    [self.layer addSublayer:self.capture.luminance];
  }

  if ([self.prefs boolForKey:@"show_binary"]) {
    self.capture.binary = true;
    [self.layer addSublayer:self.capture.binary];
  }

  [self.window.contentView setLayer:self.layer];
  [self.window.contentView setWantsLayer:YES];
  CGRect contents =  [self.window contentRectForFrameRect:self.window.frame];

  self.text =
    [[[NSTextView alloc]
       initWithFrame:CGRectMake(0.1*contents.size.width,
                                0.05*contents.size.height,
                                0.8*contents.size.width,
                                0.45*contents.size.height)] autorelease];
  self.text.horizontallyResizable = NO;
  self.text.verticallyResizable = NO;
  self.text.textContainerInset = CGSizeMake(10, 10);
  self.text.textColor = [NSColor yellowColor];
  self.text.editable = NO;
  self.text.font = [NSFont systemFontOfSize:36];
  self.text.alignment = NSCenterTextAlignment;

  [self.window.contentView addSubview:self.text];

  self.text.backgroundColor = NSColor.clearColor;
  self.text.layer.backgroundColor = CGColorCreateGenericRGB(0, 0, 1, 0.4);
  self.text.layer.borderColor = CGColorCreateGenericRGB(1, 1, 1, 0.4);
  self.text.layer.borderWidth = 2;
  self.text.layer.cornerRadius = 10;

  [self.text setAlphaValue:0];

  self.capture.delegate = self;

  [self.window orderFront:self.window];
  // [self.window makeKeyAndOrderFront:NSApp];
}

- (void)captureResult:(ZXCapture*)capture_ result:(ZXResult*)result {
  NSString* value = result.text;
  if (result.text != self.text.string) {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.text.string = result.text;
        self.text.frame = self.text_frame;
        double h =
          height(result.text, self.text.font, self.text.frame.size.width);
        h += 2*self.text.textContainerInset.height;
        if (h <= [self.text frame].size.height) {
          CGRect f = self.text.frame;
          f.size.height = h;
          self.text.frame = f;
        }
        [NSAnimationContext beginGrouping];
        NSAnimationContext.currentContext.duration = 0.8;
        [self.text.animator setAlphaValue:0.9];
        [NSAnimationContext endGrouping];

        dispatch_after(
          dispatch_time(DISPATCH_TIME_NOW, 5 * NSEC_PER_SEC),
          dispatch_get_main_queue(),
          ^{
            if ([self.text.string isEqualToString:value] ||
                [self.text.string  isEqualToString:@""]) {
              [NSAnimationContext beginGrouping];
              NSAnimationContext.currentContext.duration = 0.8;
              [self.text.animator setAlphaValue:0];
              [NSAnimationContext endGrouping];
              dispatch_after(
                dispatch_time(DISPATCH_TIME_NOW, 1 * NSEC_PER_SEC),
                dispatch_get_main_queue(),
                ^{
                  if (self.text.string == value) {
                    self.text.string = @"";
                  }
                });
            }
          });
        NSLog(@"%@", result.text);
      });
    if (![self.prefs boolForKey:@"continuous"]) {
      [self.capture stop];
      [NSApp
          performSelectorOnMainThread:@selector(terminate:)
                           withObject:self
                        waitUntilDone:false];
    }
  }
}

- (void)captureSize:(ZXCapture*)capture_
              width:(NSNumber*)width
             height:(NSNumber*)height {
  self.width = [width doubleValue];
  self.height = [height doubleValue];
  dispatch_async(dispatch_get_main_queue(), ^{
      [self resize:self.window.frame.size];
      [self.window orderFrontRegardless];
    });
}

@end

@implementation WindowView
- (void)cancelOperation:(id)sender {
  AppDelegate* d = (AppDelegate*)[NSApp delegate];
  [d cancel];
}
@end

@implementation Menu
- (id)init {
  if ((self = [super initWithTitle:@""])) {
    NSString* name = NSRunningApplication.currentApplication.localizedName;
    
    NSMutableArray* items = [NSMutableArray arrayWithCapacity:0];
    
    { NSMutableArray* apple = [NSMutableArray arrayWithCapacity:0];
      
      { NSMutableDictionary* about = [NSMutableDictionary dictionaryWithCapacity:0];
        { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
          [contents setObject:[NSString stringWithFormat:@"About %@", name]
                       forKey:@"title"];
          [about setObject:contents forKey:@"about"]; }
        [apple addObject:about]; }

      [apple addObject:@"separator"];
      
      { NSMutableDictionary* services = [NSMutableDictionary dictionaryWithCapacity:0];
        [apple addObject:services]; }

      [apple addObject:@"separator"];
      
      { NSMutableDictionary* hide = [NSMutableDictionary dictionaryWithCapacity:0];
        { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
          [contents setObject:[NSString stringWithFormat:@"Hide %@", name]
                       forKey:@"title"];
          [contents setObject:@"cmd:h" forKey:@"key"];
          [hide setObject:contents forKey:@"hide"]; }
        [apple addObject:hide]; }

      { NSMutableDictionary* hide_others = [NSMutableDictionary dictionaryWithCapacity:0];
        { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
          [contents setObject:@"cmd:opt:h" forKey:@"key"];
          [hide_others setObject:contents forKey:@"hide_others"]; }
        [apple addObject:hide_others]; }

      [apple addObject:@"show_all"];

      [apple addObject:@"separator"];

      { NSMutableDictionary* quit = [NSMutableDictionary dictionaryWithCapacity:0];
        { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
          [contents setObject:[NSString stringWithFormat:@"Quit %@", name]
                 forKey:@"title"];
          [contents setObject:@"cmd:q" forKey:@"key"];
          [quit setObject:contents forKey:@"quit"]; }
        [apple addObject:quit]; }

      [items addObject:[NSDictionary dictionaryWithObject:apple forKey:@"apple"]]; }

    { NSMutableDictionary* view = [NSMutableDictionary dictionaryWithCapacity:0];
      
      { NSMutableDictionary* key = [NSMutableDictionary dictionaryWithCapacity:0];
        [key setObject:@"View" forKey:@"title"];

        NSMutableArray* contents = [NSMutableArray arrayWithCapacity:0];

        { NSMutableDictionary* fullscreen = [NSMutableDictionary dictionaryWithCapacity:0];

          { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
            [contents setObject:@"cmd:f" forKey:@"key"];
            [fullscreen setObject:contents forKey:@"fullscreen"]; }

          [contents addObject:fullscreen]; }

        { NSMutableDictionary* capture = [NSMutableDictionary dictionaryWithCapacity:0];
          
          { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
            [contents setObject:@"cmd:c" forKey:@"key"];
            [capture setObject:contents forKey:@"capture"]; }
          [contents addObject:capture]; }

        AppDelegate* ad = (AppDelegate*)[NSApp delegate];

        { NSMutableDictionary* luminance = [NSMutableDictionary dictionaryWithCapacity:0];
          { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
            [contents setObject:@"cmd:l" forKey:@"key"];
            [contents setObject:[NSNumber numberWithBool:ad.show_luminance]
                         forKey:@"state"];
            [luminance setObject:contents forKey:@"luminance"]; }
          [contents addObject:luminance]; }

        { NSMutableDictionary* binary = [NSMutableDictionary dictionaryWithCapacity:0];
          { NSMutableDictionary* contents = [NSMutableDictionary dictionaryWithCapacity:0];
            [contents setObject:@"cmd:b" forKey:@"key"];
            [contents setObject:[NSNumber numberWithBool:ad.show_binary]
                         forKey:@"state"];
            [binary setObject:contents forKey:@"binary"]; }
          [contents addObject:binary]; }

        [view setObject:contents forKey:key]; }

      [items addObject:[NSDictionary dictionaryWithObject:view forKey:@"view"]]; }

    [items addObject:[NSDictionary dictionaryWithObject:[NSArray array] forKey:@"window"]];
    [items addObject:[NSDictionary dictionaryWithObject:[NSArray array] forKey:@"help"]];
    
    [self items:items];
  }
    
  return self;
}

- (void)items:(id)args_ {
  NSArray* args = nil;
  if ([args_ isKindOfClass:[NSArray class]]) {
    args = args_;
  } else {
    args = [NSArray arrayWithObject:args_];
  }

  for (id arg in args) {
    if ([arg isKindOfClass:[NSArray class]]) {
      [[[Item alloc] initWithMenu:self arg:arg] autorelease];
    } else if ([arg isKindOfClass:[NSDictionary class]]) {
      for(id key in arg) {
        id value = [arg objectForKey:key];
        [[[Item alloc] initWithMenu:self
                                arg:[NSDictionary dictionaryWithObject:value forKey:key]] autorelease];
      }
    }
  }
  [self update];
}
@end

@implementation Item

- (void)params:(NSDictionary*)params {
  for(id key_ in params) {
    if (![key_ isKindOfClass:[NSString class]]) {
      throw  "don't understand param";
    }
    NSString* key = (NSString*)key_;
    id value = [params objectForKey:key];
    if ([key isEqualToString:@"title"]) {
      self.title = value;
    } else if ([key isEqualToString:@"key"]) {
      if (![value isKindOfClass:[NSString class]]) {
        throw "don't understand value";
      }
      NSString* string = (NSString*)value;
      NSString* ke = nil;
      NSUInteger m = 0;
      for(id v in [string componentsSeparatedByString: @":"]) {
        NSString* string = (NSString*)v;
        if ([string isEqualToString:@"cmd"]) {
          m |= NSCommandKeyMask;
        } else if ([string isEqualToString:@"ctl"]) {
          m |= NSControlKeyMask;
        } else if ([string isEqualToString:@"shift"]) {
          m |= NSShiftKeyMask;
        } else if ([string isEqualToString:@"opt"]) {
          m |= NSAlternateKeyMask;
        } else {
          ke = string;
        }
      }
      self.keyEquivalent = ke;
      self.keyEquivalentModifierMask = m;
    } else if ([key isEqualToString:@"state"]) {
      if (![value isKindOfClass:[NSNumber class]]) {
        throw "don't understand param #{key}: #{value}";
      }
      self.state = [(NSNumber*)value boolValue];
    } else {
      throw "don't understand param #{key}: #{value}";
    }
  }
}


- (id)initWithMenu:(Menu*)parent arg:(id)arg {
  if ((self = [super initWithTitle:@"" action:nil keyEquivalent:@""])) {
    self.target = [NSApp delegate];
    self.enabled = true;
    Menu* menu = nil;
    NSDictionary* dict = nil;
    if ([arg isKindOfClass:[NSDictionary class]]) {
      dict = (NSDictionary*)arg;
    }
    if (dict) {
      if ([dict count] != 1) {
        throw "not sure what #{arg} means 0";
      }
      id key = [[dict keyEnumerator] nextObject];
      id value = [dict objectForKey:key];
      if ([key isKindOfClass:[NSString class]]) {
        NSString* s = (NSString*)key;
        self.action = NSSelectorFromString([NSString stringWithFormat:@"%@:", s]);
        self.title =
          [[s stringByReplacingOccurrencesOfString:@"_" withString:@" "] capitalizedString];
      } else if ([key isKindOfClass:[NSDictionary class]]) {
        [self params:key];
      } else {
        throw "not sure what #{key} means 1";
      }
      if ([value isKindOfClass:[NSString class]]) {
        throw "oops";
      } else if ([value isKindOfClass:[NSArray class]]) {
        menu = [[[Menu alloc] initWithTitle:self.title] autorelease];
        [menu items:value];
      } else if ([value isKindOfClass:[NSDictionary class]]) {
        NSDictionary* dict = (NSDictionary*)value;
        if ([dict count] == 1 &&
            [[[dict keyEnumerator] nextObject]
              isKindOfClass:[NSDictionary class]]) {
          [self params:[[dict keyEnumerator] nextObject]];
          menu = [[[Menu alloc] initWithTitle:self.title] autorelease];
          [menu items:[dict objectForKey:[[dict keyEnumerator] nextObject]]];
        } else {
          [self params:value];
        }
      } else {
        throw "not sure what #{value} means 2";
      }
    } else {
      throw "implement #{arg.class} item arg #{arg}";
    }
    [parent addItem:self];
    if (dict && menu) {
      NSString* first = nil;
      if ([[dict objectForKey:[[dict keyEnumerator] nextObject]]
            isKindOfClass:[NSString class]]) {
        first = (NSString*)[dict objectForKey:[[dict keyEnumerator] nextObject]];
      }
      if (first) {
        if ([first isEqualToString:@"apple"]) {
          // [NSApp setAppleMenu:menu];
          [NSApp performSelector:@selector(setAppleMenu:) withObject:menu];
        } else if ([first isEqualToString:@"services"]) {
          [NSApp setServicesMenu:menu];
        } else if ([first isEqualToString:@"window"]) {
          [NSApp setWindowsMenu:menu];
        }
      }
      self.action = nil;
      self.target = nil;
      [parent setSubmenu:menu forItem:self];
    }
  }
  return self;
}

@end
