//
//  ZXMainViewController.h
//  Barcodes
//
//  Created by Romain Pechayre on 11/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "ZXingWidgetController.h"
#import "MessageViewController.h"

@class UniversalResultParser;
@class ParsedResult;


@interface ZXMainViewController : UIViewController <ZXingDelegate,UIActionSheetDelegate,MessageViewControllerDelegate> {
  UniversalResultParser *resultParser;
  NSArray *actions;
  ParsedResult *result;
}

@property(nonatomic,retain) UniversalResultParser *resultParser;
@property (nonatomic,assign) NSArray *actions;
@property (nonatomic,assign) ParsedResult *result;

- (IBAction)scan:(id)sender;
- (IBAction)info:(id)sender;
- (IBAction)showArchive:(id)sender;

- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)result;
- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller;
- (void)performResultAction;


@end
