#ifndef QZXING_H
#define QZXING_H

#include "QZXing_global.h"
#include <QObject>
#include <QImage>

#if QT_VERSION >= 0x040700
#include <QtDeclarative>
#endif

/**
  * A class containing a very very small subset of the ZXing library.
  * Created for ease of use.
  *
  * Anyone interested in using more technical stuff
  * from the ZXing library is welcomed to add/edit on free will.
  *
  * Regarding DecoderFormat, by default all of those are enabled (except DataMatrix will is still not supported)
  */
class QZXINGSHARED_EXPORT QZXing : public QObject{
    Q_OBJECT
    Q_ENUMS(DecoderFormat)
public:
    enum DecoderFormat {
                    DecoderFormat_None = 0,
                    DecoderFormat_QR_CODE = 1,
                    DecoderFormat_DATA_MATRIX = 2,
                    DecoderFormat_UPC_E = 4,
                    DecoderFormat_UPC_A = 8,
                    DecoderFormat_EAN_8 = 16,
                    DecoderFormat_EAN_13 = 32,
                    DecoderFormat_CODE_128 = 64,
                    DecoderFormat_CODE_39 = 128,
                    DecoderFormat_ITF = 256,
                    DecoderFormat_Aztec = 512
            } ;
    typedef unsigned int DecoderFormatType;

public:
    QZXing(QObject *parent = NULL);

    /**
      * Set the enabled decoders.
      * As argument it is possible to pass conjuction of decoders by using logic OR.
      * e.x. setDecoder ( DecoderFormat_QR_CODE | DecoderFormat_EAN_13 | DecoderFormat_CODE_39 )
      */
    void setDecoder(DecoderFormatType hint);

#if QT_VERSION >= 0x040700
    static void registerQMLTypes()
    {
        qmlRegisterType<QZXing>("QZXing", 1, 2, "QZXing");
    }
#endif

public slots:
    /**
      * The decoding function. Will try to decode the given image based on the enabled decoders.
      *
      */
    QString decodeImage(QImage image);

    /**
      * The decoding function accessible from QML
      */
    QString decodeImageQML(QObject *item);

    /**
      * The decoding function accessible from QML. Able to set the decoding
      * of a portion of the image.
      */
    QString decodeSubImageQML(QObject* item,
                              const double offsetX = 0 , const double offsetY = 0,
                              const double width = 0, const double height = 0);

signals:
    void decodingStarted();
    void decodingFinished(bool succeeded);
    void tagFound(QString tag);

private:
    void* decoder;
    DecoderFormatType supportedFormats;
    QObject* imageHandler;
};

#endif // QZXING_H

