package com.google.zxing.client.android;

import com.google.zxing.client.android.camera.CameraManager;

/**
 * A Mediator class that keeps several objects in place that can be accessed
 * from the decoding and capture classes without binding these together. Before
 * this Mediator class the objects were just passed between each other (in ctors
 * mostly) introducing high coupling. This Mediator tries to solves this.
 * 
 * @author Hans Cappelle, Twitter: @cappha
 * 
 */
public class Mediator {

	/**
	 * the singleton instance
	 */
	private static Mediator mInstance;

	/**
	 * the viewfinderView object responsible for drawing the resultpoints
	 */
	private ViewfinderView mViewfinderView;

	/**
	 * a camera manager that keeps track of camera state, resolution, ...
	 */
	private CameraManager mCameraManager;

	/**
	 * the capture activity that the user has open
	 */
	private CaptureActivity mActivity;

	/**
	 * let's keep this a singleton
	 */
	private Mediator() {
		// nothing to do, just keeping this def ctor private
	}

	/**
	 * use this to retrieve the only instance
	 * 
	 * @return a single instance
	 */
	public static Mediator getInstance() {
		if (mInstance == null)
			mInstance = new Mediator();
		return mInstance;
	}

	/**
	 * 
	 * @param viewfinderView
	 */
	public void setViewfinderView(ViewfinderView viewfinderView) {
		mViewfinderView = viewfinderView;
	}

	/**
	 * 
	 * @return viewfinderView
	 */
	public ViewfinderView getViewfinderView() {
		return mViewfinderView;
	}

	/**
	 * 
	 * @param cameraManager
	 */
	public void setCameraManager(CameraManager cameraManager) {
		mCameraManager = cameraManager;
	}

	/**
	 * 
	 * @return cameraManager
	 */
	public CameraManager getCameraManager() {
		return mCameraManager;
	}

	/**
	 * 
	 * @param activity
	 */
	public void setCaptureActivity(CaptureActivity activity) {
		mActivity = activity;
	}

	/**
	 * 
	 * @return activity
	 */
	public CaptureActivity getCaptureActivity() {
		return mActivity;
	}

}
