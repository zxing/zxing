#
# CMake listfile to specify the build process, see:
# http://www.cmake.org/cmake/help/documentation.html
#
project(zxing)
cmake_minimum_required(VERSION 2.8.0)

set(CMAKE_LIBRARY_PATH /opt/local/lib ${CMAKE_LIBRARY_PATH})

# Check for polluted source tree.
if(EXISTS ${CMAKE_SOURCE_DIR}/CMakeCache.txt OR
    EXISTS ${CMAKE_SOURCE_DIR}/CMakeFiles)
    message(FATAL_ERROR
        "Source directory is polluted:"
        "\n  * remove CMakeCache.txt"
        "\n  * remove CMakeFiles directory")
endif()

# Suppress in-source builds.
if(${CMAKE_SOURCE_DIR} STREQUAL ${CMAKE_BINARY_DIR})
    message(FATAL_ERROR
        "CMake generation is not allowed within the source directory:"
        "\n  * mkdir build"
        "\n  * cd build"
        "\n  * Unix-like: cmake -G \"Unix Makefiles\" .."
        "\n  * Windows: cmake -G \"Visual Studio 10\" ..")
endif()

# Adjust CMake's module path.
set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake/)

# Suppress MSVC CRT warnings.
if(MSVC)
    add_definitions(-D_CRT_SECURE_NO_WARNINGS)
    add_definitions(/Za)
endif()

# Add libzxing library.
file(GLOB_RECURSE LIBZXING_FILES
    "./core/src/*.cpp"
    "./core/src/*.h"
    "./core/src/*.cc"
    "./core/src/*.hh"
)
if(WIN32)
    file(GLOB LIBZXING_WIN32_FILES
        "./core/lib/win32/*.c"
        "./core/lib/win32/*.h"
    )
    set(LIBZXING_FILES ${LIBZXING_FILES} ${LIBZXING_WIN32_FILES})
    include_directories(SYSTEM "./core/lib/win32/")
endif()
include_directories("./core/src/")
add_library(libzxing STATIC ${LIBZXING_FILES})
set_target_properties(libzxing PROPERTIES PREFIX "")
find_package(Iconv)
if(ICONV_FOUND)
    include_directories(${ICONV_INCLUDE_DIR})
    target_link_libraries(libzxing ${ICONV_LIBRARIES})
else()
    add_definitions(-DNO_ICONV=1)
endif()

# Add cli executable.
file(GLOB_RECURSE ZXING_FILES
    "./cli/src/*.cpp"
    "./cli/src/*.h"
)
add_executable(zxing ${ZXING_FILES})
target_link_libraries(zxing libzxing)

# Add testrunner executable.
find_package(CPPUNIT)
if(CPPUNIT_FOUND)
    file(GLOB_RECURSE TESTRUNNER_FILES
        "./core/tests/src/*.cpp"
        "./core/tests/src/*.h"
    )
    add_executable(testrunner ${TESTRUNNER_FILES})
    include_directories(${CPPUNIT_INCLUDE_DIR})
    target_link_libraries(testrunner libzxing  ${CPPUNIT_LIBRARIES})
else()
    message(WARNING "Not building testrunner, because CppUnit is missing")
endif()
