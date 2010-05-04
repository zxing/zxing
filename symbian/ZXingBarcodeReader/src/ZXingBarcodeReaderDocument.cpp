/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include "ZXingBarcodeReaderAppUi.h"
#include "ZXingBarcodeReaderDocument.h"

// ============================ MEMBER FUNCTIONS ===============================

CZXingBarcodeReaderDocument* CZXingBarcodeReaderDocument::NewL (
        CEikApplication& aApp )
    {
    CZXingBarcodeReaderDocument* self = NewLC (aApp );
    CleanupStack::Pop (self );
    return self;
    }

CZXingBarcodeReaderDocument* CZXingBarcodeReaderDocument::NewLC (
        CEikApplication& aApp )
    {
    CZXingBarcodeReaderDocument* self =
            new (ELeave) CZXingBarcodeReaderDocument (aApp );

    CleanupStack::PushL (self );
    self->ConstructL ();
    return self;
    }

void CZXingBarcodeReaderDocument::ConstructL ()
    {
    }

CZXingBarcodeReaderDocument::CZXingBarcodeReaderDocument (
        CEikApplication& aApp ) :
    CAknDocument (aApp )
    {
    }

CZXingBarcodeReaderDocument::~CZXingBarcodeReaderDocument ()
    {
    }

CEikAppUi* CZXingBarcodeReaderDocument::CreateAppUiL ()
    {
    return new (ELeave) CZXingBarcodeReaderAppUi;
    }

// End of File
