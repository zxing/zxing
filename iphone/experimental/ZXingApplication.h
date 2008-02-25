/*
 * Copyright 2008 Google Inc.
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

#import <Foundation/Foundation.h>
#import <PhotoLibrary/CameraController.h>
#import <UIKit/UIKit.h>
#import <UIKit/UIApplication.h>

@interface ZXingApplication : UIApplication
{
  CameraController* cameraController;

  UIWindow* mainWindow;

  UIView* previewView;
  UIView* decodeView;

  UIImageView* decodeImageView;
}

- (void) decodeImage: (id) param1;

- (void) cameraControllerReadyStateChanged: (id) param1;
- (void) cameraController: (id) param1
              tookPicture: (id) param2
              withPreview: (id) preview
                 jpegData: (id) param3
          imageProperties: (id) param4;

@end
