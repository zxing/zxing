#-------------------------------------------------
#
# Project created by QtCreator 2011-11-13T18:12:28
#
#-------------------------------------------------

QT       += core gui

VERSION = 1.1

greaterThan(QT_VERSION, 4.7): QT += declarative

TARGET = QZXing
TEMPLATE = lib

DEFINES += QZXING_LIBRARY \
         ZXING_ICONV_CONST

INCLUDEPATH  += ./


HEADERS += zxing/BarcodeFormat.h \
    zxing/Binarizer.h \
    zxing/BinaryBitmap.h \
    zxing/DecodeHints.h \
    zxing/Exception.h \
    zxing/LuminanceSource.h \
    zxing/MultiFormatReader.h \
    zxing/Reader.h \
    zxing/ReaderException.h \
    zxing/Result.h \
    zxing/ResultPoint.h \
    zxing/ResultPointCallback.h \
    zxing/FormatException.h \
    zxing/NotFoundException.h \
    zxing/common/StringUtils.h \
    zxing/common/CharacterSetECI.h \
    zxing/common/ECI.h \
    zxing/common/Array.h \
    zxing/common/BitArray.h \
    zxing/common/BitMatrix.h \
    zxing/common/BitSource.h \
    zxing/common/Counted.h \
    zxing/common/DecoderResult.h \
    zxing/common/DetectorResult.h \
    zxing/common/EdgeDetector.h \
    zxing/common/GlobalHistogramBinarizer.h \
    zxing/common/GreyscaleLuminanceSource.h \
    zxing/common/GreyscaleRotatedLuminanceSource.h \
    zxing/common/GridSampler.h \
    zxing/common/HybridBinarizer.h \
    zxing/common/IllegalArgumentException.h \
    zxing/common/PerspectiveTransform.h \
    zxing/common/Point.h \
    zxing/common/Str.h \
    zxing/common/reedsolomon/GF256.h \
    zxing/common/reedsolomon/GF256Poly.h \
    zxing/common/reedsolomon/ReedSolomonDecoder.h \
    zxing/common/reedsolomon/ReedSolomonException.h \
    zxing/common/detector/MonochromeRectangleDetector.h \
    zxing/common/detector/WhiteRectangleDetector.h \
    zxing/oned/Code128Reader.h \
    zxing/oned/Code39Reader.h \
    zxing/oned/EAN13Reader.h \
    zxing/oned/EAN8Reader.h \
    zxing/oned/ITFReader.h \
    zxing/oned/MultiFormatOneDReader.h \
    zxing/oned/MultiFormatUPCEANReader.h \
    zxing/oned/OneDReader.h \
    zxing/oned/OneDResultPoint.h \
    zxing/oned/UPCAReader.h \
    zxing/oned/UPCEANReader.h \
    zxing/oned/UPCEReader.h \
    zxing/qrcode/ErrorCorrectionLevel.h \
    zxing/qrcode/FormatInformation.h \
    zxing/qrcode/QRCodeReader.h \
    zxing/qrcode/Version.h \
    zxing/qrcode/decoder/BitMatrixParser.h \
    zxing/qrcode/decoder/DataBlock.h \
    zxing/qrcode/decoder/DataMask.h \
    zxing/qrcode/decoder/DecodedBitStreamParser.h \
    zxing/qrcode/decoder/Decoder.h \
    zxing/qrcode/decoder/Mode.h \
    zxing/qrcode/detector/AlignmentPattern.h \
    zxing/qrcode/detector/AlignmentPatternFinder.h \
    zxing/qrcode/detector/Detector.h \
    zxing/qrcode/detector/FinderPattern.h \
    zxing/qrcode/detector/FinderPatternFinder.h \
    zxing/qrcode/detector/FinderPatternInfo.h \
    zxing/qrcode/detector/QREdgeDetector.h \
    zxing/datamatrix/VersionDM.h \
    zxing/datamatrix/DataMatrixReader.h \
    zxing/datamatrix/decoder/BitMatrixParserDM.h \
    zxing/datamatrix/decoder/DataBlockDM.h \
    zxing/datamatrix/decoder/DecodedBitStreamParserDM.h \
    zxing/datamatrix/decoder/DecoderDM.h \
    zxing/datamatrix/detector/CornerPoint.h \
    zxing/datamatrix/detector/DetectorDM.h \
    zxing/datamatrix/detector/DetectorException.h \
    zxing/datamatrix/detector/MonochromeRectangleDetectorDM.h \
    QZXing_global.h \
    CameraImageWrapper.h \
    qzxing.h

