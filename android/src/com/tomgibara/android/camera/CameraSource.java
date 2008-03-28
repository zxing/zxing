package com.tomgibara.android.camera;

import android.graphics.Canvas;

// This is public-domain code generously supplied to the developer community
// by Tom Gibara, at http://www.tomgibara.com/android/camera-source

/**
 * Provides a simple abstraction for obtaining preview captures from a camera
 * on the Android platform. This interface intended to be used temporarily while
 * the Google Android SDK fails to support camera capture from desktop devices
 * (webcams etc).
 * 
 * @author Tom Gibara
 */

public interface CameraSource {

	static final String LOG_TAG = "camera";
	
	/**
	 * Open the camera source for subsequent use via calls to capture().
	 * 
	 * @return true if the camera source was successfully opened.
	 */
	
	boolean open();
	
	/**
	 * Close the camera source. Calling close on a closed CameraSource is
	 * permitted but has no effect. The camera source may be reopened after
	 * being closed.
	 */
	
	void close();
	
	/**
	 * The width of the captured image.
	 * 
	 * @return the width of the capture in pixels
	 */
	
	int getWidth();
	
	/**
	 * The height of the captured image.
	 * 
	 * @return the height of the capture in pixels
	 */
	
	int getHeight();
	
	/**
	 * Attempts to render the current camera view onto the supplied canvas.
	 * The capture will be rendered into the rectangle (0,0,width,height).
	 * Outstanding transformations on the canvas may alter this.
	 * 
	 * @param canvas the canvas to which the captured pixel data will be written
	 * @return true iff a frame was successfully written to the canvas
	 */
	
	boolean capture(Canvas canvas);
	
}
