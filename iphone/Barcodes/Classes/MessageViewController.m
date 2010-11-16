//
//  MessageViewController.m
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

#import "MessageViewController.h"


@implementation MessageViewController

//@synthesize callbackTarget;
//@synthesize callbackSelectorSuccess;
//@synthesize callbackSelectorFailure;
@synthesize contentURL;
@synthesize webView;
@synthesize delegate;

//- (UIWebView *)webView {
//  return (UIWebView *)self.view;
//}

- (id)initWithMessageFilename:(NSString *)filename {
	if ((self = [super initWithNibName:@"Message" bundle:nil])) {
    self.contentURL = [NSURL fileURLWithPath:[[NSBundle mainBundle] pathForResource:filename 
                                                                             ofType:@"html"]];
	}
	return self;
}

- (IBAction)dismiss:(id)sender {
  [delegate modalViewControllerWantsToBeDismissed:self];
}

- (void)loadView {
  [super loadView];
  self.webView.delegate = self;
  [self.webView loadRequest:[NSURLRequest requestWithURL:self.contentURL]];
}

- (void)viewDidLoad {
  [super viewDidLoad];
}


- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
	// Return YES for supported orientations
	return (interfaceOrientation == UIInterfaceOrientationPortrait);
}

- (void)didReceiveMemoryWarning {
	[super didReceiveMemoryWarning]; // Releases the view if it doesn't have a superview
	// Release anything that's not essential, such as cached data
}


- (void)dealloc {
  delegate = nil;
  [webView release];
	[super dealloc];
}

// open a URL, asynchronously
- (void) openURL:(NSURL *)url {
  [url autorelease];
  [[UIApplication sharedApplication] openURL:url];
}


// UIWebViewDelegate methods

- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType {
  if ([[request URL] isFileURL]) {
    // only load 'file' URL requests ourselves
    return true;
  } else {
    // any other url:s are handed off to the system
    NSURL *url = [[request URL] retain];
    [self performSelectorOnMainThread:@selector(openURL:) withObject:url waitUntilDone:NO];
    return NO;
  }
}

- (void)webView:(UIWebView *)webView didFailLoadWithError:(NSError *)error {
  NSLog(@"failed to load content, performing failure callback");
}

- (void)webViewDidFinishLoad:(UIWebView *)webView {
  NSLog(@"finished loading content, performing success callback");
}


@end
