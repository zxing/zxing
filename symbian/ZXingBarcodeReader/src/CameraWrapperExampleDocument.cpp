/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include "CameraWrapperExampleAppUi.h"
#include "CameraWrapperExampleDocument.h"

// ============================ MEMBER FUNCTIONS ===============================

CCameraWrapperExampleDocument* CCameraWrapperExampleDocument::NewL (
        CEikApplication& aApp )
    {
    CCameraWrapperExampleDocument* self = NewLC (aApp );
    CleanupStack::Pop (self );
    return self;
    }

CCameraWrapperExampleDocument* CCameraWrapperExampleDocument::NewLC (
        CEikApplication& aApp )
    {
    CCameraWrapperExampleDocument* self =
            new (ELeave) CCameraWrapperExampleDocument (aApp );

    CleanupStack::PushL (self );
    self->ConstructL ();
    return self;
    }

void CCameraWrapperExampleDocument::ConstructL ()
    {
    }

CCameraWrapperExampleDocument::CCameraWrapperExampleDocument (
        CEikApplication& aApp ) :
    CAknDocument (aApp )
    {
    }

CCameraWrapperExampleDocument::~CCameraWrapperExampleDocument ()
    {
    }

CEikAppUi* CCameraWrapperExampleDocument::CreateAppUiL ()
    {
    return new (ELeave) CCameraWrapperExampleAppUi;
    }

// End of File
