//
//  ModalViewControllerDelegate.h
//  Barcodes
//
//  Created by Romain Pechayre on 11/16/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>


@protocol ModalViewControllerDelegate
- (void)modalViewControllerWantsToBeDismissed:(UIViewController *)controller;

@end
