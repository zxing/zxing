/*
* ============================================================================
*  Name        : cameraengineobserver.h
*  Part of     : CameraWrapper
*  Description : Observer interface for camera engine (wrapper DLL)
*  Version     : %version: 1 %
*
*  Copyright (c) 2009 Nokia Corporation.
*  This material, including documentation and any related
*  computer programs, is protected by copyright controlled by
*  Nokia Corporation.
* ==============================================================================
*/

#ifndef __CCAMERAENGINEOBSERVER_H__
#define __CCAMERAENGINEOBSERVER_H__

// FORWARD DECLARATIONS
class CFbsBitmap;
class TECAMEvent;

enum TCameraEngineError
  {
  EErrReserve,
  EErrPowerOn,
  EErrViewFinderReady,
  EErrImageReady,
  EErrAutoFocusInit,
  EErrAutoFocusMode,
  EErrAutoFocusArea,
  EErrAutoFocusRange,
  EErrAutoFocusType,
  EErrOptimisedFocusComplete,  
  };
  

class MCameraEngineObserver
  {
public:

  /**
    * Camera is ready to use for capturing images.
    */
  virtual void MceoCameraReady() = 0;
  
  /**
    * Camera AF lens has attained optimal focus
    */
  virtual void MceoFocusComplete() = 0;
  
  /**
    * Captured data is ready - call CCameraEngine::ReleaseImageBuffer()
    * after processing/saving the data (typically, JPG-encoded image)
    * @param aData Pointer to a descriptor containing a frame of camera data.
    */
  virtual void MceoCapturedDataReady( TDesC8* aData ) = 0;
  
  /**
    * Captured bitmap is ready.
    * after processing/saving the image, call 
    * CCameraEngine::ReleaseImageBuffer() to free the bitmap.
    * @param aBitmap Pointer to an FBS bitmap containing a captured image.
    */  
  virtual void MceoCapturedBitmapReady( CFbsBitmap* aBitmap ) = 0;
  
  /**
    * A new viewfinder frame is ready.
    * after displaying the frame, call 
    * CCameraEngine::ReleaseViewFinderBuffer()
    * to free the bitmap.
    * @param aFrame Pointer to an FBS bitmap containing a viewfinder frame.
    */  
  virtual void MceoViewFinderFrameReady( CFbsBitmap& aFrame ) = 0;
  
   /**
    * Notifies clients about errors in camera engine
    * @param aErrorType type of error (see TCameraEngineError)
    * @param aError Symbian system-wide error code
    */
  virtual void MceoHandleError( TCameraEngineError aErrorType, TInt aError ) = 0;

   /**
    * Notifies client about other events not recognized by camera engine.
    * The default implementation is empty.
    * @param aEvent camera event (see MCameraObserver2::HandleEvent())
    */
  virtual void MceoHandleOtherEvent( const TECAMEvent& /*aEvent*/ ) {}
  };

#endif // __CCAMERAENGINEOBSERVER_H__

// eof
