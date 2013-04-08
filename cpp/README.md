# ZXing C++ Port

This is a manual port of ZXing to C++. It has been tested on Linux, Mac OS X and Windows.

## Building using SCons

SCons is a build tool written in Python. You'll need to have Python
installed, but scons installation is optional: a run time copy of
SCons (called `scons-local`) is included. To use the included copy,
replace `scons` with `python scons/scons.py` in the instructions below.

To build the library only:

  1. Install libiconv (optional; included in many operating systems)
  2. `cd` to the `cpp` folder
  3. Run `scons lib`

To build the command line utility utility:
 
  1. Run `scons zxing`
  2. Run `build/zxing` for a command line reference

To build the unit tests (optional):

  1. Install CppUnit (`libcppunit-dev` on Ubuntu)
  2. Run `scons tests`
  3. Run `build/testrunner`

To clean:

  1. Run `scons -c all`

# Building using CMake

CMake is a tool, that generates native makefiles and workspaces. It
integrates well with a number of IDEs including Qt Creator and Visual
Studio.

Usage with Qt Creator:

  1. Open `CMakeLists.txt` as new project
  2. Specify command line arguments (see below) and press _Finish_

Usage with Makefiles, Visual Studio, etc. (see `cmake --help` for a complete list of generators):

  1. `cd` to `cpp/build`
  3. Unix: run `cmake -G "Unix Makefiles" ..`
  3. Windows: run `cmake -G "Visual Studio 10" ..`
  
You can switch between build modes by specifying:

  - `-DCMAKE_BUILD_TYPE=Debug` or
  - `-DCMAKE_BUILD_TYPE=Release`

# Development tips

To profile the code (very useful to optimize the code):

  1. Install Valgrind
  2. Run `valgrind --tool=callgrind build/zxing - path/to/test/data/*.jpg > report.html`
  3. Analyze output using KCachegrind

To run the black box tests and check for changes:

  1. Build `zxing-img`, e.g., scons zxing
  2. Run the tests: `bash blackboxtest.sh 2>&1 | tee bb.results`
  3. Diff them with the known results: `diff bb.results blackboxtest.results`
