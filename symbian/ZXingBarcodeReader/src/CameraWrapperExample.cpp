/*
 * Copyright (c) 2009 Nokia Corporation.
 */

// INCLUDE FILES
#include <eikstart.h>
#include "CameraWrapperExampleApplication.h"

LOCAL_C CApaApplication* NewApplication ()
    {
    return new CCameraWrapperExampleApplication;
    }

GLDEF_C TInt E32Main ()
    {
    return EikStart::RunApplication (NewApplication );
    }

