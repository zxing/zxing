/**
 * Copyright 2009 Jeff Verkoeyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <UIKit/UIKit.h>

@protocol CancelDelegate;

@interface OverlayView : UIView {
	UIImageView *imageView;
	NSArray *_points;
	UIButton *cancelButton;
	id<CancelDelegate> delegate;
}

//@property (nonatomic, retain)   UIImage*  image;
@property (nonatomic, retain) NSArray*  points;
@property (nonatomic, assign) id<CancelDelegate> delegate;

- (id)initWithCancelEnabled:(BOOL)cancelEnabled frame:(CGRect)frame;

- (CGRect)cropRect;

@end

@protocol CancelDelegate
- (void)cancelled;
@end