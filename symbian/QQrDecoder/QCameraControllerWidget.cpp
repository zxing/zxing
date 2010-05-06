#include "QCameraControllerWidget.h"
#include <QPainter>

QCameraControllerWidget::QCameraControllerWidget(QWidget* parent) : QWidget(parent),
iCameraWrapper(NULL), iBackBuffer(NULL), iBackBufferDevice(NULL), iBackBufferContext(NULL)
{
    timer = new QTimer(this);
    connect(timer, SIGNAL(timeout()), this, SLOT(sendBackbufferToDecode()));
    timer->start(500);
}

QCameraControllerWidget::~QCameraControllerWidget()
{
    if (iCameraWrapper)
    {
        iCameraWrapper->ReleaseAndPowerOff();
    }

    delete iCameraWrapper;

    if(timer)
    {
        delete timer;
        timer = NULL;
    }

    ReleaseBackBuffer();
}

void QCameraControllerWidget::CaptureImage()
{
    if (iCameraWrapper && iCameraWrapper->State() == CCameraEngine::EEngineViewFinding)
    {
        emit logMessage("Capturing picture");
        iCameraWrapper->StopViewFinder();
        TRAPD(err,iCameraWrapper->CaptureL());
        if (err)
        {
            emit logMessage("Camera capture error");                    
        }
    }
}

void QCameraControllerWidget::paintEvent(QPaintEvent* event)
{
    if(iBackBuffer)
    {
        QPainter paint(this);
        paint.drawPixmap(0,0,QPixmap::fromSymbianCFbsBitmap(iBackBuffer));
    }
}

void QCameraControllerWidget::resizeEvent(QResizeEvent* event)
{
    static int savedWidth = 0;
    static int savedHeight = 0;
    
    if(!savedWidth || !savedHeight)
    {
        InitializeCamera();
        savedWidth = geometry().width();
        savedHeight = geometry().height();
    }
}

void QCameraControllerWidget::InitializeCamera()
{
    // Create camera wrapper class here because
    // whole camera wrapper and all handles have to reset
    // while orientatio of the application changes.
    if (iCameraWrapper)
    {
        // Power off camera if it is on
        iCameraWrapper->StopViewFinder();
        iCameraWrapper->ReleaseAndPowerOff();
        delete iCameraWrapper; iCameraWrapper = NULL;
    }
    TInt camErr(KErrNotSupported);
    if(CCameraEngine::CamerasAvailable() > 0)
    {
        TRAP(camErr, iCameraWrapper = CCameraEngine::NewL(0,0,this));
    }

    // iViewFinderSize is picture size for viewfinder.
    // iCaptureSize is picture size for capturing picture.
    // We want fill whole screen
    if (geometry().width() > geometry().height())
    {
        iViewFinderSize = TSize(geometry().width(),geometry().width());
        iCaptureSize = TSize(geometry().width(),geometry().width()); // Captured picture size
    }
    else
    {
        iViewFinderSize = TSize(geometry().height(), geometry().height());
        iCaptureSize = TSize(geometry().height(),geometry().height()); // Captured picture size
    }

    // Create back buffer where recieved camera pictures are copied
    ReleaseBackBuffer();
    CreateBackBufferL();

    // Power on camera, start viewfinder when MceoCameraReady() received
    if(camErr == KErrNone)
    {
        iCameraWrapper->ReserveAndPowerOn();    
        emit logMessage("Camera power on");
    }
    else
    {
        emit logMessage("no camera found");
    }
}

void QCameraControllerWidget::CreateBackBufferL()
{
    // create back buffer bitmap
    iBackBuffer = q_check_ptr(new CFbsBitmap);

    try{
        TSize size;
        size.iHeight = this->geometry().height();
        size.iWidth = this->geometry().width();
        QT_TRAP_THROWING( iBackBuffer->Create(size,EColor64K));
    }
    catch(std::exception& e)
    {

    }

    // create back buffer graphics context
    iBackBufferDevice = CFbsBitmapDevice::NewL(iBackBuffer);
    User::LeaveIfError(iBackBufferDevice->CreateContext(iBackBufferContext));
    iBackBufferContext->SetPenStyle(CGraphicsContext::ESolidPen);

    iBackBufferContext->SetBrushColor(KRgbBlack);
    iBackBufferContext->Clear();
}

void QCameraControllerWidget::ReleaseBackBuffer()
{
    if (iBackBufferContext)
    {
        delete iBackBufferContext;
        iBackBufferContext = NULL;
    }
    if (iBackBufferDevice)
    {
        delete iBackBufferDevice;
        iBackBufferDevice = NULL;
    }
    if (iBackBuffer)
    {
        delete iBackBuffer;
        iBackBuffer = NULL;
    }
}

