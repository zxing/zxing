/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include <coemain.h>
#include <eikon.hrh>
#include <aknutils.h>
#include <pathinfo.h>
#include <f32file.h>
#include <BAUTILS.H>
#include "ZXingBarcodeReaderAppView.h"
#include "ZXingBarcodeReaderAppUi.h"



// ============================ MEMBER FUNCTIONS ===============================

CZXingBarcodeReaderAppView* CZXingBarcodeReaderAppView::NewL (const TRect& aRect )
	{
	CZXingBarcodeReaderAppView* self = CZXingBarcodeReaderAppView::NewLC (aRect );
	CleanupStack::Pop (self );
	return self;
	}

CZXingBarcodeReaderAppView* CZXingBarcodeReaderAppView::NewLC (const TRect& aRect )
	{
	CZXingBarcodeReaderAppView* self = new (ELeave) CZXingBarcodeReaderAppView;
	CleanupStack::PushL (self );
	self->ConstructL (aRect );
	return self;
	}

void CZXingBarcodeReaderAppView::ConstructL (const TRect& aRect )
	{
	// Create a window for this application view
	CreateWindowL ();

	iTitleFont = AknLayoutUtils::FontFromId(EAknLogicalFontPrimarySmallFont);

	iAppUi = static_cast<CZXingBarcodeReaderAppUi*>(iEikonEnv->EikAppUi());

	// Set the windows size
	SetRect (aRect );

	//Start decoder timer.
	StartTimer();
	
	// Activate the window, which makes it ready to be drawn
	ActivateL ();
	}

CZXingBarcodeReaderAppView::CZXingBarcodeReaderAppView () : iPeriodic(NULL)
	{
	}

CZXingBarcodeReaderAppView::~CZXingBarcodeReaderAppView ()
	{
	if (iCameraWrapper)
		{
	iCameraWrapper->ReleaseAndPowerOff();
		}
	delete iCameraWrapper;
	delete iData;

	ReleaseBackBuffer();
	}

TKeyResponse CZXingBarcodeReaderAppView::OfferKeyEventL(const TKeyEvent& aKeyEvent,TEventCode aType)
	{
	switch ( aKeyEvent.iCode )
		{
		case EKeyOK:
		case EStdKeyDevice3:
			{
			// Capture picture
			iCameraShutterFocusing = EFalse;
			StartFocusing();
			return EKeyWasConsumed;
			}
		case EKeyUpArrow:
			{
			if (iCameraWrapper->State() == CCameraEngine::EEngineViewFinding)
				{
			iCameraWrapper->AdjustDigitalZoom(ETrue);
				}
			return EKeyWasConsumed;
			}
		case EKeyDownArrow:
			{
			if (iCameraWrapper->State() == CCameraEngine::EEngineViewFinding)
				{
			iCameraWrapper->AdjustDigitalZoom(EFalse);

				}
			return EKeyWasConsumed;
			}
		default:
			{
			break;
			}
		};



#ifdef ENABLE_CAMERA_SHUTTER
	// Camera shutter autofocus
	switch ( aKeyEvent.iScanCode )
		{
		case KStdKeyCameraFocus:
		case KStdKeyCameraFocus2:
			{
			// Camera shutter autofocus
			if (aType == EEventKeyDown)
				{
			if (!iAppUi->IsBackCBA())
				{
			iCameraShutterFocusing = ETrue;
			StartFocusing();
				}
			return EKeyWasConsumed;
				}
			else if (aType == EEventKeyUp)
				{
			// Camera state can be EEngineIdle or EEngineFocusing
			if (!iAppUi->IsBackCBA() && (iCameraWrapper->State() == CCameraEngine::EEngineFocusing ||
					iCameraWrapper->State() == CCameraEngine::EEngineIdle))
				{
			iCameraWrapper->FocusCancel();
			CancelCapturedPicture();
			iAppUi->UseOptionsExitCbaL();
				}
			return EKeyWasConsumed;
				}
			}
		default:
			{
			break;
			}
		};
#endif

	return EKeyWasNotConsumed;
	}

