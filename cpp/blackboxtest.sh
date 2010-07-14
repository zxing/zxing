#!/bin/sh

blackboxpath="../core/test/data/blackbox"

formats="ean13 ean8 upce upca qrcode"

passed=0;
failed=0;
oldcat="";

for format in $formats; do
	for pic in `ls ${blackboxpath}/${format}-*/*.{jpg,JPG} 2>/dev/null`; do
		category=${pic%/*};
		category=${category##*/};
		if [ "$oldcat" != "$category" ]; then
			echo "***** $oldcat finished - $passed of $((passed+failed)) passed **** ***** ******* ***** *********************"
			oldcat=$category;
			passed=0;
			failed=0;
		fi
		echo -n "Processing: $pic ... "
		tmp="${pic%JPG}";
		txt="${tmp%jpg}txt";
		expected=`cat "$txt"`;
		actual=`build/zxing $pic`;
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
