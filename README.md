<img align="right" src="https://raw.github.com/wiki/zxing/zxing/zxing-logo.png"/>

## Project in Maintenance Mode Only

The project is in maintenance mode, meaning, changes are driven by contributed patches.
Only bug fixes and minor enhancements will be considered. The Barcode Scanner app can
no longer be published, so it's unlikely any changes will be accepted for it.
There is otherwise no active development or roadmap for this project. It is "DIY".

## Get Started Developing

To get started, please visit: https://github.com/zxing/zxing/wiki/Getting-Started-Developing

ZXing ("zebra crossing") is an open-source, multi-format 1D/2D barcode image processing
library implemented in Java, with ports to other languages.

## Supported Formats

| 1D product            | 1D industrial | 2D             |
|:----------------------|:--------------|:---------------|
| UPC-A                 | Code 39       | QR Code        |
| UPC-E                 | Code 93       | Data Matrix    |
| EAN-8                 | Code 128      | Aztec          |
| EAN-13                | Codabar       | PDF 417        |
| UPC/EAN Extension 2/5 | ITF           | MaxiCode       |
|                       |               | RSS-14         |
|                       |               | RSS-Expanded   |

## Components

### Active

| Module              | Description
| ------------------- | -----------
| core                | The core image decoding library, and test code
| javase              | JavaSE-specific client code
| android             | Android client Barcode Scanner [<img height='62' width='161' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/>](https://play.google.com/store/apps/details?id=com.google.zxing.client.android)
| android-integration | Supports integration with Barcode Scanner via `Intent`
| android-core        | Android-related code shared among `android`, other Android apps
| zxingorg            | The source behind `zxing.org`
| zxing.appspot.com   | The source behind web-based barcode generator at `zxing.appspot.com`

### Available in previous releases

| Module | Description
| ------ | -----------
| [cpp](https://github.com/zxing/zxing/tree/00f634024ceeee591f54e6984ea7dd666fab22ae/cpp)                   | C++ port
| [iphone](https://github.com/zxing/zxing/tree/00f634024ceeee591f54e6984ea7dd666fab22ae/iphone)             | iPhone client
| [objc](https://github.com/zxing/zxing/tree/00f634024ceeee591f54e6984ea7dd666fab22ae/objc)                 | Objective C port
| [actionscript](https://github.com/zxing/zxing/tree/c1df162b95e07928afbd4830798cc1408af1ac67/actionscript) | Partial ActionScript port
| [jruby](https://github.com/zxing/zxing/tree/a95a8fee842f67fb43799a8e0e70e4c68b509c43/jruby)               | JRuby wrapper

### ZXing-based third-party open source projects

| Module                                                                                    | Description
| ----------------------------------------------------------------------------------------- | -----------
| [SCodes](https://github.com/scytheStudio/SCodes)                                          | Qt & Qml wrapper
| [QZXing](https://github.com/ftylitak/qzxing)                                              | port to Qt framework
| [glassechidna/zxing-cpp](https://github.com/glassechidna/zxing-cpp)                       | port to C++ (forked from the [deprecated official C++ port](https://github.com/zxing/zxing/tree/00f634024ceeee591f54e6984ea7dd666fab22ae/cpp))
| [zxing-cpp/zxing-cpp](https://github.com/zxing-cpp/zxing-cpp)                             | latest/active port to C++, includes Android, iOS, Python, WASM and WinRT wrappers
| [zxing_cpp.rb](https://github.com/glassechidna/zxing_cpp.rb)                              | bindings for Ruby (not just JRuby), powered by [zxing-cpp](https://github.com/glassechidna/zxing-cpp)
| [jsqrcode](https://github.com/LazarSoft/jsqrcode)                                         | port to JavaScript
| [python-zxing](https://github.com/oostendo/python-zxing)                                  | bindings for Python
| [ZXing .NET](https://github.com/micjahn/ZXing.Net)                                        | port to .NET and C#, and related Windows platform
| [php-qrcode-detector-decoder](https://github.com/khanamiryan/php-qrcode-detector-decoder) | port to PHP
| [ZXing Delphi](https://github.com/Spelt/ZXing.Delphi)                                     | Port to native Delphi object pascal, targeted at Firemonkey compatible devices (IOS/Android/Win/OSX) and VCL.
| [ZXingObjC](https://github.com/TheLevelUp/ZXingObjC)                                      | Port to Objective-C
| [php-zxing](https://github.com/dsiddharth2/php-zxing)                                     | PHP wrapper to Zxing Java library
| [zxing-js/library](https://github.com/zxing-js/library)                                   | TypeScript port of ZXing library
| [pyzxing](https://github.com/ChenjieXu/pyzxing)                                           | Python wrapper to ZXing library
| [zxing-dart](https://github.com/shirne/zxing-dart)                                        | Port to dart
| [rxing](https://github.com/rxing-core/rxing)                                                | Port to rust

### Other related third-party open source projects

| Module                                         | Description
| ---------------------------------------------- | -----------
| [Barcode4J](http://barcode4j.sourceforge.net/) | Generator library in Java
| [BoofCV](https://boofcv.org) | Generator and reader library in Java
| [ZBar](http://zbar.sourceforge.net/)           | Reader library in C99
| [OkapiBarcode](https://github.com/woo-j/OkapiBarcode)  | Generator library in Java |

## Links

  * [Online Decoder](https://zxing.org/w/decode.jspx)
  * [QR Code Generator](https://zxing.appspot.com/generator)
  * [Javadoc](https://zxing.github.io/zxing/apidocs/)
  * [Documentation Site](https://zxing.github.io/zxing/)

## Contacting

Post to the [discussion forum](https://groups.google.com/group/zxing) or tag a question with [`zxing`
on StackOverflow](https://stackoverflow.com/questions/tagged/zxing).

## Etcetera

[![codecov](https://codecov.io/gh/zxing/zxing/branch/master/graph/badge.svg?token=6RrJHvUMDl)](https://codecov.io/gh/zxing/zxing)
[![Codacy](https://app.codacy.com/project/badge/Grade/5d1186edeb714f0187e3eb18cc6aeef1)](https://www.codacy.com/gh/zxing/zxing/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zxing/zxing&amp;utm_campaign=Badge_Grade)

QR code is trademarked by Denso Wave, inc. Thanks to Haase & Martin OHG for contributing the logo.

Optimized with [![JProfiler](https://www.ej-technologies.com/images/banners/jprofiler_small.png)](https://www.ej-technologies.com/products/jprofiler/overview.html)
