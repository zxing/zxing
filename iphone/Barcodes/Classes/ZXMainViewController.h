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
#import "ModalViewControllerDelegate.h"

@class UniversalResultParser;
@class ParsedResult;
@class ResultAction;


@interface ZXMainViewController : UIViewController <ZXingDelegate,UIActionSheetDelegate,ModalViewControllerDelegate,UIAlertViewDelegate> {
  NSArray *actions;
  ParsedResult *result;
}

@property (nonatomic,assign) NSArray *actions;
@property (nonatomic,assign) ParsedResult *result;

- (IBAction)scan:(id)sender;

- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)result;
- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller;
- (void)performResultAction;



@end
