/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include "CameraWrapperExample.hrh"
#include "CameraWrapperExampleDocument.h"
#include "CameraWrapperExampleApplication.h"

// ============================ MEMBER FUNCTIONS ===============================

CApaDocument* CCameraWrapperExampleApplication::CreateDocumentL ()
    {
    // Create an CameraWrapperExample document, and return a pointer to it
    return CCameraWrapperExampleDocument::NewL (*this );
    }

TUid CCameraWrapperExampleApplication::AppDllUid () const
    {
    // Return the UID for the CameraWrapperExample application
    return KUidCameraWrapperExampleApp;
    }

// End of File
