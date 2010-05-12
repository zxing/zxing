//
//  OpenUrlAction.m
//  ZXing
//
//  Created by Christian Brunschen on 28/05/2008.
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

#import "OpenUrlAction.h"


@implementation OpenUrlAction

@synthesize URL;

- (id)initWithURL:(NSURL *)url {
  if ((self = [super init]) != nil) {
    self.URL = url;
  }
  return self;
}

+ (id)actionWithURL:(NSURL *)URL {
  return [[[self alloc] initWithURL:URL] autorelease];
}

- (NSString *)title {
  return [NSString localizedStringWithFormat:NSLocalizedString(@"OpenURLAction action title", @"Open URL"), self.URL];
}

- (NSString *)alertTitle {
  return NSLocalizedString(@"OpenURLAction alert title", @"Open URL");
}

- (NSString *)alertMessage {
  return [NSString localizedStringWithFormat:NSLocalizedString(@"OpenURLAction alert message", @"Open URL <%@>?"), self.URL];
}

- (NSString *)alertButtonTitle {
  return NSLocalizedString(@"OpenURLAction alert button title", @"Open");
}

- (void)performActionWithController:(UIViewController *)controller 
                      shouldConfirm:(bool)shouldConfirm {
  if (shouldConfirm) {
    UIAlertView *alertView = [[UIAlertView alloc] initWithTitle:nil 
                                                        message:[self alertMessage] 
                                                       delegate:self 
                                              cancelButtonTitle:NSLocalizedString(@"OpenURLAction cancel button title", @"Cancel") 
                                              otherButtonTitles:[self alertButtonTitle], nil];
    [alertView show];
    [alertView release];
  } else {
    [self openURL];
  }
}

- (void)openURL {
  [[UIApplication sharedApplication] openURL:self.URL];
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
  if (buttonIndex != [alertView cancelButtonIndex]) {
    // perform the action
    [self openURL];
  }
}

@end
