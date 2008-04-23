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
//  DecoderViewController.m
//  ZXing
//
//  Created by Christian Brunschen on 31/03/2008.
//

#import "DecoderViewController.h"
#import "Decoder.h"

@implementation DecoderViewController

@synthesize imageView;
@synthesize messageView;
@synthesize decoder;

- (id)init
{
    if (self = [super init]) {
        // Initialize your view controller.
        self.title = @"DecoderViewController";

        Decoder *d = [[Decoder alloc] init];
        self.decoder = d;
        d.delegate = self;
        [d release];
    }
    return self;
}


- (void)loadView
{
    [[NSBundle mainBundle] loadNibNamed:@"DecoderView" owner:self options:nil];
    
    /* programmatically create the message view for now */
    // TODO(christian.brunschen): find a suitable & working IB configuration
    CGRect rect = [UIScreen mainScreen].applicationFrame;
    rect = CGRectInset(rect, CGRectGetWidth(rect)/4, CGRectGetHeight(rect)/3);
    rect.origin.y -= 50;
    UITextView *mView = [[UITextView alloc] initWithFrame:rect];
    mView.autoresizingMask = UIViewAutoresizingFlexibleHeight|UIViewAutoresizingFlexibleWidth;
    mView.text = @"Please take or select a picture containing a barcode";
    mView.backgroundColor = [UIColor yellowColor];
    mView.alpha = 0.7;
    [self.view addSubview:mView];
    self.messageView = mView;
    [mView release];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    // Return YES for supported orientations.
    return YES;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning]; // Releases the view if it doesn't have a superview.
    // Release anything that's not essential, such as cached data.
}

- (void)dealloc
{
    [decoder release];
    [imageView release];
    [messageView release];
    [super dealloc];
}


// own methods

- (void)pickAndDecode {
    // Create the Image Picker
    UIImagePickerControllerSourceType sourceType = 
    [UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera] 
    ? UIImagePickerControllerSourceTypeCamera 
    : UIImagePickerControllerSourceTypePhotoLibrary;
    
    UIImagePickerController* picker = [[UIImagePickerController alloc] init];
    picker.sourceType = sourceType;
    picker.delegate = self;
    picker.allowsImageEditing = YES;
        
    // Picker is displayed asynchronously.
    [self presentModalViewController:picker animated:YES];
}


// DecoderDelegate methods

- (void)decoder:(Decoder *)decoder willDecodeImage:(UIImage *)image {
    [self.imageView setImage:image];
    self.messageView.text = [NSString stringWithFormat:@"Decoding image (%.0fx%.0f) ...", image.size.width, image.size.height];
}

- (void)decoder:(Decoder *)decoder didDecodeImage:(UIImage *)image withResult:(NSString *)result {
    self.messageView.text = result;
}


// UIImagePickerControllerDelegate methods

- (void)imagePickerController:(UIImagePickerController *)picker
        didFinishPickingImage:(UIImage *)image
                  editingInfo:(NSDictionary *)editingInfo
{
    [[picker parentViewController] dismissModalViewControllerAnimated:YES];
    [image retain];
    [picker release];
    [self.decoder decodeImage:image];
    [image release];
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker
{
    [picker dismissModalViewControllerAnimated:YES];
    [picker release];
}



@end
