# ZXing iPhone/iPad README #

ZXing for iOS is a sub-project of zxing project partially maintained
by independent developers. As of 26th of March 2011, it contains 3 iOS
projects:

* Barcodes:  Zxing iOS app. Available on the app store. 
* ZXingWidget: a Library that can be included in any iOS app.
* ScanTest: a simple demo app for ZXingWidget.

## How to include ZXingWidget in a easy and clean way (in Xcode4) ##

1. Locate the `ZXingWidget.xcodeproj` file under
     `zxing/iphone/ZXingWidget/`. Drag `ZXingWidget.xcodeproj` and
     drop it onto the root of your Xcode project sidebar.  A dialog
     will appear &mdash; make sure _Copy items_ is unchecked and _Reference
     Type_ is _Relative to Project_ before clicking
     _Add_. Alternatively you can right-click on you project navigator
     and select 'Add files to _your project_'.
   
2. Now you need to link the ZXingWidget static library to your project.  To do that

    a. select you project file in the project navigator

    b. In the second column, select your target (not the project itself)

    c. Go to the _Build Phases_ tab, expand the _Link Binary with Libraries_ section

    d. Click the _Add_ button. A dialog will appear and you should see
    `libZXingWidget.a` at or near the top of the list
  
3. Now you need to add ZXingWidget as a dependency of your project, so
   Xcode compiles it whenever you compile your project.

    a. Like in substep **c** of previous step, you need to do that in the _Build Phases_ tab of your target

    b. Expand the _Target Dependencies_ section

    c. Click the _Add_ button and a dialog will appear. Select `ZXingWidget` target.
  
4. Add the `ZXingWidget` to the _Headers Search Path_.

    a. Select your project in the 
     project navigator and then select your target.

    b. Go to the _Build Settings_ tab, look for _Header Search Paths_ and double-click
	 it.

    c: Add the relative path from your project's directory to the
	`zxing/iphone/ZXingWidget/Classes` directory and select the _Recursive Path_ checkbox.
  
5. Add the C++ core to the _Headers Search Path_. Repeat step **5**, but add the path to the C++ core: `zxing/cpp/core/src`. Do _not_ select the _Recursive Path_ checkbox.
 
6. Import the following iOS frameworks: 
    * AVFoundation
    * AudioToolbox
    * CoreVideo
    * CoreMedia
    * libiconv
    * AddressBook
    * AddressBookUI

	This must be done by adding them in the _Link Libraries with
           Binary_ just like step **2.c**. If you are supporting iOS 4,
           for AVFoundation, you must select _Optional_ in the pull
           down to the right in order to weak link the
           framework. (Some symbols used in ZXing didn't exist before
           iOS 5.)
 
7. `#import <ZXingWidgetController.h>` in a source file
  
8. `#import <QRCodeReader.h>` or `#import <MultiFormatReader.h>` for example because you will need to
   inject a barcode reader into `ZXingWidgetController`.

9. Make sure the file in which you are importing the code headers has a `.mm` extension to enable importing
C++.

10. Rename your `main.m` to `main.mm` to link against the C++ standard library.

11. Make sure the C++ library in _Build Settings_ is _Compiler
    Default_. Some versions of Xcode set this to the LLVM standard
    library. Alternately, you can try setting the C++ library to the
    LLVM version int the `XingWidget` project but this is not
    well-tested.

## Known issues for above steps to include: ##

* If you have building error like " ... : No such file or directory",
    then it is a classical error, it means that the include path is
    not well specified. The best way to fix this is to look at the
    build command that failed. To do that, click on the lower right
    corner of your Xcode project, you should see the build command
    that failed. Click on it and expand it by clicking on the _more_
    symbols that just appeared. This will show the exact command line
    instruction that failed. You can then make sure that the header
    search path you specified is there, and you can also copy/paste
    this line into your terminal and try to see if you can
    reproduce/fix the error by adding the right path to the
    compiler. Once is is fixed, you should have an idea of what's the
    problem and accordingly modify your Header Search Path.
