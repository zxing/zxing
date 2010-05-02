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
