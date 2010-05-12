//
//  EmailAction.m
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

#import "EmailAction.h"

@implementation EmailAction

@synthesize recipient;

static NSURL *MailtoURL(NSString *to, NSString *sub, NSString *body) {
  NSMutableString *result = [NSMutableString stringWithFormat:@"mailto:%@", 
            [to stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
  if (sub) {
    [result appendFormat:@"&subject=%@", [sub stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
  }
  if (body) {
    [result appendFormat:@"&body=%@", [body stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
  }
  return [NSURL URLWithString:result];
}

- (id)initWithRecipient:(NSString *)rec subject:(NSString *)subject body:(NSString *)body {
  if ((self = [super initWithURL:MailtoURL(rec, subject, body)]) != nil) {
    self.recipient = rec;
  }
  return self;
}

+ (id)actionWithRecipient:(NSString *)recipient subject:(NSString *)subject body:(NSString *)body {
  return [[[self alloc] initWithRecipient:recipient subject:subject body:body] autorelease];
}

- (NSString *)title {
  return [NSString localizedStringWithFormat:NSLocalizedString(@"EmailAction action title", @"Email %@"), self.recipient];
}

- (NSString *)alertTitle {
  return NSLocalizedString(@"EmailAction alert title", @"Compose Email");
}

- (NSString *)alertMessage {
  return [NSString localizedStringWithFormat:NSLocalizedString(@"EmailAction alert message", @"Compose Email to %@?"), self.recipient];
}

- (NSString *)alertButtonTitle {
  return NSLocalizedString(@"EmailAction alert button title", @"Compose");
}


@end
