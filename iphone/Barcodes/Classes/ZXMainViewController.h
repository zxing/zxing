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


@interface ZXMainViewController : UIViewController <ZXingDelegate,UIActionSheetDelegate,ModalViewControllerDelegate> {
  NSArray *actions;
  ParsedResult *result;
  IBOutlet UITextView *resultView;
  IBOutlet UIButton *lastActionButton;
}

@property (nonatomic,assign) NSArray *actions;
@property (nonatomic,assign) ParsedResult *result;
@property (nonatomic,retain) IBOutlet UITextView *resultView;
@property (nonatomic,retain) IBOutlet UIButton *lastActionButton;

- (IBAction)scan:(id)sender;
- (IBAction)info:(id)sender;
- (IBAction)showArchive:(id)sender;
- (IBAction)lastResultAction:(id)sender;

- (void)zxingController:(ZXingWidgetController*)controller didScanResult:(NSString *)result;
- (void)zxingControllerDidCancel:(ZXingWidgetController*)controller;
- (void)performResultAction;
- (void)setResultViewWithText:(NSString*)theResult;



@end
