/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include <avkon.hrh>
#include <eikon.hrh>
#include <aknmessagequerydialog.h>
#include <aknnotewrappers.h>
#include <eikenv.h>

#include <ZXingBarcodeReader_0xEF24C10A.rsg>
#include "ZXingBarcodeReader.hrh"
#include "ZXingBarcodeReader.pan"
#include "ZXingBarcodeReaderApplication.h"
#include "ZXingBarcodeReaderAppUi.h"
#include "ZXingBarcodeReaderAppView.h"


// ============================ MEMBER FUNCTIONS ===============================

void CZXingBarcodeReaderAppUi::ConstructL ()
    {
    // Initialise app UI with standard value.
    BaseConstructL (CAknAppUi::EAknEnableSkin );

    // Start receiving camera shutter key events
    #ifdef ENABLE_CAMERA_SHUTTER
    CaptureCameraShutter(ETrue);
    #endif
    
    // Make this class observe changes in foreground events
    iEikonEnv->AddForegroundObserverL(*this);
    
    // Create view
    iAppView = CZXingBarcodeReaderAppView::NewL (ClientRect () );
    }

CZXingBarcodeReaderAppUi::CZXingBarcodeReaderAppUi ()
    {
    }

CZXingBarcodeReaderAppUi::~CZXingBarcodeReaderAppUi ()
    {
    delete iAppView;
    
    #ifdef ENABLE_CAMERA_SHUTTER
    CaptureCameraShutter(EFalse);
    #endif

    iShutterKeyHandles.Close();
    }

#ifdef ENABLE_CAMERA_SHUTTER

void CZXingBarcodeReaderAppUi::CaptureCameraShutter(TBool aEnable)
    {
    // Try to capture events from the camera shutter key(s)
    // http://wiki.forum.nokia.com/index.php/KIS000563_-_Camera_shutter_key_(EKeyCamera_events)_cannot_be_used_in_3rd_party_applications
    if (aEnable && !iCameraKeyCaptured)
        {
        iCameraKeyCaptured = ETrue;
        // Enable capturing
        RProcess proc;
        iShutterKeyHandles.Reset();
        if(proc.HasCapability(ECapabilitySwEvent))
            {
            for(TInt i=0; KCameraShutterKeyEventCodes[i] != 0; i++)
                {
                TInt32 handle = iEikonEnv->RootWin().CaptureKey( KCameraShutterKeyEventCodes[i], 0, 0 );
                if(handle >= 0)
                    {
                    iShutterKeyHandles.Append(handle);
                    }
                }
            }
        }
    else if(!aEnable && iCameraKeyCaptured)
        {
        iCameraKeyCaptured = EFalse;
        // Disable capturing
        // Release the captured camera shutter key(s)
        for(TInt i=0; i < iShutterKeyHandles.Count(); i++)
            {
            iEikonEnv->RootWin().CancelCaptureKey( iShutterKeyHandles[i] );
            }
        }
    }
#endif


void CZXingBarcodeReaderAppUi::HandleGainingForeground()
    {
    // Application gets focused so reserve the camera
    // http://wiki.forum.nokia.com/index.php/CS000821_-_Handling_Camera_resource
    if ( iAppView && 
         iAppView->CameraEngine() &&
         iAppView->CameraEngine()->State() != CCameraEngine::EEngineNotReady )
        {
        iAppView->CameraEngine()->ReserveAndPowerOn();
        
#ifdef ENABLE_CAMERA_SHUTTER
        CaptureCameraShutter(ETrue);
#endif
        }
    }

void CZXingBarcodeReaderAppUi::HandleLosingForeground()
    {
    // Application loses focus so release the camera
    // http://wiki.forum.nokia.com/index.php/CS000821_-_Handling_Camera_resource
    if ( iAppView && 
         iAppView->CameraEngine() &&
         iAppView->CameraEngine()->State() != CCameraEngine::EEngineNotReady )
        {
        iAppView->CameraEngine()->ReleaseAndPowerOff();
        
#ifdef ENABLE_CAMERA_SHUTTER
        CaptureCameraShutter(EFalse);
#endif
        }
    }

