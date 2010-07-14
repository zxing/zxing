#ifndef CAMERAIMAGE_H
#define CAMERAIMAGE_H

#include <zxing/LuminanceSource.h>
#include <string>
#include <FBS.H>  

using namespace zxing;
using namespace std;

class CameraImage : public LuminanceSource
{
public:
    CameraImage();
    CameraImage(CameraImage& otherInstance);
    ~CameraImage();
    
    int getWidth() const;
    int getHeight() const;
    
    unsigned char getPixel(int x, int y) const;
    
    void setImage(CFbsBitmap* newImage);
    
    CFbsBitmap* getImage();
  
private:
    CFbsBitmap* image;
};

#endif //CAMERAIMAGE_H
