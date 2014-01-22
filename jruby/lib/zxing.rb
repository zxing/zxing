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

raise "ZXing requires JRuby" unless defined?(JRuby)

require File.expand_path( File.dirname(__FILE__) + '/core.jar' )    # ZXing core classes
require File.expand_path( File.dirname(__FILE__) + '/javase.jar' )  # ZXing JavaSE classes

require 'uri'

# Google ZXing classes
java_import com.google.zxing.MultiFormatReader
java_import com.google.zxing.BinaryBitmap
java_import com.google.zxing.Binarizer
java_import com.google.zxing.common.GlobalHistogramBinarizer
java_import com.google.zxing.LuminanceSource
java_import com.google.zxing.client.j2se.BufferedImageLuminanceSource

# Standard Java classes
java_import javax.imageio.ImageIO
java_import java.net.URL

module ZXing

  @@decoder = MultiFormatReader.new

  # Transform the module into a singleton!
  extend self

  def decode(descriptor)
    begin
      decode!(descriptor)
    rescue NativeException
      return nil
    end
  end

  def decode!(descriptor)
    descriptor = descriptor.path if descriptor.respond_to? :path
    descriptor = descriptor.to_s
    descriptor = case descriptor
    when URI.regexp(['http', 'https'])
      URL.new(descriptor)
    else
      Java::JavaIO::File.new(descriptor)
    end
    image = ImageIO.read(descriptor)
    bitmap = to_bitmap(image)
    @@decoder.decode(bitmap).to_s
  end

  private

  def to_bitmap(image)
    luminance = BufferedImageLuminanceSource.new(image)
    binarizer = GlobalHistogramBinarizer.new(luminance)
    BinaryBitmap.new(binarizer)
  end
end
