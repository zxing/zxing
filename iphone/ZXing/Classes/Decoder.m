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
//  Decoder.m
//  ZXing
//
//  Created by Christian Brunschen on 31/03/2008.
//

#import "Decoder.h"

@implementation Decoder

@synthesize image;
@synthesize result;
@synthesize delegate;

- (void)decode:(id)arg {
	[NSThread sleepForTimeInterval:2.0];
	self.result = @"This is some sample (fake) decoded text.";
	[self.delegate decoder:self didDecodeImage:self.image withResult:self.result];	
}

- (void) decodeImage:(UIImage *)i {
	self.image = i;
	[self.delegate decoder:self willDecodeImage:i];
	[NSThread detachNewThreadSelector:@selector(decode:) toTarget:self withObject:nil];
}

- (void) dealloc {
	[image release];
	[result release];
	[super dealloc];
}

@end
