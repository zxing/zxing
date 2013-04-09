#!/usr/bin/env bash

blackboxpath="../core/test/data/blackbox"

if [ "$*" != "" ]; then
	formats="$*"
else
	formats="ean13 ean8 upce upca qrcode aztec"
fi

passed=0;
failed=0;
oldcat="";

for format in $formats; do
	for pic in `ls ${blackboxpath}/${format}-*/*.{jpg,JPG,gif,GIF,png,PNG} 2>/dev/null | sort -n`; do
		category=${pic%/*};
		category=${category##*/};
		if [ "$oldcat" != "$category" ]; then
			echo "***** $oldcat finished - $passed of $((passed+failed)) passed **** ***** ******* ***** *********************"
			oldcat=$category;
			passed=0;
			failed=0;
		fi
		echo -n "Processing: $pic ... "
		tmp="${pic}"
		tmp="${tmp%JPG}";
		tmp="${tmp%jpg}";
		tmp="${tmp%gif}";
		tmp="${tmp%GIF}";
		tmp="${tmp%png}";
		tmp="${tmp%PNG}";
		txt="${tmp}txt";
		expected=`cat "$txt"`;
		actual=`$VALGRIND build/zxing $pic`;
		if [ "$expected" == "$actual" ]; then
			echo "passed."
			passed=$((passed+1));
		else
			echo -e "FAILED\n   Expected: $expected\n   Detected: $actual"
			failed=$((failed+1));
		fi
	done
done
echo "***** $oldcat finished - $passed of $((passed+failed)) passed **** ***** ******* ***** *********************"
