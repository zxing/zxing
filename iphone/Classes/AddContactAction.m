//
//  AddContactAction.m
//  ZXing
//
//  Created by Christian Brunschen on 29/05/2008.
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

#import "AddContactAction.h"
#import "AddressBook/AddressBook.h"


@implementation AddContactAction

@synthesize name;
@synthesize phoneNumbers;
@synthesize note;
@synthesize email;
@synthesize urlString;
@synthesize address;

+ actionWithName:(NSString *)n
    phoneNumbers:(NSArray *)nums
           email:(NSString *)em
             url:(NSString *)us
         address:(NSString *)ad
            note:(NSString *)nt {
  AddContactAction *aca = [[[self alloc] init] autorelease];
  aca.name = n;
  aca.phoneNumbers = nums;
  aca.email = em;
  aca.urlString = us;
  aca.address = ad;
  aca.note = nt;
  return aca;
}

- (NSString *)title {
  return @"Add Contact";
}

- (void)performActionWithController:(UIViewController *)controller {
  CFErrorRef *error = NULL;
  NSCharacterSet *whitespaceSet = [NSCharacterSet whitespaceCharacterSet];
  
  ABRecordRef person = ABPersonCreate();
    
  NSRange commaRange = [name rangeOfString:@","];
  if (commaRange.location != NSNotFound) {
    NSString *lastName = [[name substringToIndex:commaRange.location] 
                          stringByTrimmingCharactersInSet:whitespaceSet];
    ABRecordSetValue(person, kABPersonLastNameProperty, lastName, error);
    NSArray *firstNames = [[[name substringFromIndex:commaRange.location + commaRange.length]
                            stringByTrimmingCharactersInSet:whitespaceSet] 
                           componentsSeparatedByCharactersInSet:whitespaceSet];
    ABRecordSetValue(person, kABPersonFirstNameProperty, [firstNames objectAtIndex:0], error);
    for (int i = 1; i < [firstNames count]; i++) {
      ABRecordSetValue(person, kABPersonMiddleNameProperty, [firstNames objectAtIndex:1], error);
    }
  } else {
    NSArray *nameParts = [name componentsSeparatedByCharactersInSet:whitespaceSet];
    int nParts = nameParts.count;
    if (nParts == 1) {
      ABRecordSetValue(person, kABPersonFirstNameProperty, name, error);
    } else if (nParts >= 2) {
      int lastPart = nParts - 1;
      ABRecordSetValue(person, kABPersonFirstNameProperty, [nameParts objectAtIndex:0], error);
      for (int i = 1; i < lastPart; i++) {
        ABRecordSetValue(person, kABPersonMiddleNameProperty, [nameParts objectAtIndex:i], error);
      }
      ABRecordSetValue(person, kABPersonLastNameProperty, [nameParts objectAtIndex:lastPart], error);
    }
  }
  
  if (self.note) {
    ABRecordSetValue(person, kABPersonNoteProperty, self.note, error);
  }
  
  if (self.phoneNumbers && self.phoneNumbers.count > 0) {
    // multi-values: nultiple phone numbers
    ABMutableMultiValueRef phoneNumberMultiValue = 
    ABMultiValueCreateMutable(kABStringPropertyType);
    for (NSString *number in self.phoneNumbers) {
      ABMultiValueAddValueAndLabel(phoneNumberMultiValue, number, 
                                   kABPersonPhoneMainLabel, NULL);
    }
    ABRecordSetValue(person, kABPersonPhoneProperty,
                     phoneNumberMultiValue, error);
  }
  
  if (self.email) {
    // a single email address
    ABMutableMultiValueRef emailMultiValue = 
    ABMultiValueCreateMutable(kABStringPropertyType);
    ABMultiValueAddValueAndLabel(emailMultiValue, self.email, 
                                 kABHomeLabel, NULL);
    ABRecordSetValue(person, kABPersonEmailProperty, emailMultiValue, error);
  }
  
  if (self.urlString) {
    // a single url as the home page
    ABMutableMultiValueRef urlMultiValue = 
    ABMultiValueCreateMutable(kABStringPropertyType);
    ABMultiValueAddValueAndLabel(urlMultiValue, self.urlString,
                                 kABPersonHomePageLabel, NULL);
    ABRecordSetValue(person, kABPersonURLProperty, urlMultiValue, error);
  }
  
  ABUnknownPersonViewController *unknownPersonViewController = 
  [[ABUnknownPersonViewController alloc] init];
  unknownPersonViewController.displayedPerson = person;
  unknownPersonViewController.allowsActions = true;
  unknownPersonViewController.allowsAddingToAddressBook = true;
  unknownPersonViewController.unknownPersonViewDelegate = self;
  CFRelease(person);

  viewController = [controller retain];
  [[viewController navigationController] pushViewController:unknownPersonViewController animated:YES];
  [unknownPersonViewController release];
}

- (void)dismissUnknownPersonViewController:(ABUnknownPersonViewController *)unknownPersonViewController {
  [[viewController navigationController] popToViewController:viewController animated:YES];
  [viewController release];
  viewController = nil;
}

// ABUnknownPersonViewControllerDelegate

- (void)unknownPersonViewController:(ABUnknownPersonViewController *)unknownPersonViewController
                 didResolveToPerson:(ABRecordRef)person {
  if (person) {
    [self performSelector:@selector(dismissUnknownPersonViewController:) withObject:unknownPersonViewController afterDelay:0.0];
  }
}
@end
