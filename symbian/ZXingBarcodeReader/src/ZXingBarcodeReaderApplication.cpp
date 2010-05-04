/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include "ZXingBarcodeReader.hrh"
#include "ZXingBarcodeReaderDocument.h"
#include "ZXingBarcodeReaderApplication.h"

// ============================ MEMBER FUNCTIONS ===============================

CApaDocument* CZXingBarcodeReaderApplication::CreateDocumentL ()
    {
    // Create an CameraWrapperExample document, and return a pointer to it
    return CZXingBarcodeReaderDocument::NewL (*this );
    }

TUid CZXingBarcodeReaderApplication::AppDllUid () const
    {
    // Return the UID for the CameraWrapperExample application
    return KUidCameraWrapperExampleApp;
    }

// End of File
