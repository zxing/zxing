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
| android             | Android client, [Barcode Scanner](https://play.google.com/store/apps/details?id=com.google.zxing.client.android)                                     |
| androidtest         | Android test app, ZXing Test
| android-integration | Supports integration with Barcode Scanner via `Intent`
| zxingorg            | The source behind `zxing.org`
| zxing.appspot.com   | The source behind web-based barcode generator at `zxing.appspot.com`

Try the [web-based QR Code generator](http://zxing.appspot.com/generator) and
[web-based barcode decoder](http://zxing.org/w/).

### Intermittently maintained

There are also additional modules which are contributed and/or intermittently maintained:

| Module       | Description
| ------------ | -----------
| actionscript | partial port to Actionscript
| glass-mirror | partial implementation for the Google Glass Mirror API
| jruby        | JRuby wrapper


### Available in previous releases

| Module | Description
| ------ | -----------
| cpp    | C++ port
| iphone | iPhone client
| objc   | Objective C port

### Related third-party open source projects

| Module                                             | Description
| -------------------------------------------------- | -----------
| [QZXing](https://sourceforge.net/projects/qzxing)  | port to Qt framework
| [ZXing .NET](http://zxingnet.codeplex.com/)        | port to .NET and C#, and related Windows platform

### Other third-party open source projects

| Module                                        | Description
| --------------------------------------------- | -----------
| [ZBar](http://zbar.sourceforge.net/)          | Decoder in C++, especially for iPhone
| [Zint](http://sourceforge.net/projects/zint/) | Barcode generator

## Contacting

Post to the [discussion forum](https://groups.google.com/group/zxing) or tag a question with [`zxing`
on StackOverflow](http://stackoverflow.com/questions/tagged/zxing).

## Etcetera

QR code is trademarked by Denso Wave, inc. Thanks to Haase & Martin OHG for contributing the logo.
Optimized with [http://www.ej-technologies.com/products/jprofiler/overview.html](JProfiler)
![JProfiler](http://www.ej-technologies.com/images/banners/jprofiler_small.png)
