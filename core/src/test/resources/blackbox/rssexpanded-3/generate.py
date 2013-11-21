#!/usr/bin/env python

import os
import glob
import sys
import subprocess
import traceback
import hashlib
import urllib
import urllib2

ZINT = 'zint'
ZINT_RSS_EXPANDED_CODE = 31
ZINT_BINARY = "zint" # If available in PATH

POSTSCRIPT = 'postscript'

TEST_FILE = "../../../src/com/google/zxing/oned/rss/RSSExpandedBlackBox3TestCase.java"

def check_zint():
    try:
        subprocess.Popen("zint", stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    except:
        print >> sys.stderr, "zint not installed. Go to http://www.zint.org.uk/ and install it"
        return False
    return True

def check_postscript():
    return True

GENERATORS = []
if check_zint():
    GENERATORS.append(ZINT)
if check_postscript():
    GENERATORS.append(POSTSCRIPT)

def generate_image_zint(image_filename, barcode):
    braces_barcode = barcode.replace("(","[").replace(")","]")
    os.system('%s -o %s -b %s -d "%s"' % (ZINT_BINARY, image_filename, ZINT_RSS_EXPANDED_CODE, braces_barcode.replace('"','\\"')))

def generate_image_postscript(image_filename, barcode):
    hashname = "cache/%s.png" % hashlib.new("md5", barcode).hexdigest()
    # If it exists in the cache, don't download it
    if os.path.exists(hashname):
        content = open(hashname).read()
        open(image_filename,'w').write(content)
        return

    # We tried use the Python, Perl and LaTeX bindings without success :-(
    baseurl = "http://www.terryburton.co.uk/barcodewriter/generator/"
    encoded = urllib.urlencode({
             "encoder"     : "rssexpanded",
             "data"        : barcode,
             "options"     : "",
             "translate_x" : "50", "translate_y" : "50",
             "scale_x"     : "2", "scale_y"     : "2",
             "rotate"      : "0", "submit"      : "Make Barcode"
         })
    web_urlobject = urllib2.urlopen(baseurl, data = encoded)
    web_content   = web_urlobject.read()
    png_url       = web_content.split('">PNG</a>')[0].split('<a href="')[-1]
    if not png_url.startswith("tmp"):
        raise Exception("There was an error processing barcode %s in postscript" % barcode)
    full_url      = baseurl + png_url
    png_content   = urllib2.urlopen(full_url).read()
    open(hashname,'w').write(png_content)
    open(image_filename,'w').write(png_content)

def generate_image(image_filename, barcode, generator):
    if generator == ZINT:
        generate_image_zint(image_filename, barcode)
    elif generator == POSTSCRIPT:
        generate_image_postscript(image_filename, barcode)
    else:
        raise Exception("Unknown generator: %s" % generator)

def extract_barcodes():
    for line in open("generation.config"):
        content = line.split("#")[0].strip()
        if content != "":
            if content[0] == '-':
                pos = content[1:].find('-')
                exceptions = content[1:pos+1].split(",")
                barcode = content[pos + 2:]
                yield exceptions, barcode
            else:
                yield (), content

if __name__ == '__main__':
    counter = 0

    for image_to_delete in glob.glob("*.png"):
        os.remove(image_to_delete)

    for text_to_delete in glob.glob("*.txt"):
        os.remove(text_to_delete)

    for generator in GENERATORS:
        for exceptions, barcode in extract_barcodes():
            if generator in exceptions:
                continue
            try:
                counter += 1
                image_filename = str(counter) + ".png"
                text_filename  = str(counter) + ".txt"
                open(text_filename, "w").write(barcode)
                generate_image(image_filename, barcode, generator)
            except Exception:
                print "Error with generator %s and barcode %s" % (generator, barcode)
                traceback.print_exc()
                counter -= 1

    open(TEST_FILE,'w').write("""package com.google.zxing.oned.rss;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.AbstractBlackBoxTestCase;

public class RSSExpandedBlackBox3TestCase extends AbstractBlackBoxTestCase {
    
    public RSSExpandedBlackBox3TestCase() {
        super("test/data/blackbox/rssexpanded-3", new MultiFormatReader(), BarcodeFormat.RSS_EXPANDED);
        addTest(%(number)s, %(number)s, 0.0f);
        addTest(%(number)s, %(number)s, 180.0f);
    }
}
    """ % {"number" : counter})