void CZXingBarcodeReaderAppView::CancelCapturedPicture(TBool aCleanTexts)
	{
	if (iCameraWrapper && iCameraWrapper->State() == CCameraEngine::EEngineIdle)
		{
	TRAPD(err,iCameraWrapper->StartViewFinderL(iViewFinderSize));
	if (aCleanTexts)
		{
	if (err)
		{
	SetError(_L("Camera viewfinder error %d"), err);                    
		}
	else
		{
	SetTitle(_L("Camera viewfinder"));
		}            
		}
		}
	}

void CZXingBarcodeReaderAppView::Draw(const TRect& /*aRect*/) const
		{
	CWindowGc& gc = SystemGc ();

	// Draw backbuffer that has camera picture
	gc.BitBlt(TPoint(0, 0), iBackBuffer);

	// Draw texts
	DrawTexts(gc);

	// Focus rect
	if (iCameraWrapper && iCameraWrapper->State() == CCameraEngine::EEngineFocusing)
		{
	gc.SetPenColor(KRgbWhite);
	gc.DrawRect(iFocusRect);
		}
		}

void CZXingBarcodeReaderAppView::DrawTexts(CWindowGc& gc) const
		{
	if (iTitle.Length()>0)
		{
	TRect rect(Rect());
	gc.SetPenColor(KRgbWhite);
	gc.UseFont(iTitleFont);
	gc.DrawText(iTitle, rect, rect.Height()/10, CGraphicsContext::ECenter );
	gc.DiscardFont();
		}
		}

void CZXingBarcodeReaderAppView::SizeChanged()
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
	if (Rect().Size().iWidth > Rect().Size().iHeight)
		{
	iViewFinderSize = TSize(Rect().Size().iWidth,Rect().Size().iWidth);
	iCaptureSize = TSize(1280,960); // Captured picture size
		}
	else
		{
	iViewFinderSize = TSize(Rect().Size().iHeight,Rect().Size().iHeight);
	iCaptureSize = TSize(1280,960); // Captured picture size
		}

	// Focus rectangle
	iFocusRect = Rect();
	iFocusRect.Shrink(Rect().Size().iWidth/4, Rect().Size().iHeight/4);

	// Create back buffer where recieved camera pictures are copied
	ReleaseBackBuffer();
	CreateBackBufferL();

	// Power on camera, start viewfinder when MceoCameraReady() received
	if(camErr == KErrNone)
		{
	iCameraWrapper->ReserveAndPowerOn();    
	SetTitle(_L("Camera power on"));
		}
	else
		{
	SetTitle(_L("no camera found"));
		}
	}

void CZXingBarcodeReaderAppView::HandlePointerEventL (
		const TPointerEvent& aPointerEvent )
	{
	if (aPointerEvent.iType == TPointerEvent::EButton1Down)
		{
	// When pointing to screen camera capture picture
	if (!iAppUi->IsBackCBA() && 
			iCameraWrapper && iCameraWrapper->State() == CCameraEngine::EEngineViewFinding)
		{
	iCameraShutterFocusing = EFalse;
	StartFocusing();
		}
	// After captureing, when pointing again to screen camera
	// start viewfinder again
	else if (!iAppUi->IsBackCBA() &&
			iCameraWrapper && iCameraWrapper->State() == CCameraEngine::EEngineIdle)
		{
	CancelCapturedPicture();
	iAppUi->UseOptionsExitCbaL();
		}
		}
	}

void CZXingBarcodeReaderAppView::SetTitle(const TDesC& aTitle)
	{
	iTitle.Copy(aTitle);
	DrawNow();
	}

void CZXingBarcodeReaderAppView::SetError( const TDesC& aMsg, TInt aVal )
	{
	iTitle.Format(aMsg, aVal);
	DrawNow();
	}

void CZXingBarcodeReaderAppView::SetError( const TDesC& aMsg, TInt aVal1, TInt aVal2 )
	{
	iTitle.Format(aMsg, aVal1, aVal2);
	DrawNow();
	}

