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

| *core*                | The core image decoding library, and test code
| *javase*              | JavaSE-specific client code
| *android*             | Android client, <a href="https://play.google.com/store/apps/details?id=com.google.zxing.client.android">Barcode Scanner</a>                                      |
| *androidtest*         | Android test app, ZXing Test
| *android-integration* | Supports integration with Barcode Scanner via `Intent`
| *zxingorg*            | The source behind `zxing.org`
| *zxing.appspot.com*   | The source behind web-based barcode generator at `zxing.appspot.com`

Try the [http://zxing.appspot.com/generator web-based QR Code generator] and
[http://zxing.org/w/ web-based barcode decoder].

### Intermittently maintained

There are also additional modules which are contributed and/or intermittently maintained:

| *actionscript* | partial port to Actionscript
| *glass-mirror* | partial implementation for the Google Glass Mirror API
| *jruby*        | JRuby wrapper


### Available in previous releases

| *cpp*    | C++ port
| *iphone* | iPhone client
| *objc*   | Objective C port

### Related third-party open source projects

| *[https://sourceforge.net/projects/qzxing QZXing]* | port to Qt framework
| *[http://zxingnet.codeplex.com/ ZXing .NET]* | port to .NET and C#, and related Windows platform

### Other third-party open source projects

| *[http://zbar.sourceforge.net/ ZBar]*          | Decoder in C++, especially for iPhone
| *[http://sourceforge.net/projects/zint/ Zint]* | Barcode generator


## Contacting

Post to the [https://groups.google.com/group/zxing discussion forum] or tag a question with `zxing`
on [http://stackoverflow.com/questions/tagged/zxing StackOverflow].

## Etcetera

_QR code is trademarked by Denso Wave, inc. Thanks to Haase & Martin OHG for contributing the logo._

Optimized with
<a href="http://www.ej-technologies.com/products/jprofiler/overview.html">
<img src="http://www.ej-technologies.com/images/banners/jprofiler_small.png"/></a>

<script type="text/javascript" src="http://www.ohloh.net/p/12084/widgets/project_factoids_stats.js"></script>