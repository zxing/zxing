#!/usr/bin/env jruby --headless -rubygems

require File.expand_path( File.dirname(__FILE__) + '/test_helper')
require 'zxing'

class ZXingTest < Test::Unit::TestCase
  context "A QR decoder singleton" do

    class Foo < Struct.new(:v); def to_s; self.v; end; end

    setup do
      @decoder = ZXing
      @uri = "http://2d-code.co.uk/images/bbc-logo-in-qr-code.gif"
      @path = File.expand_path( File.dirname(__FILE__) + '/qrcode.png')
      @file = File.new(@path)
      @google_logo = "http://www.google.com/logos/grandparentsday10.gif"
      @uri_result = "http://bbc.co.uk/programmes"
      @path_result = "http://rubyflow.com"
    end

    should "decode a URL" do
      assert_equal @decoder.decode(@uri), @uri_result
    end

    should "decode a file path" do
      assert_equal @decoder.decode(@path), @path_result
    end

    should "return nil if #decode fails" do
      assert_nil @decoder.decode(@google_logo)
    end

    should "raise an exception if #decode! fails" do
      assert_raise(NativeException) { @decoder.decode!(@google_logo) }
    end

    should "decode objects that respond to #path" do
      assert_equal @decoder.decode(@file), @path_result
    end

    should "call #to_s to argument passed in as a last resort" do
      assert_equal @decoder.decode(Foo.new(@path)), @path_result
    end
  end

  context "A QR decoder module" do
    
    setup do
      class SpyRing; include ZXing end
      @ring = SpyRing.new
    end

    should "include #decode and #decode! into classes" do
      assert_equal defined?(@ring.decode), "method"
      assert_equal defined?(@ring.decode!), "method"
    end

  end
end
