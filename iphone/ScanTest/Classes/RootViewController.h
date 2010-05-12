//
//  RootViewController.h
//  ScanTest
//
//  Created by David Kavanagh on 5/10/10.
//  Copyright __MyCompanyName__ 2010. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "ZXingWidgetController.h"

@interface RootViewController : UIViewController <ZXingDelegate> {
	IBOutlet UITextView *resultsView;
	ZXingWidgetController *scanController;
}
@property (nonatomic, assign) IBOutlet UITextView *resultsView;

- (IBAction)scanPressed:(id)sender;
@end
