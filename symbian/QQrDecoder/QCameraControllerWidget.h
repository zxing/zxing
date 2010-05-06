#ifndef QCAMERACONTROLLER_H
#define QCAMERACONTROLLER_H

#include <QWidget>
#include <FBS.H>  
#include <BITDEV.H>
#include <BITSTD.H>
#include <e32cmn.h>
#include <GDI.H>

#include <cameraengine.h>
#include <cameraengineobserver.h>

#include <QTimer>

class QCameraControllerWidget : public QWidget, public MCameraEngineObserver
{
    Q_OBJECT

public:
    QCameraControllerWidget(QWidget* parent);
    ~QCameraControllerWidget();

protected:
    void paintEvent(QPaintEvent* event);
    void resizeEvent(QResizeEvent* event);

private: // From MCameraEngineObserver
    void CreateBackBufferL();
    void ReleaseBackBuffer();

    void MceoCameraReady();
    void MceoFocusComplete();
    void MceoCapturedDataReady( TDesC8* aData );
    void MceoCapturedBitmapReady( CFbsBitmap* aBitmap );
    void MceoViewFinderFrameReady( CFbsBitmap& aFrame );
    void MceoHandleError( TCameraEngineError aErrorType, TInt aError );
    void MceoHandleOtherEvent( const TECAMEvent& /*aEvent*/ );
    void InitializeCamera();
    
////////////////////////
public slots:    
    void CaptureImage();
    
private slots:
    void sendBackbufferToDecode();
    
signals:
    void logMessage(QString str);
    void imageCaptured(QImage cImage);

private:
    // CameraWrapper class
    CCameraEngine*                      iCameraWrapper;

    CFbsBitmap*                         iBackBuffer;
    CFbsBitmapDevice*                   iBackBufferDevice;
    CFbsBitGc*                          iBackBufferContext;

    TSize                               iViewFinderSize;
    TSize                               iCaptureSize;
    
    //Timer
    QTimer* timer;
};

#endif //QCAMERACONTROLLER_H
