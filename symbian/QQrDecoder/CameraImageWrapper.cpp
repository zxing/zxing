#include "CameraImageWrapper.h"
#include <QColor>
#include <QApplication>
#include <QDesktopWidget>

CameraImageWrapper::CameraImageWrapper() : LuminanceSource()
{
}

CameraImageWrapper::CameraImageWrapper(CameraImageWrapper& otherInstance) : LuminanceSource()
                    {
    image = otherInstance.getOriginalImage().copy();
                    }

CameraImageWrapper::~CameraImageWrapper()
{
}

int CameraImageWrapper::getWidth() const
    {
    return image.width();
    }

int CameraImageWrapper::getHeight() const
    {
    return image.height();
    }

unsigned char CameraImageWrapper::getPixel(int x, int y) const
    {
    QRgb pixel = image.pixel(x,y);

    return qGray(pixel);//((qRed(pixel) + qGreen(pixel) + qBlue(pixel)) / 3);
    }

unsigned char* CameraImageWrapper::copyMatrix() const
    {
        unsigned char* newMatrix = (unsigned char*)malloc(image.width()*image.height()*sizeof(unsigned char));
            
        int cnt = 0;
        for(int i=0; i<image.width(); i++)
        {
            for(int j=0; j<image.height(); j++)
            {
                newMatrix[cnt++] = getPixel(i,j);
            }
        }
        
        return newMatrix;
    }

void CameraImageWrapper::setImage(QString fileName)
{
    image.load(fileName);

    if(image.width() > QApplication::desktop()->width())
        image = image.scaled(QApplication::desktop()->width(), image.height(), Qt::IgnoreAspectRatio);

    if(image.height() > QApplication::desktop()->height())
        image = image.scaled(image.width(), QApplication::desktop()->height(), Qt::IgnoreAspectRatio);
}

void CameraImageWrapper::setImage(QImage newImage)
{
    image = newImage.copy();

    if(image.width() > 640)
        image = image.scaled(640, image.height(), Qt::KeepAspectRatio);
}

QImage CameraImageWrapper::grayScaleImage(QImage::Format f)
{
    QImage tmp(image.width(), image.height(), f);
    for(int i=0; i<image.width(); i++)
    {
        for(int j=0; j<image.height(); j++)
        {
            int pix = (int)getPixel(i,j);
            tmp.setPixel(i,j, qRgb(pix ,pix,pix));
        }   
    }

    return tmp;

    //return image.convertToFormat(f);
}

QImage CameraImageWrapper::getOriginalImage()
{
    return image;
}


