/*
 * Copyright (c) 2009 Nokia Corporation.
 */

#ifndef __CAMERAWRAPPEREXAMPLE_PAN__
#define __CAMERAWRAPPEREXAMPLE_PAN__

enum TCameraWrapperExamplePanics
    {
    ECameraWrapperExampleUi = 1
    // add further panics here
    };

inline void Panic (TCameraWrapperExamplePanics aReason )
    {
    _LIT (applicationName, "ZXingBarcodeReader" );
    User::Panic (applicationName, aReason );
    }

#endif // __CAMERAWRAPPEREXAMPLE_PAN__
