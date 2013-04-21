# Mention default target.
all:

# Implicit rule to compile C++ files.  Modify to your taste.
%.o: %.cc
	g++ -c -O2 -Wall -Wextra -pedantic $<

# Components of the library.
library-objects = \
	BigUnsigned.o \
	BigInteger.o \
	BigIntegerAlgorithms.o \
	BigUnsignedInABase.o \
	BigIntegerUtils.o \

library-headers = \
	NumberlikeArray.hh \
	BigUnsigned.hh \
	BigInteger.hh \
	BigIntegerAlgorithms.hh \
	BigUnsignedInABase.hh \
	BigIntegerLibrary.hh \

# To ``make the library'', make all its objects using the implicit rule.
library: $(library-objects)

# Conservatively assume that all the objects depend on all the headers.
$(library-objects): $(library-headers)

# TESTSUITE (NOTE: Currently expects a 32-bit system)
# Compiling the testsuite.
testsuite.o: $(library-headers)
testsuite: testsuite.o $(library-objects)
	g++ $^ -o $@
# Extract the expected output from the testsuite source.
testsuite.expected: testsuite.cc
	nl -ba -p -s: $< | sed -nre 's,^ +([0-9]+):.*//([^ ]),Line \1: \2,p' >$@
# Run the testsuite.
.PHONY: test
test: testsuite testsuite.expected
	./run-testsuite
testsuite-cleanfiles = \
	testsuite.o testsuite testsuite.expected \
	testsuite.out testsuite.err

# The rules below build a program that uses the library.  They are preset to
# build ``sample'' from ``sample.cc''.  You can change the name(s) of the
# source file(s) and program file to build your own program, or you can write
# your own Makefile.

# Components of the program.
program = sample
program-objects = sample.o

# Conservatively assume all the program source files depend on all the library
# headers.  You can change this if it is not the case.
$(program-objects) : $(library-headers)

# How to link the program.  The implicit rule covers individual objects.
$(program) : $(program-objects) $(library-objects)
	g++ $^ -o $@

# Delete all generated files we know about.
clean :
	rm -f $(library-objects) $(testsuite-cleanfiles) $(program-objects) $(program)

# I removed the *.tag dependency tracking system because it had few advantages
# over manually entering all the dependencies.  If there were a portable,
# reliable dependency tracking system, I'd use it, but I know of no such;
# cons and depcomp are almost good enough.

# Come back and define default target.
all : library $(program)