void CZXingBarcodeReaderAppView::CreateBackBufferL()
	{
	// create back buffer bitmap
	iBackBuffer = new (ELeave) CFbsBitmap;

	User::LeaveIfError( iBackBuffer->Create(Size(),EColor16M));

	// create back buffer graphics context
	iBackBufferDevice = CFbsBitmapDevice::NewL(iBackBuffer);
	User::LeaveIfError(iBackBufferDevice->CreateContext(iBackBufferContext));
	iBackBufferContext->SetPenStyle(CGraphicsContext::ESolidPen);

	iBackBufferContext->SetBrushColor(KRgbBlack);
	iBackBufferContext->Clear();
	}

void CZXingBarcodeReaderAppView::ReleaseBackBuffer()
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

void CZXingBarcodeReaderAppView::MceoCameraReady()
	{
	iAppUi->UseOptionsExitCbaL();

	if (iCameraWrapper->State() == CCameraEngine::EEngineIdle)
		{
	// Prepare camera
	TRAPD(err,iCameraWrapper->PrepareL(iCaptureSize));
	if (err)
		{
	SetError(_L("Camera prepare error %d"), err);
	return;
		}

	// Start viewfinder. Viewfinder pictures starts coming into MceoViewFinderFrameReady();
	TRAPD(err2,iCameraWrapper->StartViewFinderL(iViewFinderSize));
	if (err2)
		{
	SetError(_L("Camera start viewfinder error %d"), err2);
	return;
		}

	SetTitle(_L("Camera viewfinder"));
		}
	}

void CZXingBarcodeReaderAppView::Capture()
	{
	// This method is called when picture is focused with camera shutter and pressed whole down
	// as taking a new picture
#ifdef ENABLE_CAMERA_SHUTTER
	if (iCameraWrapper && !iAppUi->IsBackCBA())
		{
	// No focus supported
	SetTitle(_L("Capturing picture"));
	iCameraWrapper->StopViewFinder();
	TRAPD(err,iCameraWrapper->CaptureL());
	if (err)
		{
	SetError(_L("Camera capture error %d"), err);                    
		}
		}
#endif
	}

void CZXingBarcodeReaderAppView::StartFocusing()
	{
	if (iCameraWrapper && iCameraWrapper->State() == CCameraEngine::EEngineViewFinding)
		{
	if (!iCameraWrapper->IsAutoFocusSupported())
		{
	// No focus supported
	SetTitle(_L("Capturing picture"));
	iCameraWrapper->StopViewFinder();
	TRAPD(err,iCameraWrapper->CaptureL());
	if (err)
		{
	SetError(_L("Camera capture error %d"), err);                    
		}
		}
	else
		{
	// Focusing supported
	iCameraWrapper->StartFocusL();
	SetTitle(_L("Autofocusing..."));                    
		}
		}
	}

void CZXingBarcodeReaderAppView::MceoFocusComplete()
	{
	// CameraEngine state is EEngineIdle
	SetTitle(_L("Focused"));                    

	if (iCameraShutterFocusing)
		{
	// Leave as focused. User must press whole camera shutter down for capturing
	// then CZXingBarcodeReaderAppView::Capture() is called
		}
	else
		{
	// Capture picture after it has focused
	iCameraWrapper->StopViewFinder();
	TRAPD(err,iCameraWrapper->CaptureL());
	if (err)
		{
	SetError(_L("Camera capture error %d"), err);                    
		}
		}
	}

void CZXingBarcodeReaderAppView::MceoCapturedDataReady( TDesC8* aData )
	{
	SetTitle(_L("Saving picture..."));

	delete iData; iData = NULL;
	iData = aData->Alloc();

	if (iCameraWrapper)
		iCameraWrapper->ReleaseImageBuffer();

	TRAP_IGNORE(iAppUi->UseOptionsBackCbaL());

	StorePicture(iData);
	}

