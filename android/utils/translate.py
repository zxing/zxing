#!/usr/bin/env python
#
# Copyright (C) 2010 ZXing authors
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

# Translate a string from English to all locales used in the Barcode
# Scanner Android project
#
# Author: Neha Pandey

from urllib2 import urlopen
from urllib import urlencode
import sys

def translate (in_lang, out_lang, input):
    """Translate the input from in_lang to out_lang using Google Translate"""
    # Create the URL
    langpair = '%s|%s' % (in_lang, out_lang)
    base = 'http://ajax.googleapis.com/ajax/services/language/translate?'
    params = urlencode ((('v',1.0),
                         ('q',input),
                         ('langpair',langpair),) )
    url = base + params
    # Call translation
    content = urlopen(url).read()

    # Snip out unwanted fluff from the translation
    start_index = content.find('"translatedText":"') + 18
    translation = content [start_index:]
    end_index = translation.find('"}, "')
    output = translation[:end_index]
    return output

# All the languages to translate to
language_list = ['en', 'ar', 'cs', 'da', 'de', 'es',
                 'fi', 'fr', 'hu', 'it', 'ja', 'nl',
                 'pl', 'pt', 'ru', 'sv', 'zh-CN',
                 'zh-TW']

if (len(sys.argv) < 3):
    print "Usage: %s name String to translate" % sys.argv[0]
    print "Sample: %s ask-banana Give me a banana" % sys.argv[0]
    import sys
    sys.exit (-1);

# First argument is the name of the string
string_name = sys.argv[1]
# Remaining arguments is the string to be translated in English
input_string =' '.join(sys.argv[2:])

# Translate all languages
for i in range(len(language_list)) :
    translation = translate ('en', language_list[i], input_string)
    xml_string = '<string name="' + string_name + '">' + \
      translation + '</string>'
    print language_list[i], xml_string

