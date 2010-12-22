//
//  BarcodesAppDelegate.h
//  Barcodes
//
//  Created by Romain Pechayre on 11/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface BarcodesAppDelegate : NSObject <UIApplicationDelegate> {
  UIWindow *window;
  UITabBarController *tabBarController;
  
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet UITabBarController *tabBarController;
@end