void QCameraControllerWidget::MceoCameraReady()
{
    if (iCameraWrapper->State() == CCameraEngine::EEngineIdle)
    {
        // Prepare camera
        TSize imageSize;
        imageSize.iHeight = 480;
        imageSize.iWidth = 640;

        CCamera::TFormat format = CCamera::EFormatFbsBitmapColor64K;

        TRAPD(err,iCameraWrapper->PrepareL(imageSize, format));
        if (err)
        {
            emit logMessage("Camera prepare error");
            return;
        }

        // Start viewfinder. Viewfinder pictures starts coming into MceoViewFinderFrameReady();

        TSize finderSize;
        finderSize.iHeight = this->geometry().height();
        finderSize.iWidth = this->geometry().width();
        TRAPD(err2,iCameraWrapper->StartViewFinderL(finderSize));
        if (err2)
        {
            emit logMessage("Camera start viewfinder error");
            return;
        }

        emit logMessage("Camera viewfinder started");
    }
}

void QCameraControllerWidget::MceoFocusComplete()
{
    // CameraEngine state is EEngineIdle
    emit logMessage("Focused");

    // Capture picture after it has focused
    iCameraWrapper->StopViewFinder();
    TRAPD(err,iCameraWrapper->CaptureL());
    if (err)
    {
        emit logMessage("Camera capture error");
    }
}

void QCameraControllerWidget::MceoCapturedDataReady( TDesC8* aData )
{

}

void QCameraControllerWidget::MceoCapturedBitmapReady( CFbsBitmap* aBitmap )
{
    if (iBackBufferContext)
    {
        emit logMessage("Succesfull capture");

        QPixmap pix(QPixmap::fromSymbianCFbsBitmap(aBitmap));
        emit imageCaptured(pix.toImage());

        TSize finderSize;
        finderSize.iHeight = this->geometry().height();
        finderSize.iWidth = this->geometry().width();
        TRAPD(err2,iCameraWrapper->StartViewFinderL(finderSize));
        if (err2)
        {
            emit logMessage("Camera start viewfinder error");
            return;
        }
    }
    if (iCameraWrapper)
        iCameraWrapper->ReleaseImageBuffer();
}

void QCameraControllerWidget::MceoViewFinderFrameReady( CFbsBitmap& aFrame )
{
    if (iBackBufferContext)
    {
        TSize bmpSizeInPixels = aFrame.SizeInPixels();
        TInt xDelta = 0;
        TInt yDelta = 0;
        TPoint pos( xDelta, yDelta );

        // Copy received viewfinder picture to back buffer
        iBackBufferContext->BitBlt( pos, &aFrame, TRect( TPoint( 0, 0 ), bmpSizeInPixels ));

        // Update backbuffer into screen 
        update();
    }
    if (iCameraWrapper)
        iCameraWrapper->ReleaseViewFinderBuffer();
}

void QCameraControllerWidget::MceoHandleError( TCameraEngineError aErrorType, TInt aError )
{
    // NOTE: CameraEngine state seems to go into CCameraEngine::EEngineIdle state

    if (aErrorType == EErrReserve)
    {
        return; //-18 comes on application startup, but everything works ok
    }

    switch (aErrorType)
    {
    case EErrReserve:
    {
        emit logMessage("Camera reserved error");
        break;
    }
    case EErrPowerOn:
    {
        emit logMessage("Camera power on error");
        break;
    }
    case EErrViewFinderReady:
    {
        emit logMessage("Camera viewfinder error");
        break;
    }
    case EErrImageReady:
    {
        emit logMessage("Camera image ready error");
        break;
    }
    case EErrAutoFocusInit:
    case EErrAutoFocusMode:
    case EErrAutoFocusArea:
    case EErrAutoFocusRange:
    case EErrAutoFocusType:
    case EErrOptimisedFocusComplete:
    {
        //emit logMessage("Try focusing again");
        break;
    }
    default:
    {
        emit logMessage("Unknown error");
        break;
    }
    };

    // Try handle error
    //CancelCapturedPicture(EFalse);
    //    iAppUi->UseOptionsExitCbaL();
}

void QCameraControllerWidget::MceoHandleOtherEvent( const TECAMEvent& /*aEvent*/ )
{
}

//Timer slot
void QCameraControllerWidget::sendBackbufferToDecode()
{
    if(!iBackBuffer)
        return;

    QPixmap pix(QPixmap::fromSymbianCFbsBitmap(iBackBuffer));
    emit imageCaptured(pix.toImage());

    if(timer)
        timer->start(500);
}

