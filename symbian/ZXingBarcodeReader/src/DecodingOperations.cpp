#include "ZXingBarcodeReaderAppView.h"
#include <e32std.h>

#include <zxing/qrcode/QRCodeReader.h>

#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/Binarizer.h>
#include <zxing/BinaryBitmap.h>
#include <CameraImage.h>
#include <string>

using namespace zxing;
using namespace zxing::qrcode;

void CZXingBarcodeReaderAppView::StartTimer()
	{
	const TInt tickInterval=2000000;
	iPeriodic=CPeriodic::NewL(0); // neutral priority

	//CleanupStack::PushL(iPeriodic);

	iPeriodic->Start(tickInterval,tickInterval,TCallBack(&CZXingBarcodeReaderAppView::Tick, this));

//	CleanupStack::PopAndDestroy(iPeriodic);
	}

TInt CZXingBarcodeReaderAppView::Tick(TAny* aObject)
	{
	// cast, and call non-static function
	((CZXingBarcodeReaderAppView*)aObject)->decodeBackbufferImage();
	return 1;
	}

void CZXingBarcodeReaderAppView::decodeBackbufferImage()
	{
	QRCodeReader decoder;

	CameraImage image;
	image.setImage(iBackBuffer);


	Ref<Result> res;

	try
		{
		Ref<LuminanceSource> imageRef(new CameraImage(image));
		GlobalHistogramBinarizer* binz = new GlobalHistogramBinarizer(imageRef);
	
		Ref<Binarizer> bz (binz);
		BinaryBitmap* bb = new BinaryBitmap(bz);
	
		Ref<BinaryBitmap> ref(bb);
	
		res = decoder.decode(ref);
	
		string string = res->getText()->getText();
		HBufC8 *pHeap8 = HBufC8::NewMaxLC(string.size());
		pHeap8->Des().Copy((const TUint8 *)string.c_str());
		
		HBufC *pHeap16 = HBufC::NewMaxLC(pHeap8->Length());
		pHeap16->Des().Copy(*pHeap8);
		
		ShowResultL(*pHeap16);
		}
	catch(zxing::Exception& e)
		{
			string string = "Error...retrying...";
			HBufC8 *pHeap8 = HBufC8::NewMaxLC(string.size());
			pHeap8->Des().Copy((const TUint8 *)string.c_str());
			
			HBufC *pHeap16 = HBufC::NewMaxLC(pHeap8->Length());
			pHeap16->Des().Copy(*pHeap8);
			
			ShowResultL(*pHeap16);
		}
	}

void CZXingBarcodeReaderAppView::ShowResultL(TDesC16& message)
	{
	if (!iNote)
		{
	// Create the note once
	iNote = CAknInfoPopupNoteController::NewL();
		}
	// Hide the note. The last note may be visible when creating the second
	iNote->HideInfoPopupNote();

	// Set the time delay period before the popup is shown (in milliseconds)
	iNote->SetTimeDelayBeforeShow(100);

	// Set the time period of how long the popup is in the view (in milliseconds)
	iNote->SetTimePopupInView(2*1000);

	// Note text
	iNote->SetTextL(message);
	
	TRect rect(Rect());
	
	// Note position
	iNote->SetPositionAndAlignment(TPoint(rect.Width()/5,rect.Height()/7),EHLeftVTop);

	// Show note
	iNote->ShowInfoPopupNote();
	}
