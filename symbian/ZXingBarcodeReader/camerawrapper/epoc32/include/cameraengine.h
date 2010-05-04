/*
* ============================================================================
*  Name        : cameraengine.h
*  Part of     : CameraWrapper
*  Description : Camera engine class declaration
*  Version     : %version: 2 %
*
*  Copyright (c) 2009 Nokia Corporation.
*  This material, including documentation and any related
*  computer programs, is protected by copyright controlled by
*  Nokia Corporation.
* ==============================================================================
*/

#ifndef CCAMERAENGINE_H
#define CCAMERAENGINE_H

// INCLUDES
#include <e32base.h>
#include <ecam.h>

// FORWARD DECLARATIONS
class CCameraEnginePrivate;
class MCameraEngineObserver;
class CCameraAdvancedSettings;

NONSHARABLE_CLASS( CCameraEngine ) : public CBase
  {
public:

  enum TCameraEngineState
      {
      EEngineNotReady,
      EEngineIdle,
      EEngineViewFinding,
      EEngineCapturing,
      EEngineFocusing
      };

  IMPORT_C static CCameraEngine* NewL( TInt aCameraHandle, 
                                       TInt aPriority, 
                                       MCameraEngineObserver* aObserver );
    IMPORT_C ~CCameraEngine();
  
public:

  /**
   * Returns the current state (TCameraEngineState)
   * of the camera engine.
   */
  IMPORT_C TCameraEngineState State() const;

  /**
   * Returns true if the camera has been reserved and
   * powered on.
   */
  IMPORT_C TBool IsCameraReady() const;

  /**
   * Returns true if the camera supports AutoFocus.
   */
  IMPORT_C TBool IsAutoFocusSupported() const;
  
  /**
   * Captures an image. When complete, observer will receive
   * MceoCapturedDataReady() or MceoCapturedBitmapReady() callback,
   * depending on which image format was used in PrepareL().
   * @leave May leave with KErrNotReady if camera is not
   * reserved or prepared for capture.
   */   
  IMPORT_C void CaptureL();
  
  /**
   * Reserves and powers on the camera. When complete,
   * observer will receive MceoCameraReady() callback
   *
   */
  IMPORT_C void ReserveAndPowerOn();

  /**
   * Releases and powers off the camera
   *
   */
  IMPORT_C void ReleaseAndPowerOff();

  /**
   * Prepares for image capture.
   * @param aCaptureSize requested capture size. On return,
   * contains the selected size (closest match)
   * @param aFormat Image format to use. Default is JPEG with
   * EXIF information as provided by the camera module
   * @leave KErrNotSupported, KErrNoMemory, KErrNotReady
   */
  IMPORT_C void PrepareL( TSize& aCaptureSize,               
      CCamera::TFormat aFormat = CCamera::EFormatExif );
      
  /**
   * Starts the viewfinder. Observer will receive 
   * MceoViewFinderFrameReady() callbacks periodically.
   * @param aSize requested viewfinder size. On return,
   * contains the selected size.
   * 
   * @leave KErrNotSupported is viewfinding with bitmaps is not
   * supported, KErrNotReady 
   */
  IMPORT_C void StartViewFinderL( TSize& aSize );

  /**
   * Stops the viewfinder if active.
   */
  IMPORT_C void StopViewFinder();
  
  /**
   * Releases memory for the last received viewfinder frame.
   * Client must call this in response to MceoViewFinderFrameReady()
   * callback, after drawing the viewfinder frame is complete.
   */
  IMPORT_C void ReleaseViewFinderBuffer();
  
  /**
   * Releases memory for the last captured image.
   * Client must call this in response to MceoCapturedDataReady()
   * or MceoCapturedBitmapReady()callback, after processing the 
   * data/bitmap is complete.
   */
  IMPORT_C void ReleaseImageBuffer();
  
  /**
   * Starts focusing. Does nothing if AutoFocus is not supported.
   * When complete, observer will receive MceoFocusComplete()
   * callback.
   * @leave KErrInUse, KErrNotReady
   */
  IMPORT_C void StartFocusL();
  
  /**
   * Cancels the ongoing focusing operation.
   */
  IMPORT_C void FocusCancel();

  /**
   * Gets a bitfield of supported focus ranges.
   * @param aSupportedRanges a bitfield of either TAutoFocusRange
   * (S60 3.0/3.1 devices) or TFocusRange (S60 3.2 and onwards) values
   */
  IMPORT_C void SupportedFocusRanges( TInt& aSupportedRanges ) const;
  
  /**
   * Sets the focus range
   * @param aFocusRange one of the values returned by
   * SupportedFocusRanges().
   */
  IMPORT_C void SetFocusRange( TInt aFocusRange );
  
  /**
   * Returns a pointer to CCamera object used by the engine.
   * Allows getting access to additional functionality
   * from CCamera - do not use for functionality already provided
   * by CCameraEngine methods.
   */
  IMPORT_C CCamera* Camera();
  
  /**
   * Returns a pointer to CCameraAdvancedSettings object used by 
   * the engine. May be NULL if adv. settings is not available.
   * Allows getting access to additional functionality
   * from CCameraAdvancedSettings - do not use for functionality already 
   * provided by CCameraEngine methods.
   */
  IMPORT_C CCamera::CCameraAdvancedSettings* AdvancedSettings();
  
  /**
   * Static function that returns the number of cameras on the device.
   */
  IMPORT_C static TInt CamerasAvailable();
  
  /**
   * Maximum digital zoom value. 0 if digital zoom is not supported
   */
  IMPORT_C TInt MaxDigitalZoom();
  
  /**
   * Current digital zoom value
   */
  IMPORT_C TInt DigitalZoom();
  
  /**
   * Adjust digital zoom. set aTele to ETrue to increase zoom (tele)
   * or EFalse to decrease zoom (wide)
   * @return Returns the new zoom level or KErrNotSupported
   */
  IMPORT_C TInt AdjustDigitalZoom( TBool aTele );
  
  /**
   * Returns a bitfield of supported exposure modes
   * See CCamera::TExposure
   */
  IMPORT_C TInt SupportedExposureModes();
  
  /**
   * Returns the current exposure mode
   * See CCamera::TExposure
   */
  IMPORT_C TInt Exposure();
  
  /**
   * Set camera exposure mode   
   * See CCamera::TExposure
   * @param aExposure One of the modes from SupportedExposureModes
   * @return KErrNone or KErrNotSupported
   */
  IMPORT_C TInt SetExposure( TInt aExposure );

  /**
   * Returns a bitfield of supported flash modes
   * See CCamera::TFlash
   */
  IMPORT_C TInt SupportedFlashModes();
  
  /**
   * Returns the current flash mode
   * See CCamera::TFlash
   */
  IMPORT_C TInt Flash();

  /**
   * Set camera flash mode
   * See CCamera::TFlash
   * @param aFlash One of the modes from SupportedFlashModes
   * @return KErrNone or KErrNotSupported
   */
  IMPORT_C TInt SetFlash( TInt aFlash );

protected:
    CCameraEngine();

private:
    CCameraEnginePrivate* iPimpl;
  };

#endif //CCAMERAENGINE_H
