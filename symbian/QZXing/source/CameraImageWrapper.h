#ifndef CAMERAIMAGE_H
#define CAMERAIMAGE_H

#include <QImage>
#include <QString>
#include <zxing/LuminanceSource.h>

using namespace zxing;

class CameraImageWrapper : public LuminanceSource
{
public:
    CameraImageWrapper();
    CameraImageWrapper(QImage& image);
    CameraImageWrapper(CameraImageWrapper& otherInstance);
    ~CameraImageWrapper();
    
    int getWidth() const;
    int getHeight() const;
    
    unsigned char getPixel(int x, int y) const;
    unsigned char* copyMatrix() const;
    
    /**
      * Set the source of the image. If it fails,  returns false.
      */
    bool setImage(QString fileName);
    bool setImage(QImage newImage);

    QImage grayScaleImage(QImage::Format f);
    QImage getOriginalImage();


    // Callers take ownership of the returned memory and must call delete [] on it themselves.
    unsigned char* getRow(int y, unsigned char* row);
    unsigned char* getMatrix();

    void setSmoothTransformation(bool enable);
  
private:
    QImage image;
    unsigned char* pRow;
    unsigned char* pMatrix;
    bool isSmoothTransformationEnabled;
};

#endif //CAMERAIMAGE_H
