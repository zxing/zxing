TEMPLATE = app
TARGET = QQrDecoder
QT += core \
    gui

VERSION = 1.1.0

CONFIG += mobility
MOBILITY = multimedia #\
    #systeminfo

RESOURCES +=  resources.qrc

HEADERS += CameraImageWrapper.h \
    zxing/BarcodeFormat.h \
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
    QQrDecoder.h \
    QCameraControllerWidget.h \
    button.h \
    myvideosurface.h
SOURCES += CameraImageWrapper.cpp \
    zxing/BarcodeFormat.cpp \
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
    main.cpp \
    QQrDecoder.cpp \
    QCameraControllerWidget.cpp \
    button.cpp \
    myvideosurface.cpp
FORMS += QQrDecoder.ui

symbian{
    TARGET.UID3 = 0xEF2CE79D
    TARGET.EPOCSTACKSIZE = 0x14000
    TARGET.EPOCHEAPSIZE = 0x20000 0x8000000

    # Because landscape orientation lock
    LIBS += -lcone -leikcore -lavkon

    # Self-signing capabilities
    TARGET.CAPABILITY += NetworkServices \
        ReadUserData \
        WriteUserData \
        LocalServices \
        UserEnvironment
}
else{
    DEFINES += NO_ICONV
}


DEFINES += ZXING_ICONV_CONST

ICON = QQrDecoder.svg