SOURCES += zxing/BarcodeFormat.cpp \
    zxing/Binarizer.cpp \
    zxing/BinaryBitmap.cpp \
    zxing/DecodeHints.cpp \
    zxing/Exception.cpp \
    zxing/LuminanceSource.cpp \
    zxing/MultiFormatReader.cpp \
    zxing/Reader.cpp \
    zxing/ReaderException.cpp \
    zxing/Result.cpp \
    zxing/ResultPoint.cpp \
    zxing/ResultPointCallback.cpp \
    zxing/FormatException.cpp \
    zxing/NotFoundException.cpp \
    zxing/common/StringUtils.cpp \
    zxing/common/CharacterSetECI.cpp \
    zxing/common/ECI.cpp \
    zxing/common/Array.cpp \
    zxing/common/BitArray.cpp \
    zxing/common/BitMatrix.cpp \
    zxing/common/BitSource.cpp \
    zxing/common/Counted.cpp \
    zxing/common/DecoderResult.cpp \
    zxing/common/DetectorResult.cpp \
    zxing/common/EdgeDetector.cpp \
    zxing/common/GlobalHistogramBinarizer.cpp \
    zxing/common/GreyscaleLuminanceSource.cpp \
    zxing/common/GreyscaleRotatedLuminanceSource.cpp \
    zxing/common/GridSampler.cpp \
    zxing/common/HybridBinarizer.cpp \
    zxing/common/IllegalArgumentException.cpp \
    zxing/common/PerspectiveTransform.cpp \
    zxing/common/Str.cpp \
    zxing/common/reedsolomon/GF256.cpp \
    zxing/common/reedsolomon/GF256Poly.cpp \
    zxing/common/reedsolomon/ReedSolomonDecoder.cpp \
    zxing/common/reedsolomon/ReedSolomonException.cpp \
    zxing/common/detector/MonochromeRectangleDetector.cpp \
    zxing/common/detector/WhiteRectangleDetector.cpp \
    zxing/oned/Code128Reader.cpp \
    zxing/oned/Code39Reader.cpp \
    zxing/oned/EAN13Reader.cpp \
    zxing/oned/EAN8Reader.cpp \
    zxing/oned/ITFReader.cpp \
    zxing/oned/MultiFormatOneDReader.cpp \
    zxing/oned/MultiFormatUPCEANReader.cpp \
    zxing/oned/OneDReader.cpp \
    zxing/oned/OneDResultPoint.cpp \
    zxing/oned/UPCAReader.cpp \
    zxing/oned/UPCEANReader.cpp \
    zxing/oned/UPCEReader.cpp \
    zxing/qrcode/ErrorCorrectionLevel.cpp \
    zxing/qrcode/FormatInformation.cpp \
    zxing/qrcode/QRCodeReader.cpp \
    zxing/qrcode/Version.cpp \
    zxing/qrcode/decoder/BitMatrixParser.cpp \
    zxing/qrcode/decoder/DataBlock.cpp \
    zxing/qrcode/decoder/DataMask.cpp \
    zxing/qrcode/decoder/DecodedBitStreamParser.cpp \
    zxing/qrcode/decoder/Decoder.cpp \
    zxing/qrcode/decoder/Mode.cpp \
    zxing/qrcode/detector/AlignmentPattern.cpp \
    zxing/qrcode/detector/AlignmentPatternFinder.cpp \
    zxing/qrcode/detector/Detector.cpp \
    zxing/qrcode/detector/FinderPattern.cpp \
    zxing/qrcode/detector/FinderPatternFinder.cpp \
    zxing/qrcode/detector/FinderPatternInfo.cpp \
    zxing/qrcode/detector/QREdgeDetector.cpp \
    zxing/datamatrix/VersionDM.cpp \
    zxing/datamatrix/DataMatrixReader.cpp \
    zxing/datamatrix/decoder/BitMatrixParserDM.cpp \
    zxing/datamatrix/decoder/DataBlockDM.cpp\
    zxing/datamatrix/decoder/DecodedBitStreamParserDM.cpp \
    zxing/datamatrix/decoder/DecoderDM.cpp \
    zxing/datamatrix/detector/CornerPoint.cpp \
    zxing/datamatrix/detector/DetectorDM.cpp \
    zxing/datamatrix/detector/DetectorException.cpp \
    zxing/datamatrix/detector/MonochromeRectangleDetectorDM.cpp \
    CameraImageWrapper.cpp \
    qzxing.cpp

symbian {
    TARGET.UID3 = 0xE618743C
    TARGET.EPOCALLOWDLLDATA = 1
    addFiles.sources = QZXing.dll
    addFiles.path = !:/sys/bin
    DEPLOYMENT += addFiles
    #TARGET.CAPABILITY = All -TCB -AllFiles -DRM
    TARGET.CAPABILITY += NetworkServices \
        ReadUserData \
        WriteUserData \
        LocalServices \
        UserEnvironment \
        Location
}

unix:!symbian {
    maemo5 {
        target.path = /opt/usr/lib
    } else {
        target.path = /usr/lib
    }

    DEFINES += NOFMAXL
    INSTALLS += target
}

win32{
    DEFINES += NO_ICONV
}

OTHER_FILES += \
    qtc_packaging/debian_harmattan/rules \
    qtc_packaging/debian_harmattan/README \
    qtc_packaging/debian_harmattan/manifest.aegis \
    qtc_packaging/debian_harmattan/copyright \
    qtc_packaging/debian_harmattan/control \
    qtc_packaging/debian_harmattan/compat \
    qtc_packaging/debian_harmattan/changelog \
    qtc_packaging/debian_fremantle/rules \
    qtc_packaging/debian_fremantle/README \
    qtc_packaging/debian_fremantle/copyright \
    qtc_packaging/debian_fremantle/control \
    qtc_packaging/debian_fremantle/compat \
    qtc_packaging/debian_fremantle/changelog


