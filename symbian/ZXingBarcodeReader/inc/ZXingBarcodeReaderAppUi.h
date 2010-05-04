/*
 * Copyright (c) 2009 Nokia Corporation.
 */

#ifndef __CAMERAWRAPPEREXAMPLEAPPUI_h__
#define __CAMERAWRAPPEREXAMPLEAPPUI_h__

// INCLUDES
#include <aknappui.h>

//#define EKeyZoomIn      EKeyApplicationC
//#define EKeyZoomOut     EKeyApplicationD
//#define EKeyVolumeUp    EKeyIncVolume
//#define EKeyVolumeDown  EKeyDecVolume


#ifdef ENABLE_CAMERA_SHUTTER
// Focus key events (shutter key pressed half-way down)
const TInt KStdKeyCameraFocus   = 0xe2;
const TInt KStdKeyCameraFocus2  = 0xeb;     // S60 3.2 and onwards

// All known event codes used for the camera shutter key on S60 3.x devices
const TUint KKeyCameraShutter1   = 0xf883;
const TUint KKeyCameraShutter2   = 0xf849;   // S60 3.2
const TUint KKeyCameraNseries1   = 0xf881;   // S60 3.2 Nseries
const TUint KKeyCameraNseries2   = 0xf88c;

const TUint KCameraShutterKeyEventCodes[6] = {
    EKeyCamera, // general camera key
    KKeyCameraShutter1,
    KKeyCameraShutter2,
    KKeyCameraNseries1,
    KKeyCameraNseries2,
    0 };
#endif


// FORWARD DECLARATIONS
class CZXingBarcodeReaderAppView;

// CLASS DECLARATION
class CZXingBarcodeReaderAppUi : public CAknAppUi,
                                   public MCoeForegroundObserver
    {
    public:
        // Constructors and destructor
        void ConstructL ();
        CZXingBarcodeReaderAppUi ();
        virtual ~CZXingBarcodeReaderAppUi ();
        
    private:
        TKeyResponse HandleKeyEventL(const TKeyEvent& aKeyEvent,TEventCode aType);
        void HandleCommandL (TInt aCommand );
        void HandleResourceChangeL(TInt aType);
        
        #ifdef ENABLE_CAMERA_SHUTTER
        void CaptureCameraShutter(TBool aEnable);
        #endif
        
    public:
        void UseOptionsExitCbaL();
        void UseOptionsBackCbaL();
        TBool IsBackCBA();        
        
    private: // From MCoeForegroundObserver
        void HandleGainingForeground();
        void HandleLosingForeground();
        
    private:
        // Data
        CZXingBarcodeReaderAppView*   iAppView;
        RArray<TInt32>                  iShutterKeyHandles;
        TBool                           iCameraKeyCaptured;

    };

#endif // __CAMERAWRAPPEREXAMPLEAPPUI_h__

// End of File
