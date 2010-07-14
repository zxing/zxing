#include "ZXingBarcodeReaderAppView.h"
#include <e32std.h>

#include <zxing/qrcode/QRCodeReader.h>
#include <zxing/MultiFormatReader.h>

#include <zxing/common/GlobalHistogramBinarizer.h>
#include <zxing/Binarizer.h>
#include <zxing/BinaryBitmap.h>
#include <CameraImage.h>
#include <string>
#include <aknmessagequerydialog.h>
#include "ZXingBarcodeReader_0xEF24C10A.rsg"

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
    MultiFormatReader decoder;

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
			/*string string = "Error...retrying...";
			HBufC8 *pHeap8 = HBufC8::NewMaxLC(string.size());
			pHeap8->Des().Copy((const TUint8 *)string.c_str());
			
			HBufC *pHeap16 = HBufC::NewMaxLC(pHeap8->Length());
			pHeap16->Des().Copy(*pHeap8);
			
			ShowResultL(*pHeap16);*/
		}
	}

void CZXingBarcodeReaderAppView::ShowResultL(TDesC16& message)
	{
	 StopTimer();
	    
	CAknMessageQueryDialog* dlg = new (ELeave) CAknMessageQueryDialog ();
	
	dlg->PrepareLC(R_TEXT_QUERY_DIALOG );
	   
	//HBufC* title = NULL;	
	//title = iEikonEnv->AllocReadResourceLC ( TEXT_DIALOG_TITLE );
	dlg->QueryHeading ()->SetTextL (_L("Information") );
		
	dlg->SetMessageTextL ( message );
	
	dlg->RunLD();
	
	CleanupStack::PopAndDestroy();
	
	StartTimer();
	}
