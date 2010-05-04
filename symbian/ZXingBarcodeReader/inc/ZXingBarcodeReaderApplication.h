/*
 * Copyright (c) 2009 Nokia Corporation.
 */

#ifndef __CAMERAWRAPPEREXAMPLEAPPLICATION_H__
#define __CAMERAWRAPPEREXAMPLEAPPLICATION_H__

// INCLUDES
#include <aknapp.h>
#include "ZXingBarcodeReader.hrh"

// UID for the application;
// this should correspond to the uid defined in the mmp file
const TUid KUidCameraWrapperExampleApp =
    {
    _UID3
    };

// CLASS DECLARATION

class CZXingBarcodeReaderApplication : public CAknApplication
    {
    public:
        // Functions from base classes
        TUid AppDllUid () const;
    
    protected:
        // Functions from base classes
        CApaDocument* CreateDocumentL ();
    };

#endif // __CAMERAWRAPPEREXAMPLEAPPLICATION_H__

// End of File