void CZXingBarcodeReaderAppUi::UseOptionsExitCbaL()
    {
    CEikButtonGroupContainer* cba = Cba();
    if (cba)
        {
        cba->SetCommandSetL(R_AVKON_SOFTKEYS_OPTIONS_EXIT);
        cba->DrawNow();
        }
    }

void CZXingBarcodeReaderAppUi::UseOptionsBackCbaL()
    {
    CEikButtonGroupContainer* cba = Cba();
    if (cba)
        {
        cba->SetCommandSetL(R_AVKON_SOFTKEYS_OPTIONS_BACK);
        cba->DrawNow();
        }
    }

TBool CZXingBarcodeReaderAppUi::IsBackCBA()
    {
    CEikButtonGroupContainer* cba = Cba();
    // NOTE: There should be EAknSoftkeyBack in the application because
    // we use R_AVKON_SOFTKEYS_SELECT_BACK, but it seems that there is EAknSoftkeyCancel
    CCoeControl* back = cba->ControlOrNull(EAknSoftkeyBack);
    CCoeControl* cancel = cba->ControlOrNull(EAknSoftkeyCancel);
    if (back || cancel)
        return ETrue;
    else
        return EFalse;
    }

TKeyResponse CZXingBarcodeReaderAppUi::HandleKeyEventL(
    const TKeyEvent& aKeyEvent,TEventCode aType)
    {
    // Capture picture with selection key
    switch ( aKeyEvent.iCode )
        {
        case EKeyOK:
        case EStdKeyDevice3:
        case EKeyUpArrow:
        case EKeyDownArrow:  
            {
            // Capture picture
            return iAppView->OfferKeyEventL(aKeyEvent,aType);
            }
        default:
            {
            break;
            }
        };

    // Camera shutter events handling
    #ifdef ENABLE_CAMERA_SHUTTER
    // Camera shutter autofocus
    switch ( aKeyEvent.iScanCode )
        {
        case KStdKeyCameraFocus:
        case KStdKeyCameraFocus2:
            {
            // Camera shutter autofocus
            return iAppView->OfferKeyEventL(aKeyEvent,aType);
            }
        default:
            {
            break;
            }
        };
    // Camera shutter key
    for(TInt i=0; KCameraShutterKeyEventCodes[i] != 0; i++)
          {
          if( KCameraShutterKeyEventCodes[i] == aKeyEvent.iCode )
            {
            // Capture image
            iAppView->Capture();
            return EKeyWasConsumed;
            }
          }
    #endif

    return EKeyWasNotConsumed;
    }

void CZXingBarcodeReaderAppUi::HandleCommandL (TInt aCommand )
    {
    switch (aCommand )
        {
        case EEikCmdExit:
        case EAknSoftkeyExit:
            {
            Exit();
            break;
            }
        case EAknSoftkeyBack:
            {
            iAppView->CancelCapturedPicture();
            UseOptionsExitCbaL();
            break;
            }
        case EAbout:
            {
            CAknMessageQueryDialog* dlg = new (ELeave) CAknMessageQueryDialog ();
            dlg->PrepareLC (R_ABOUT_QUERY_DIALOG );
            HBufC* title = iEikonEnv->AllocReadResourceLC (R_ABOUT_DIALOG_TITLE );
            dlg->QueryHeading ()->SetTextL (*title );
            CleanupStack::PopAndDestroy (); //title
            HBufC* msg = iEikonEnv->AllocReadResourceLC (R_ABOUT_DIALOG_TEXT );
            dlg->SetMessageTextL (*msg );
            CleanupStack::PopAndDestroy (); //msg
            dlg->RunLD();
            break;
            }
        default:
            {
            break;
            }
        };
    }

void CZXingBarcodeReaderAppUi::HandleResourceChangeL(TInt aType)
    {
    CAknAppUi::HandleResourceChangeL( aType );
       
    if ( aType==KEikDynamicLayoutVariantSwitch )
        {
        if (iAppView)
            {
            TRect rect;
            AknLayoutUtils::LayoutMetricsRect(AknLayoutUtils::EMainPane,rect);
            iAppView->SetRect(rect);
            }
        }   
    
    }

// End of File
