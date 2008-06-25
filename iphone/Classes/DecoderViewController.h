//
//  DecoderViewController.h
//  ZXing
//
//  Created by Christian Brunschen on 22/05/2008.
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

#import <UIKit/UIKit.h>
#import "Decoder.h"
#import "DecoderDelegate.h"

@class ParsedResult;
@class Scan;
@class TwoDDecoderResult;
@class ResultAction;


@interface DecoderViewController : UIViewController <DecoderDelegate, UINavigationControllerDelegate, UIImagePickerControllerDelegate, UIActionSheetDelegate> {
  IBOutlet UIBarItem *cameraBarItem;
  IBOutlet UIBarItem *savedPhotosBarItem;
  IBOutlet UIBarItem *libraryBarItem;
  IBOutlet UIBarItem *archiveBarItem;
  IBOutlet UIBarItem *actionBarItem;
  
  IBOutlet UITextView *messageView;
  IBOutlet UIView *resultView;
  IBOutlet UIImageView *imageView;
  IBOutlet UIToolbar *toolbar;
  
  Decoder *decoder;
  ParsedResult *result;
  NSArray *actions;
}

@property (nonatomic, retain) UIBarItem *cameraBarItem;
@property (nonatomic, retain) UIBarItem *savedPhotosBarItem;
@property (nonatomic, retain) UIBarItem *libraryBarItem;
@property (nonatomic, retain) UIBarItem *archiveBarItem;
@property (nonatomic, retain) UIBarItem *actionBarItem;

@property (nonatomic, retain) UITextView *messageView;
@property (nonatomic, retain) UIView *resultView;
@property (nonatomic, retain) UIImageView *imageView;
@property (nonatomic, retain) UIToolbar *toolbar;

@property (nonatomic, retain) Decoder *decoder;
@property (nonatomic, retain) ParsedResult *result;
@property (nonatomic, retain) NSArray *actions;

- (void)updateToolbar;
- (void)pickAndDecodeFromSource:(UIImagePickerControllerSourceType) sourceType;
- (IBAction)pickAndDecode:(id)sender;
- (void)showMessage:(NSString *)message;
- (IBAction)performResultAction:(id)sender;
- (IBAction)showArchive:(id)sender;
- (void)showScan:(Scan *)scan;

/* DecoderDelegate methods */

- (void)decoder:(Decoder *)decoder willDecodeImage:(UIImage *)image;
- (void)decoder:(Decoder *)decoder decodingImage:(UIImage *)image usingSubset:(UIImage *)subset progress:(NSString *) message;
- (void)decoder:(Decoder *)decoder didDecodeImage:(UIImage *)image usingSubset:(UIImage *)subset withResult:(TwoDDecoderResult *)result;

/* UIImagePickerControllerDelegate methods */

- (void)imagePickerController:(UIImagePickerController *)picker
        didFinishPickingImage:(UIImage *)image
                  editingInfo:(NSDictionary *)editingInfo;
- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker;

/* UINavigationControllerDelegate methods */
- (void)navigationController:(UINavigationController *)navigationController didShowViewController:(UIViewController *)viewController animated:(BOOL)animated;
- (void)navigationController:(UINavigationController *)navigationController willShowViewController:(UIViewController *)viewController animated:(BOOL)animated;

@end
