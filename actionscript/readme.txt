Thank you for your interest in the Actionscript3/Flex conversion of the zxing library.

The core folder contains the zxing library itself.
The zxing client folder contains a very basic client which will hopefully get you started.
There are some more comments in the zxing client project file.
Many improvements can be made and not all patches and updates have been applied to this conversion.
If you would like the help, feel free to join the zxing project and submit your improvements and bugfixes.
If you have questions, please send them to the zxing newsgroup.

Bas Vijfwinkel


Build Instructions
------------------

Building ZXing AS3 code requires:

* Flex4 SDK
  http://opensource.adobe.com/wiki/display/flexsdk/Download+Flex+4

* FlexUnit
  http://flexunit.org/releases/flexunit-4.1.0-8-4.1.0.16076.zip

* AS3Corelib
  https://github.com/downloads/mikechambers/as3corelib/as3corelib-.93.zip

* Apache Ant
  http://ant.apache.org/


Setup
-----

1. Please download and unzip Flex4 SDK.
2. Define FLEX_HOME environment variable with a full path to Flex4 SDK folder.

On Mac or Linux:

$ export FLEX_HOME=/specify-full-path-to/flex4_sdk

on Win:

$ set FLEX_HOME=/specify-full-path-to/flex4_sdk

3. Download and unzip FlexUnit and AS3Corelib to core/libs folder. Make sure
that all swc and jar files are located directly under

./zxing-read-only/actionscript/core/libs:

$ ls -l libs
-rw-r----- 1 john smith  193727 Jun 28 14:18 as3corelib.swc
-rw-r--r-- 1 john smith  597386 Apr 13 16:57 flexUnitTasks-4.1.0-8.jar
-rw-r--r-- 1 john smith  192629 Apr 13 16:57 flexunit-4.1.0-8-as3_4.1.0.16076.swc
-rw-r--r-- 1 john smith  203196 Apr 13 16:57 flexunit-4.1.0-8-flex_4.1.0.16076.swc
-rw-r--r-- 1 john smith    2379 Apr 13 17:01 flexunit-aircilistener-4.1.0-8-4.1.0.16076.swc
-rw-r--r-- 1 john smith   11993 Apr 13 16:58 flexunit-cilistener-4.1.0-8-4.1.0.16076.swc
-rw-r--r-- 1 john smith    9686 Apr 13 16:58 flexunit-flexcoverlistener-4.1.0-8-4.1.0.16076.swc
-rw-r--r-- 1 john smith  227292 Apr 13 17:01 flexunit-uilistener-4.1.0-8-4.1.0.16076.swc
-rw-r--r-- 1 john smith    6332 Apr 13 16:58 fluint-extensions-4.1.0-8-4.1.0.16076.swc

4. Download and unzip Apache Ant. Add ant tool to the PATH.


Building ZXing SWC
------------------

To build zxing.swc, run ant:

$ cd zxing-read-only/actionscript/core
$ ant

The build should create zxing-1.6.swc in actionscript/core/bin folder.


Building and Running ZXing Tests
--------------------------------

To build tests, execute:

$ ant compile.tests

To run the tests, open bin/output/ZXingTestsRunner.swf in a Flash Player or a
web browser. Here is an example of running the tests with the standalone
Flash Player debugger:

$ flashplayerdebugger bin/output/ZXingTestsRunner.swf

