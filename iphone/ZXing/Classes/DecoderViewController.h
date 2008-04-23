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

//
//  DecoderViewController.h
//  ZXing
//
//  Created by Christian Brunschen on 31/03/2008.
//

#import <UIKit/UIKit.h>
#import "Decoder.h"
#import "DecoderDelegate.h"

@interface DecoderViewController : UIViewController <DecoderDelegate, UIImagePickerControllerDelegate> {
    IBOutlet UIImageView *imageView;
    IBOutlet UITextView *messageView;
    Decoder *decoder;
}

@property (nonatomic, retain) UIImageView *imageView;
@property (nonatomic, retain) UITextView *messageView;
@property (nonatomic, retain) Decoder *decoder;

- (void)pickAndDecode;

/* DecoderDelegate methods */

- (void)decoder:(Decoder *)decoder willDecodeImage:(UIImage *)image;
- (void)decoder:(Decoder *)decoder didDecodeImage:(UIImage *)image withResult:(NSString *)result;

/* UIImagePickerControllerDelegate methods */

- (void)imagePickerController:(UIImagePickerController *)picker
        didFinishPickingImage:(UIImage *)image
                  editingInfo:(NSDictionary *)editingInfo;
- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker;


@end
