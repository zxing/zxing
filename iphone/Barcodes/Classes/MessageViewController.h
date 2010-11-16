//
//  MessageViewController.h
//  ZXing
//
//  Created by Christian Brunschen on 30/07/2008.
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
#import "ModalViewControllerDelegate.h"
@class MessageViewController;



@interface MessageViewController : UIViewController <UIWebViewDelegate> {
//  id callbackTarget;
//  SEL callbackSelectorSuccess;
//  SEL callbackSelectorFailure;
  NSURL *contentURL;
  IBOutlet UIWebView *webView;
  id<ModalViewControllerDelegate> delegate;
}

//@property (nonatomic, retain) id callbackTarget;
//@property (nonatomic, assign) SEL callbackSelectorSuccess;
//@property (nonatomic, assign) SEL callbackSelectorFailure;

@property (nonatomic,retain) IBOutlet UIWebView *webView;
@property (nonatomic,assign) id<ModalViewControllerDelegate> delegate;
@property (nonatomic, retain) NSURL *contentURL;

- (id)initWithMessageFilename:(NSString *)filename;
- (IBAction)dismiss:(id)sender;

@end
