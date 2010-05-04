//
//  ZXingAppDelegate.m
//  ZXing
//
//  Created by Christian Brunschen on 23/04/2008.
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


#import "ZXingAppDelegate.h"
#import "DecoderViewController.h"

@implementation ZXingAppDelegate

@synthesize window;
@synthesize viewController;
@synthesize navigationController;

- (void)applicationDidFinishLaunching:(UIApplication *)application {
  /* create the view controller */
  DecoderViewController *vc = 
    [[DecoderViewController alloc] initWithNibName:@"DecoderView" 
                                            bundle:[NSBundle mainBundle]];
  self.viewController = vc;
  [vc release];
  
  navigationController = [[UINavigationController alloc] 
                          initWithRootViewController:viewController];
  
  // hook up the view controller's view to be in the window
  [window addSubview:navigationController.view];
  
  // show the window
  [window makeKeyAndVisible];
  
  // pick and decode using the first available source type in priority order
  UIImagePickerControllerSourceType sourceTypes[] = {
    UIImagePickerControllerSourceTypeCamera,
    UIImagePickerControllerSourceTypeSavedPhotosAlbum,
    UIImagePickerControllerSourceTypePhotoLibrary
  };

  for (int i = 0; i < sizeof(sourceTypes) / sizeof(*sourceTypes); i++) {
    if ([UIImagePickerController isSourceTypeAvailable:sourceTypes[i]]) {
      [viewController pickAndDecodeFromSource:sourceTypes[i]];
      break;
    }
  }
}

- (void)dealloc {
  [window release];
  [super dealloc];
}

@end
