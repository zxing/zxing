<img align="right" src="https://raw.github.com/wiki/zxing/zxing/zxing-logo.png"/>

ZXing ("zebra crossing") is an open-source, multi-format 1D/2D barcode image processing
library implemented in Java, with ports to other languages.

## Supported Formats

| 1D product | 1D industrial | 2D
| ---------- | ------------- | --------------
| UPC-A      | Code 39       | QR Code
| UPC-E      | Code 93       | Data Matrix
| EAN-8      | Code 128      | Aztec (beta)
| EAN-13     | Codabar       | PDF 417 (beta)
|            | ITF           |
|            | RSS-14        |
|            | RSS-Expanded  |

## Components

### Active

| Module              | Description
| ------------------- | -----------
| core                | The core image decoding library, and test code
| javase              | JavaSE-specific client code
| android             | Android client Barcode Scanner [![Barcode Scanner](http://www.android.com/images/brand/android_app_on_play_logo_small.png)](https://play.google.com/store/apps/details?id=com.google.zxing.client.android)
| androidtest         | Android test app, ZXing Test
| android-integration | Supports integration with Barcode Scanner via `Intent`
| android-core        | Android-related code shared among `android`, `androidtest`, `glass`
| glass               | Simple Google Glass application
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

| Module                                                          | Description
| --------------------------------------------------------------- | -----------
| [QZXing](https://sourceforge.net/projects/qzxing)               | port to Qt framework
| [zxing-cpp](https://github.com/glassechidna/zxing-cpp)          | port to C++ (forked from the [deprecated official C++ port](https://github.com/zxing/zxing/tree/00f634024ceeee591f54e6984ea7dd666fab22ae/cpp))
| [zxing_cpp.rb](https://github.com/glassechidna/zxing_cpp.rb)    | bindings for Ruby (not just JRuby), powered by [zxing-cpp](https://github.com/glassechidna/zxing-cpp)
| [ZXing .NET](http://zxingnet.codeplex.com/)                     | port to .NET and C#, and related Windows platform

### Other related third-party open source projects

| Module                                         | Description
| ---------------------------------------------- | -----------
| [Barcode4J](http://barcode4j.sourceforge.net/) | Generator library in Java
| [ZBar](http://zbar.sourceforge.net/)           | Reader library in C99
| [Zint](http://sourceforge.net/projects/zint/)  | Generator library in C99

## Links

* [Online Decoder](http://zxing.org/w/decode.jspx)
* [QR Code Generator](http://zxing.appspot.com/generator)
* [Javadoc](http://zxing.github.io/zxing/apidocs/)
* [Documentation Site](http://zxing.github.io/zxing/)
* [Google+](https://plus.google.com/u/0/b/105889184633382354358/105889184633382354358/posts)

## Contacting

Post to the [discussion forum](https://groups.google.com/group/zxing) or tag a question with [`zxing`
on StackOverflow](http://stackoverflow.com/questions/tagged/zxing).

## Etcetera

[![Build Status](https://travis-ci.org/zxing/zxing.png?branch=master)](https://travis-ci.org/zxing/zxing)
[![Coverity Status](https://scan.coverity.com/projects/1924/badge.svg)](https://scan.coverity.com/projects/1924)
[![codecov.io](https://codecov.io/github/zxing/zxing/coverage.svg?branch=master)](https://codecov.io/github/zxing/zxing?branch=master)

QR code is trademarked by Denso Wave, inc. Thanks to Haase & Martin OHG for contributing the logo.

Optimized with [![JProfiler](http://www.ej-technologies.com/images/banners/jprofiler_small.png)](http://www.ej-technologies.com/products/jprofiler/overview.html)
