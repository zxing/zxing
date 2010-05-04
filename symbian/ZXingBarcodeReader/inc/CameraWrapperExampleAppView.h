/*
 * Copyright (c) 2009 Nokia Corporation.
 */

#ifndef __CAMERAWRAPPEREXAMPLEAPPVIEW_h__
#define __CAMERAWRAPPEREXAMPLEAPPVIEW_h__

// INCLUDES
#include <coecntrl.h>
#include <fbs.h>

#include <cameraengine.h>
#include <cameraengineobserver.h>
#include <akninfopopupnotecontroller.h>
#include <e32base.h>  
#include <string>

class CCameraWrapperExampleAppUi;

// CLASS DECLARATION
class CCameraWrapperExampleAppView : 
public CCoeControl, public MCameraEngineObserver
    {
    public: 
        // Constructors
        static CCameraWrapperExampleAppView* NewL (const TRect& aRect );
        static CCameraWrapperExampleAppView* NewLC (const TRect& aRect );
        virtual ~CCameraWrapperExampleAppView ();
    
    private: 
        // Functions from base classes
        void Draw (const TRect& aRect ) const;
        void DrawTexts(CWindowGc& gc) const;
        void SizeChanged ();
        void HandlePointerEventL (const TPointerEvent& aPointerEvent );
        void SetTitle(const TDesC& aTitle);
        void SetError( const TDesC& aMsg, TInt aVal );
        void SetError( const TDesC& aMsg, TInt aVal1, TInt aVal2 );
        void StartFocusing();
        void StorePicture( TDesC8* aData );

    public:
        TKeyResponse OfferKeyEventL(const TKeyEvent& aKeyEvent,TEventCode aType);
        CCameraEngine* CameraEngine(){return iCameraWrapper;};
        void CancelCapturedPicture(TBool aCleanTexts=ETrue);
        void Capture();        
        
    private: // From MCameraEngineObserver
        void MceoCameraReady();
        void MceoFocusComplete();
        void MceoCapturedDataReady( TDesC8* aData );
        void MceoCapturedBitmapReady( CFbsBitmap* aBitmap );
        void MceoViewFinderFrameReady( CFbsBitmap& aFrame );
        void MceoHandleError( TCameraEngineError aErrorType, TInt aError );
        void MceoHandleOtherEvent( const TECAMEvent& /*aEvent*/ );
    
    private: 
        // Constructors
        void ConstructL (const TRect& aRect );
        CCameraWrapperExampleAppView ();

    public:
        void decodeBackbufferImage();
    
    private:
        void CreateBackBufferL();
        void ReleaseBackBuffer();
        void ShowResultL(TDesC16& message);
        
        //timer
        void StartTimer();
        static TInt Tick(TAny* aObject);
    
    private: 
        // Data
        
        CCameraWrapperExampleAppUi*         iAppUi;

        // CameraWrapper class
        CCameraEngine*                      iCameraWrapper;

        TSize                               iViewFinderSize;
        TSize                               iCaptureSize;
    
        CFbsBitmap*                         iBackBuffer;
        CFbsBitmapDevice*                   iBackBufferDevice;
        CFbsBitGc*                          iBackBufferContext;
        
        const CFont*                        iTitleFont;
        TBuf<50>                            iTitle;
        TRect                               iFocusRect;
        
        // Is new picture focused whit camera shutter key
        TBool                               iCameraShutterFocusing;
        
        HBufC8*                             iData;
       
 
// Set the note as the member variable of your application view (for example, CAknView)
private:
    CAknInfoPopupNoteController*     	iNote;
    CPeriodic* 							iPeriodic;
    
    };

#endif // __CAMERAWRAPPEREXAMPLEAPPVIEW_h__

// End of File
