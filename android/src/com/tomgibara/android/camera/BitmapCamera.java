package com.tomgibara.android.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

// This is public-domain code generously supplied to the developer community
// by Tom Gibara, at http://www.tomgibara.com/android/camera-source

/**
 * A CameraSource implementation that repeatedly captures a single bitmap.
 * 
 * @author Tom
 *
 */

public class BitmapCamera implements CameraSource {

	private final Bitmap bitmap;
	private final Rect bounds;
	private final Paint paint = new Paint();

	public BitmapCamera(Bitmap bitmap, int width, int height) {
		this.bitmap = bitmap;
		bounds = new Rect(0, 0, width, height);

		paint.setFilterBitmap(true);
		paint.setAntiAlias(true);
	}
	
	@Override
	public int getWidth() {
		return bounds.right;
	}
	
	@Override
	public int getHeight() {
		return bounds.bottom;
	}
	
	@Override
	public boolean open() {
		return true;
	}
	
	@Override
	public boolean capture(Canvas canvas) {
		if (
				bounds.right == bitmap.width() &&
				bounds.bottom == bitmap.height()) {
			canvas.drawBitmap(bitmap, 0, 0, null);
		} else {
			canvas.drawBitmap(bitmap, null, bounds, paint);
		}
		return true;
	}
	
	@Override
	public void close() {
		/* nothing to do */
	}
}
