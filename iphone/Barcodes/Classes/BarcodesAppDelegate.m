//
//  BarcodesAppDelegate.m
//  Barcodes
//
//  Created by Romain Pechayre on 11/14/10.
//  Copyright 2010 __MyCompanyName__. All rights reserved.
//

#import "BarcodesAppDelegate.h"

@implementation BarcodesAppDelegate

@synthesize window;
@synthesize tabBarController;
@synthesize viewController;

#pragma mark -
#pragma mark Application lifecycle

- (BOOL)myOpenURL:(NSURL*)url {
    if (!url) return NO;
    if ([[url scheme] isEqualToString:@"zxing"]) {
        if ([[url host] isEqualToString:@"scan"]) {
            NSArray *pairs = [[url query] componentsSeparatedByString:@"&amp;"];
        
            for (NSString *pair in pairs) {
                NSArray *elements = [pair componentsSeparatedByString:@"="];
                NSString *key = [[elements objectAtIndex:0] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
                NSString *val = [[elements objectAtIndex:1] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
            
                if ([key isEqualToString:@"ret"]) {
                    [[NSUserDefaults standardUserDefaults] setObject:val forKey:@"returnURL"];
                    [[NSUserDefaults standardUserDefaults] synchronize];
                    [[self viewController] scan:nil];
                }
                if ([key isEqualToString:@"SCAN_FORMATS"]) {
                    // Storing these, but they effect nothing yet.
                    NSArray *formats = [val componentsSeparatedByString:@","];
                    [[NSUserDefaults standardUserDefaults] setObject:formats forKey:@"scanFormats"];
                    [[NSUserDefaults standardUserDefaults] synchronize];
                }
            }
        }
        return YES;
    } else {
        return NO;
    }
}

- (void)registerView:(ZXMainViewController*)controller {
    [self setViewController:controller];
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {    
    
  // Check if launching from URL
  NSURL *url = [launchOptions objectForKey: UIApplicationLaunchOptionsURLKey];
  if ([url isMemberOfClass: [NSURL class]]) {
    return [self myOpenURL: url];
  } else {
    // Clear the return URL so the application goes back to working as normal...
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"returnURL"];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"scanFormats"];
    [[NSUserDefaults standardUserDefaults] synchronize];
  }
    
    // Override point for customization after application launch.
  [self.window addSubview:tabBarController.view];
  [self.window makeKeyAndVisible];
  
  return YES;
}

- (BOOL)application:(UIApplication *)application handleOpenURL:(NSURL *)url 
{
    return [self myOpenURL: url];
}

- (void)applicationWillResignActive:(UIApplication *)application {
    /*
     Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
     Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
     */
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    /*
     Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
     If your application supports background execution, called instead of applicationWillTerminate: when the user quits.
     */
    
    // Clear the return URL so the application goes back to working as normal...
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"returnURL"];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"scanFormats"];
    [[NSUserDefaults standardUserDefaults] synchronize];
}


- (void)applicationWillEnterForeground:(UIApplication *)application {
    /*
     Called as part of  transition from the background to the inactive state: here you can undo many of the changes made on entering the background.
     */
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    /*
     Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
     */
}


- (void)applicationWillTerminate:(UIApplication *)application {
    /*
     Called when the application is about to terminate.
     See also applicationDidEnterBackground:.
     */
}


#pragma mark -
#pragma mark Memory management

- (void)applicationDidReceiveMemoryWarning:(UIApplication *)application {
    /*
     Free up as much memory as possible by purging cached data objects that can be recreated (or reloaded from disk) later.
     */
}


- (void)dealloc {
  [tabBarController release];
  [window release];
  [super dealloc];
}


@end
