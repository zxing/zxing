# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

CC=/usr/local/bin/arm-apple-darwin-gcc -v
LD=$(CC)

CFLAGS = -L$(HEAVENLY)
LDFLAGS = -framework CoreFoundation \
	  -framework Foundation \
          -framework UIKit \
          -framework CoreGraphics \
	  -framework PhotoLibrary \
          -lobjc

all:	ZXing

ZXing:	ZXing.o ZXingApplication.o
	$(LD) $(LDFLAGS) -o $@ $^

%.o:	%.m
	$(CC) -c $(CFLAGS) $< -o $@

clean:
	rm -f *.o ZXing
	rm -f ZXing.app/ZXing
