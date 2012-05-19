#include "qzxing.h"

#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/Binarizer.h>
#include <zxing/BinaryBitmap.h>
#include <zxing/MultiFormatReader.h>
#include <zxing/DecodeHints.h>
#include "CameraImageWrapper.h"
#include "imagehandler.h"

using namespace zxing;

QZXing::QZXing(QObject *parent) : QObject(parent)
{
    decoder = new MultiFormatReader();
    setDecoder(DecoderFormat_QR_CODE |
               DecoderFormat_DATA_MATRIX |
               DecoderFormat_UPC_E |
               DecoderFormat_UPC_A |
               DecoderFormat_EAN_8 |
               DecoderFormat_EAN_13 |
               DecoderFormat_CODE_128 |
               DecoderFormat_CODE_39 |
               DecoderFormat_ITF |
               DecoderFormat_Aztec);
    imageHandler = new ImageHandler();
}

void QZXing::setDecoder(DecoderFormatType hint)
{
    DecodeHints newHints;

    if(hint & DecoderFormat_QR_CODE)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_QR_CODE);

    if(hint & DecoderFormat_DATA_MATRIX)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_DATA_MATRIX);

    if(hint & DecoderFormat_UPC_E)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_UPC_E);

    if(hint & DecoderFormat_UPC_A)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_UPC_A);

    if(hint & DecoderFormat_EAN_8)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_EAN_8);

    if(hint & DecoderFormat_EAN_13)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_EAN_13);

    if(hint & DecoderFormat_CODE_128)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_CODE_128);

    if(hint & DecoderFormat_CODE_39)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_CODE_39);

    if(hint & DecoderFormat_ITF)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_ITF);

    if(hint & DecoderFormat_Aztec)
        newHints.addFormat((BarcodeFormat)BarcodeFormat_AZTEC);

    supportedFormats = newHints.getCurrentHint();
}

QString QZXing::decodeImage(QImage image)
{
    Ref<Result> res;
    emit decodingStarted();

    try{
        Ref<LuminanceSource> imageRef(new CameraImageWrapper(image));
        GlobalHistogramBinarizer* binz = new GlobalHistogramBinarizer(imageRef);

        Ref<Binarizer> bz (binz);
        BinaryBitmap* bb = new BinaryBitmap(bz);

        Ref<BinaryBitmap> ref(bb);

        res = ((MultiFormatReader*)decoder)->decode(ref, DecodeHints((int)supportedFormats));

        QString string = QString(res->getText()->getText().c_str());
        emit tagFound(string);
        emit decodingFinished(true);
        return string;
    }
    catch(zxing::Exception& e)
    {
       emit decodingFinished(false);
       return "";
    }
}

QString QZXing::decodeImageQML(QObject *item)
{
    return decodeSubImageQML(item);
}

QString QZXing::decodeSubImageQML(QObject* item,
                                  const double offsetX, const double offsetY,
                                  const double width, const double height)
{
    if(item  == NULL)
    {
        emit decodingFinished(false);
        return "";
    }

    QImage img = ((ImageHandler*)imageHandler)->extractQImage(item, offsetX, offsetY, width, height);

    return decodeImage(img);
}