void CZXingBarcodeReaderAppView::StorePicture( TDesC8* aData )
	{
	// Create path for filename
	TFileName path = PathInfo::PhoneMemoryRootPath(); 
	path.Append(PathInfo::ImagesPath());

	// Ensure that path exists
	BaflUtils::EnsurePathExistsL(iEikonEnv->FsSession(),path);

	// Get next free filename for the image
	TFileName fileToSave;
	TBool fileExists = ETrue;
	for (TInt i=1 ; i<100 ; i++)
		{
	fileToSave.Copy(path);
	fileToSave.Append(_L("cw_image_"));
	fileToSave.AppendNum(i);
	fileToSave.Append(_L(".jpg"));
	fileExists = BaflUtils::FileExists(iEikonEnv->FsSession(),fileToSave);
	if (!fileExists)
		{
	break;
		}
		}

	// Save file
	if (!fileExists)
		{
	RFile file;
	TInt err = file.Create(iEikonEnv->FsSession(),fileToSave,EFileWrite);
	if (!err)
		{
	file.Write(*aData);
	file.Close();
	SetTitle(fileToSave);
		}
	else
		{
	SetError(_L("File saving error %d"),err);
		}
		}
	else
		{
	SetTitle(_L("File not saved, delete old pictures!"));
		}
	}


void CZXingBarcodeReaderAppView::MceoCapturedBitmapReady( CFbsBitmap* aBitmap )
	{
	if (iBackBufferContext)
		{
	TSize bmpSizeInPixels = aBitmap->SizeInPixels();
	TInt xDelta = (Rect().Width() - bmpSizeInPixels.iWidth) / 2;
	TInt yDelta = (Rect().Height() - bmpSizeInPixels.iHeight) / 2;
	TPoint pos( xDelta, yDelta );

	// Copy received viewfinder picture to back buffer
	iBackBufferContext->BitBlt( pos, aBitmap, TRect( TPoint( 0, 0 ), bmpSizeInPixels ));

	// Update backbuffer into screen 
	SetTitle(_L("New picture"));
		}
	if (iCameraWrapper)
		iCameraWrapper->ReleaseImageBuffer();
	}

void CZXingBarcodeReaderAppView::MceoViewFinderFrameReady( CFbsBitmap& aFrame )
	{
	if (iBackBufferContext)
		{
	TSize bmpSizeInPixels = aFrame.SizeInPixels();
	TInt xDelta = (Rect().Width() - bmpSizeInPixels.iWidth) / 2;
	TInt yDelta = (Rect().Height() - bmpSizeInPixels.iHeight) / 2;
	TPoint pos( xDelta, yDelta );

	// Copy received viewfinder picture to back buffer
	iBackBufferContext->BitBlt( pos, &aFrame, TRect( TPoint( 0, 0 ), bmpSizeInPixels ));

	// Update backbuffer into screen 
	DrawNow();
		}
	if (iCameraWrapper)
		iCameraWrapper->ReleaseViewFinderBuffer();
	}

void CZXingBarcodeReaderAppView::MceoHandleError( TCameraEngineError aErrorType, TInt aError )
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
			SetError(_L("Camera reserved error  (%d)"), aError);
			break;
			}
		case EErrPowerOn:
			{
			SetError(_L("Camera power on error  (%d)"), aError);
			break;
			}
		case EErrViewFinderReady:
			{
			SetError(_L("Camera viewfinder error  (%d)"), aError);
			break;
			}
		case EErrImageReady:
			{
			SetError(_L("Camera image ready error  (%d)"), aError);
			break;
			}
		case EErrAutoFocusInit:
		case EErrAutoFocusMode:
		case EErrAutoFocusArea:
		case EErrAutoFocusRange:
		case EErrAutoFocusType:
		case EErrOptimisedFocusComplete:
			{
			//SetTitle(_L("Try focusing again"));
			break;
			}
		default:
			{
			SetError(_L("Error %d (%d)"), aErrorType, aError);
			break;
			}
		};

	// Try handle error
	CancelCapturedPicture(EFalse);
	iAppUi->UseOptionsExitCbaL();
	}

void CZXingBarcodeReaderAppView::MceoHandleOtherEvent( const TECAMEvent& /*aEvent*/ )
	{
	}


void CZXingBarcodeReaderAppView::StopTimer()
{
    if(iPeriodic)
        iPeriodic->Cancel();
}




// End of File
