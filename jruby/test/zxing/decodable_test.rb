#!/usr/bin/env jruby --headless -rubygems

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
