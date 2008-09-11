/*
 * Copyright (C) 2008 Google Inc.
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

package com.android.barcodes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.result.ParsedResult;

import java.io.IOException;

/**
 * The barcode reader activity itself. This is loosely based on the CameraPreview
 * example included in the Android SDK.
 */
public final class BarcodesCaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final int SETTINGS_ID = Menu.FIRST;
    private static final int HELP_ID = Menu.FIRST + 1;
    private static final int ABOUT_ID = Menu.FIRST + 2;

    public BarcodesCaptureActivityHandler mHandler;

    private ViewfinderView mViewfinderView;
    private MediaPlayer mMediaPlayer;
    private String mLastResult;
    private boolean mPlayBeep;
    private boolean mScanIntent;
    private String mDecodeMode;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);

        CameraManager.init(getApplication());
        mViewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        mHandler = null;

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetStatusView();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPlayBeep = prefs.getBoolean(BarcodesPreferenceActivity.KEY_PLAY_BEEP, true);
        initBeepSound();

        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(Intents.Scan.ACTION)) {
            mScanIntent = true;
            mDecodeMode = intent.getStringExtra(Intents.Scan.MODE);
        } else {
            mScanIntent = false;
            mDecodeMode = null;
        }
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
        menu.add(0, SETTINGS_ID, 0, R.string.menu_settings)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, HELP_ID, 0, R.string.menu_help)
                .setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, ABOUT_ID, 0, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SETTINGS_ID: {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName(this, BarcodesPreferenceActivity.class.getName());
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
        CameraManager.get().openDriver(holder);
        if (mHandler == null) {
            mHandler = new BarcodesCaptureActivityHandler(this, mDecodeMode);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult The contents of the barcode.
     * @param duration  How long the decoding took in milliseconds.
     */
    public void handleDecode(Result rawResult, int duration) {
        if (!rawResult.toString().equals(mLastResult)) {
            mLastResult = rawResult.toString();
            playBeepSound();

            ResultPoint[] points = rawResult.getResultPoints();
            if (points != null && points.length > 0) {
                mViewfinderView.drawResultPoints(points);
            }

            TextView textView = (TextView) findViewById(R.id.status_text_view);
            ParsedResult result = ResultHandler.parseResult(rawResult);
            String displayResult = result.getDisplayResult();
            displayResult = displayResult.replace("\r", "");
            textView.setText(displayResult);

            if (!mScanIntent) {
                Button actionButton = (Button) findViewById(R.id.status_action_button);
                int buttonText = ResultHandler.getActionButtonText(result.getType());
                if (buttonText != 0) {
                    actionButton.setVisibility(View.VISIBLE);
                    actionButton.setText(buttonText);
                    ResultHandler resultHandler = new ResultHandler(this, result);
                    actionButton.setOnClickListener(resultHandler);
                    actionButton.requestFocus();
                } else {
                    actionButton.setVisibility(View.GONE);
                }
            }

            View statusView = findViewById(R.id.status_view);
            statusView.setBackgroundColor(getResources().getColor(R.color.result_points));

            // Show the green finder patterns briefly, then either return the result or go back to
            // continuous scanning.
            if (mScanIntent) {
                Intent intent = new Intent(Intents.Scan.ACTION);
                intent.putExtra(Intents.Scan.RESULT,  rawResult.toString());
                intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
                Message message = Message.obtain(mHandler, R.id.return_scan_result);
                message.obj = intent;
                mHandler.sendMessageDelayed(message, 1000);
            } else {
                Message message = Message.obtain(mHandler, R.id.restart_preview);
                mHandler.sendMessageDelayed(message, 2000);
            }
        } else if (mHandler != null) {
            Message message = Message.obtain(mHandler, R.id.restart_preview);
            message.sendToTarget();
        }
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
                mMediaPlayer.setVolume(0.15f, 0.15f);
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

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener mBeepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    private void resetStatusView() {
        resetStatusViewColor();
        TextView textView = (TextView) findViewById(R.id.status_text_view);
        textView.setText(R.string.msg_default_status);
        Button actionButton = (Button) findViewById(R.id.status_action_button);
        actionButton.setVisibility(View.GONE);
        mLastResult = "";
    }

    public void resetStatusViewColor() {
        View statusView = findViewById(R.id.status_view);
        statusView.setBackgroundColor(getResources().getColor(R.color.status_view));
    }

    public void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }

}