/*
 * Copyright (c) 2009 Nokia Corporation.
 */

#ifndef __CAMERAWRAPPEREXAMPLEDOCUMENT_h__
#define __CAMERAWRAPPEREXAMPLEDOCUMENT_h__

// INCLUDES
#include <akndoc.h>

// FORWARD DECLARATIONS
class CZXingBarcodeReaderAppUi;
class CEikApplication;

// CLASS DECLARATION
class CZXingBarcodeReaderDocument : public CAknDocument
    {
    public:
        // Constructors and destructor
        static CZXingBarcodeReaderDocument* NewL (CEikApplication& aApp );
        static CZXingBarcodeReaderDocument* NewLC (CEikApplication& aApp );
        virtual ~CZXingBarcodeReaderDocument ();
    
    public:
        // Functions from base classes
        CEikAppUi* CreateAppUiL ();
    
    private:
        // Constructors
        void ConstructL ();
        CZXingBarcodeReaderDocument (CEikApplication& aApp );

    private:
        // Data 
    
    };

#endif // __CAMERAWRAPPEREXAMPLEDOCUMENT_h__

// End of File
