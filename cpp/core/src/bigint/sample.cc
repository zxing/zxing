// Sample program demonstrating the use of the Big Integer Library.

// Standard libraries
#include <string>
#include <iostream>

// `BigIntegerLibrary.hh' includes all of the library headers.
#include "BigIntegerLibrary.hh"

int main() {
	/* The library throws `const char *' error messages when things go
	 * wrong.  It's a good idea to catch them using a `try' block like this
	 * one.  Your C++ compiler might need a command-line option to compile
	 * code that uses exceptions. */
	try {
		BigInteger a; // a is 0
		int b = 535;

		/* Any primitive integer can be converted implicitly to a
		 * BigInteger. */
		a = b;

		/* The reverse conversion requires a method call (implicit
		 * conversions were previously supported but caused trouble).
		 * If a were too big for an int, the library would throw an
		 * exception. */
		b = a.toInt();

		BigInteger c(a); // Copy a BigInteger.

		// The int literal is converted to a BigInteger.
		BigInteger d(-314159265);

		/* This won't compile (at least on 32-bit machines) because the
		 * number is too big to be a primitive integer literal, and
		 * there's no such thing as a BigInteger literal. */
		//BigInteger e(3141592653589793238462643383279);

		// Instead you can convert the number from a string.
		std::string s("3141592653589793238462643383279");
		BigInteger f = stringToBigInteger(s);

		// You can convert the other way too.
		std::string s2 = bigIntegerToString(f); 

		// f is implicitly stringified and sent to std::cout.
		std::cout << f << std::endl;

		/* Let's do some math!  The library overloads most of the
		 * mathematical operators (including assignment operators) to
		 * work on BigIntegers.  There are also ``copy-less''
		 * operations; see `BigUnsigned.hh' for details. */

		// Arithmetic operators
		BigInteger g(314159), h(265);
		std::cout << (g + h) << '\n'
			<< (g - h) << '\n'
			<< (g * h) << '\n'
			<< (g / h) << '\n'
			<< (g % h) << std::endl;

		// Bitwise operators
		BigUnsigned i(0xFF0000FF), j(0x0000FFFF);
		// The library's << operator recognizes base flags.
		std::cout.flags(std::ios::hex | std::ios::showbase);
		std::cout << (i & j) << '\n'
			<< (i | j) << '\n'
			<< (i ^ j) << '\n'
			// Shift distances are ordinary unsigned ints.
			<< (j << 21) << '\n'
			<< (j >> 10) << '\n';
		std::cout.flags(std::ios::dec);

		// Let's do some heavy lifting and calculate powers of 314.
		int maxPower = 10;
		BigUnsigned x(1), big314(314);
		for (int power = 0; power <= maxPower; power++) {
			std::cout << "314^" << power << " = " << x << std::endl;
			x *= big314; // A BigInteger assignment operator
		}

		// Some big-integer algorithms (albeit on small integers).
		std::cout << gcd(BigUnsigned(60), 72) << '\n'
			<< modinv(BigUnsigned(7), 11) << '\n'
			<< modexp(BigUnsigned(314), 159, 2653) << std::endl;

		// Add your own code here to experiment with the library.
	} catch(char const* err) {
		std::cout << "The library threw an exception:\n"
			<< err << std::endl;
	}

	return 0;
}

/*
The original sample program produces this output:

3141592653589793238462643383279
314424
313894
83252135
1185
134
0xFF
0xFF00FFFF
0xFF00FF00
0x1FFFE00000
0x3F
314^0 = 1
314^1 = 314
314^2 = 98596
314^3 = 30959144
314^4 = 9721171216
314^5 = 3052447761824
314^6 = 958468597212736
314^7 = 300959139524799104
314^8 = 94501169810786918656
314^9 = 29673367320587092457984
314^10 = 9317437338664347031806976
12
8
1931

*/
