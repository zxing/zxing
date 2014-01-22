#!/usr/bin/env jruby --headless -rubygems
#
# Copyright 2013 ZXing authors
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

require File.expand_path( File.dirname(__FILE__) + '/../test_helper')
require 'zxing/decodable'

class DecodableTest < Test::Unit::TestCase

  class Object::File
    include Decodable
  end

  class URL
    include Decodable
    def initialize(path)
      @path = path
    end
    def path; @path end
  end

  context "A Decodable module" do
    setup do
      @file = File.open( File.expand_path( File.dirname(__FILE__) + '/../qrcode.png' ))
      @uri = URL.new "http://2d-code.co.uk/images/bbc-logo-in-qr-code.gif"
      @bad_uri = URL.new "http://google.com"
    end

    should "provide #decode to decode the return value of #path" do
      assert_equal @file.decode, ZXing.decode(@file.path)
      assert_equal @uri.decode, ZXing.decode(@uri.path)
      assert_nil @bad_uri.decode
    end

    should "provide #decode! as well" do
      assert_equal @file.decode!, ZXing.decode(@file.path)
      assert_equal @uri.decode!, ZXing.decode(@uri.path)
      assert_raise(NativeException) { @bad_uri.decode! }
    end
  end

end
