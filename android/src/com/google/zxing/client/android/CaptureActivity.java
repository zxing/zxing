/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.result.ResultButtonListener;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;

import java.io.IOException;

/**
 * The barcode reader activity itself. This is loosely based on the CameraPreview
 * example included in the Android SDK.
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

  private static final int SHARE_ID = Menu.FIRST;
  private static final int SETTINGS_ID = Menu.FIRST + 1;
  private static final int HELP_ID = Menu.FIRST + 2;
  private static final int ABOUT_ID = Menu.FIRST + 3;

  private static final int MAX_RESULT_IMAGE_SIZE = 150;
  private static final int INTENT_RESULT_DURATION = 1500;
  private static final float BEEP_VOLUME = 0.15f;

  public CaptureActivityHandler mHandler;

  private ViewfinderView mViewfinderView;
  private View mStatusView;
  private View mResultView;
  private MediaPlayer mMediaPlayer;
  private Result mLastResult;
  private boolean mHasSurface;
  private boolean mPlayBeep;
  private boolean mScanIntent;
  private String mDecodeMode;
  /**
   * When the beep has finished playing, rewind to queue up another one.
   */
  private final OnCompletionListener mBeepListener = new BeepListener();

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    CameraManager.init(getApplication());
    mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    mResultView = findViewById(R.id.result_view);
    mStatusView = findViewById(R.id.status_view);
    mHandler = null;
    mLastResult = null;
    mHasSurface = false;
  }

  @Override
  protected void onResume() {
    super.onResume();

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (mHasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Intent intent = getIntent();
    if (intent != null && (intent.getAction().equals(Intents.Scan.ACTION) ||
        intent.getAction().equals(Intents.Scan.DEPRECATED_ACTION))) {
      mScanIntent = true;
      mDecodeMode = intent.getStringExtra(Intents.Scan.MODE);
      resetStatusView();
    } else {
      mScanIntent = false;
      mDecodeMode = null;
      if (mLastResult == null) {
        resetStatusView();
      }
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    mPlayBeep = prefs.getBoolean(PreferencesActivity.KEY_PLAY_BEEP, true);
    initBeepSound();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mHandler != null) {
      mHandler.quitSynchronously();
      mHandler = null;
    }
    CameraManager.get().closeDriver();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (mScanIntent) {
        setResult(RESULT_CANCELED);
        finish();
        return true;
      } else if (mLastResult != null) {
        resetStatusView();
        mHandler.sendEmptyMessage(R.id.restart_preview);
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
      // Handle these events so they don't launch the Camera app
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, SHARE_ID, 0, R.string.menu_share).setIcon(R.drawable.share_barcode);
    menu.add(0, SETTINGS_ID, 0, R.string.menu_settings)
        .setIcon(android.R.drawable.ic_menu_preferences);
    menu.add(0, HELP_ID, 0, R.string.menu_help)
        .setIcon(android.R.drawable.ic_menu_help);
    menu.add(0, ABOUT_ID, 0, R.string.menu_about)
        .setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }

  // Don't display the share menu item if the result overlay is showing.
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(SHARE_ID).setVisible(mLastResult == null);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case SHARE_ID: {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(this, ShareActivity.class.getName());
        startActivity(intent);
        break;
      }
      case SETTINGS_ID: {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(this, PreferencesActivity.class.getName());
        startActivity(intent);
        break;
      }
      case HELP_ID: {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_help);
        builder.setMessage(R.string.msg_help);
        builder.setPositiveButton(R.string.button_ok, null);
        builder.show();
        break;
      }
      case ABOUT_ID: {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_about);
        builder.setMessage(getString(R.string.msg_about) + "\n\n" + getString(R.string.zxing_url));
        builder.setIcon(R.drawable.zxing_icon);
        builder.setPositiveButton(R.string.button_open_browser, mAboutListener);
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.show();
        break;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onConfigurationChanged(Configuration config) {
    // Do nothing, this is to prevent the activity from being restarted when the keyboard opens.
    super.onConfigurationChanged(config);
  }

  private final DialogInterface.OnClickListener mAboutListener = new DialogInterface.OnClickListener() {
    public void onClick(android.content.DialogInterface dialogInterface, int i) {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.zxing_url)));
      startActivity(intent);
    }
  };

  public void surfaceCreated(SurfaceHolder holder) {
    if (!mHasSurface) {
      mHasSurface = true;
      initCamera(holder);
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    mHasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show the results.
   *
   * @param rawResult The contents of the barcode.
   * @param barcode   A greyscale bitmap of the camera data which was decoded.
   * @param duration  How long the decoding took in milliseconds.
   */
  public void handleDecode(Result rawResult, Bitmap barcode, int duration) {
    mLastResult = rawResult;
    playBeepSound();
    drawResultPoints(barcode, rawResult);

    if (mScanIntent) {
      handleDecodeForScanIntent(rawResult, barcode, duration);
    } else {
      mStatusView.setVisibility(View.GONE);
      mViewfinderView.setVisibility(View.GONE);
      mResultView.setVisibility(View.VISIBLE);

      ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
      barcodeImageView.setMaxWidth(MAX_RESULT_IMAGE_SIZE);
      barcodeImageView.setMaxHeight(MAX_RESULT_IMAGE_SIZE);
      barcodeImageView.setImageBitmap(barcode);

      TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
      formatTextView.setText(getString(R.string.msg_default_format) + ": " +
          rawResult.getBarcodeFormat().toString());

      ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
      TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
      typeTextView.setText(getString(R.string.msg_default_type) + ": " +
          resultHandler.getType().toString());

      TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
      CharSequence title = getString(resultHandler.getDisplayTitle());
      SpannableStringBuilder styled = new SpannableStringBuilder(title + "\n\n");
      styled.setSpan(new UnderlineSpan(), 0, title.length(), 0);
      styled.append(resultHandler.getDisplayContents());
      contentsTextView.setText(styled);

      int buttonCount = resultHandler.getButtonCount();
      ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
      buttonView.requestFocus();
      for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
        Button button = (Button) buttonView.getChildAt(x);
        if (x < buttonCount) {
          button.setVisibility(View.VISIBLE);
          button.setText(resultHandler.getButtonText(x));
          button.setOnClickListener(new ResultButtonListener(resultHandler, x));
        } else {
          button.setVisibility(View.GONE);
        }
      }
    }
  }

  /**
   * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
   *
   * @param barcode   A bitmap of the captured image.
   * @param rawResult The decoded results which contains the points to draw.
   */
  private void drawResultPoints(Bitmap barcode, Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      Canvas canvas = new Canvas(barcode);
      Paint paint = new Paint();
      paint.setColor(getResources().getColor(R.color.result_image_border));
      paint.setStrokeWidth(3);
      paint.setStyle(Paint.Style.STROKE);
      Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
      canvas.drawRect(border, paint);

      paint.setColor(getResources().getColor(R.color.result_points));
      if (points.length == 2) {
        paint.setStrokeWidth(4);
        canvas.drawLine(points[0].getX(), points[0].getY(), points[1].getX(),
            points[1].getY(), paint);
      } else {
        paint.setStrokeWidth(10);
        for (int x = 0; x < points.length; x++) {
          canvas.drawPoint(points[x].getX(), points[x].getY(), paint);
        }
      }
    }
  }

  private void handleDecodeForScanIntent(Result rawResult, Bitmap barcode, int duration) {
    mViewfinderView.drawResultBitmap(barcode);

    // Since this message will only be shown for a second, just tell the user what kind of
    // barcode was found (e.g. contact info) rather than the full contents, which they won't
    // have time to read.
    ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
    TextView textView = (TextView) findViewById(R.id.status_text_view);
    textView.setGravity(Gravity.CENTER);
    textView.setTextSize(18.0f);
    textView.setText(getString(resultHandler.getDisplayTitle()));

    mStatusView.setBackgroundColor(getResources().getColor(R.color.transparent));

    // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
    // the deprecated intent is retired.
    Intent intent = new Intent(getIntent().getAction());
    intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
    intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
    Message message = Message.obtain(mHandler, R.id.return_scan_result);
    message.obj = intent;
    mHandler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
  }

  /**
   * Creates the beep MediaPlayer in advance so that the sound can be triggered with the least
   * latency possible.
   */
  private void initBeepSound() {
    if (mPlayBeep && mMediaPlayer == null) {
      mMediaPlayer = new MediaPlayer();
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_SYSTEM);
      mMediaPlayer.setOnCompletionListener(mBeepListener);

      AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
      try {
        mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(),
            file.getLength());
        file.close();
        mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
        mMediaPlayer.prepare();
      } catch (IOException e) {
        mMediaPlayer = null;
      }
    }
  }

  private void playBeepSound() {
    if (mPlayBeep && mMediaPlayer != null) {
      mMediaPlayer.start();
    }
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    CameraManager.get().openDriver(surfaceHolder);
    if (mHandler == null) {
      boolean beginScanning = mLastResult == null;
      mHandler = new CaptureActivityHandler(this, mDecodeMode, beginScanning);
    }
  }

  private void resetStatusView() {
    mResultView.setVisibility(View.GONE);
    mStatusView.setVisibility(View.VISIBLE);
    mStatusView.setBackgroundColor(getResources().getColor(R.color.status_view));
    mViewfinderView.setVisibility(View.VISIBLE);

    TextView textView = (TextView) findViewById(R.id.status_text_view);
    textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    textView.setTextSize(14.0f);
    textView.setText(R.string.msg_default_status);
    mLastResult = null;
  }

  public void drawViewfinder() {
    mViewfinderView.drawViewfinder();
  }

  private static class BeepListener implements OnCompletionListener {
    public void onCompletion(MediaPlayer mediaPlayer) {
      mediaPlayer.seekTo(0);
    }
  }
}
