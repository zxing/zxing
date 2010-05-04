/*
 * Copyright (c) 2009 Nokia Corporation.
 */

#ifndef __CAMERAWRAPPEREXAMPLEDOCUMENT_h__
#define __CAMERAWRAPPEREXAMPLEDOCUMENT_h__

// INCLUDES
#include <akndoc.h>

// FORWARD DECLARATIONS
class CCameraWrapperExampleAppUi;
class CEikApplication;

// CLASS DECLARATION
class CCameraWrapperExampleDocument : public CAknDocument
    {
    public:
        // Constructors and destructor
        static CCameraWrapperExampleDocument* NewL (CEikApplication& aApp );
        static CCameraWrapperExampleDocument* NewLC (CEikApplication& aApp );
        virtual ~CCameraWrapperExampleDocument ();
    
    public:
        // Functions from base classes
        CEikAppUi* CreateAppUiL ();
    
    private:
        // Constructors
        void ConstructL ();
        CCameraWrapperExampleDocument (CEikApplication& aApp );

    private:
        // Data 
    
    };

#endif // __CAMERAWRAPPEREXAMPLEDOCUMENT_h__

// End of File
