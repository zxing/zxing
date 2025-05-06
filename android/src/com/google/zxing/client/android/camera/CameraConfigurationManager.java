package com.google.zxing.client.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.google.zxing.client.android.PreferencesActivity;
import com.google.zxing.client.android.camera.open.CameraFacing;
import com.google.zxing.client.android.camera.open.OpenCamera;

@SuppressWarnings("deprecation") // Se suprimen advertencias por uso de APIs antiguas
final class CameraConfigurationManager {

  private static final String TAG = "CameraConfiguration";

  private final Context context;
  private int cwNeededRotation;
  private int cwRotationFromDisplayToCamera;
  private Point screenResolution;
  private Point cameraResolution;
  private Point bestPreviewSize;
  private Point previewSizeOnScreen;

  CameraConfigurationManager(Context context) {
    this.context = context;
  }

  void initFromCameraParameters(OpenCamera camera) {
    Camera.Parameters parameters = camera.getCamera().getParameters();
    WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();

    int displayRotation = display.getRotation();
    int cwRotationFromNaturalToDisplay;

    switch (displayRotation) {
      case Surface.ROTATION_0:
        cwRotationFromNaturalToDisplay = 0;
        break;
      case Surface.ROTATION_90:
        cwRotationFromNaturalToDisplay = 90;
        break;
      case Surface.ROTATION_180:
        cwRotationFromNaturalToDisplay = 180;
        break;
      case Surface.ROTATION_270:
        cwRotationFromNaturalToDisplay = 270;
        break;
      default:
        // Corrección para rotaciones no estándar
        if (displayRotation % 90 == 0) {
          cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
        } else {
          throw new IllegalArgumentException("Rotación inválida: " + displayRotation);
        }
    }
    Log.i(TAG, "Display en rotación: " + cwRotationFromNaturalToDisplay);

    int cwRotationFromNaturalToCamera = camera.getOrientation();
    Log.i(TAG, "Orientación de la cámara: " + cwRotationFromNaturalToCamera);

    if (camera.getFacing() == CameraFacing.FRONT) {
      cwRotationFromNaturalToCamera = (360 - cwRotationFromNaturalToCamera) % 360;
      Log.i(TAG, "Cámara frontal ajustada a: " + cwRotationFromNaturalToCamera);
    }

    cwRotationFromDisplayToCamera =
        (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
    Log.i(TAG, "Orientación final: " + cwRotationFromDisplayToCamera);

    if (camera.getFacing() == CameraFacing.FRONT) {
      Log.i(TAG, "Compensando rotación para cámara frontal");
      cwNeededRotation = (360 - cwRotationFromDisplayToCamera) % 360;
    } else {
      cwNeededRotation = cwRotationFromDisplayToCamera;
    }
    Log.i(TAG, "Rotación necesaria: " + cwNeededRotation);

    Point theScreenResolution = new Point();
    display.getSize(theScreenResolution);
    screenResolution = theScreenResolution;
    Log.i(TAG, "Resolución de pantalla: " + screenResolution);

    cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
    Log.i(TAG, "Resolución de cámara: " + cameraResolution);

    bestPreviewSize = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenResolution);
    Log.i(TAG, "Mejor tamaño de previsualización: " + bestPreviewSize);

    boolean isScreenPortrait = screenResolution.x < screenResolution.y;
    boolean isPreviewPortrait = bestPreviewSize.x < bestPreviewSize.y;

    previewSizeOnScreen = isScreenPortrait == isPreviewPortrait
        ? bestPreviewSize
        : new Point(bestPreviewSize.y, bestPreviewSize.x);

    Log.i(TAG, "Tamaño de previsualización en pantalla: " + previewSizeOnScreen);
  }

  void setDesiredCameraParameters(OpenCamera camera, boolean safeMode) {
    Camera theCamera = camera.getCamera();
    Camera.Parameters parameters = theCamera.getParameters();

    if (parameters == null) {
      Log.w(TAG, "No hay parámetros disponibles de cámara.");
      return;
    }

    Log.i(TAG, "Parámetros iniciales de cámara: " + parameters.flatten());

    if (safeMode) {
      Log.w(TAG, "Modo seguro activado, se ignorarán varios ajustes.");
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

    initializeTorch(parameters, prefs, safeMode);

    CameraConfigurationUtils.setFocus(
        parameters,
        prefs.getBoolean(PreferencesActivity.KEY_AUTO_FOCUS, true),
        prefs.getBoolean(PreferencesActivity.KEY_DISABLE_CONTINUOUS_FOCUS, false),
        safeMode);

    if (!safeMode) {
      if (prefs.getBoolean(PreferencesActivity.KEY_INVERT_SCAN, false)) {
        CameraConfigurationUtils.setInvertColor(parameters);
      }

      if (!prefs.getBoolean(PreferencesActivity.KEY_DISABLE_BARCODE_SCENE_MODE, true)) {
        CameraConfigurationUtils.setBarcodeSceneMode(parameters);
      }

      if (!prefs.getBoolean(PreferencesActivity.KEY_DISABLE_METERING, true)) {
        CameraConfigurationUtils.setVideoStabilization(parameters);
        CameraConfigurationUtils.setFocusArea(parameters);
        CameraConfigurationUtils.setMetering(parameters);
      }

      parameters.setRecordingHint(true); // Mejora el rendimiento en algunos dispositivos
    }

    parameters.setPreviewSize(bestPreviewSize.x, bestPreviewSize.y);
    theCamera.setParameters(parameters);
    theCamera.setDisplayOrientation(cwRotationFromDisplayToCamera);

    Camera.Size afterSize = theCamera.getParameters().getPreviewSize();
    if (afterSize != null &&
        (bestPreviewSize.x != afterSize.width || bestPreviewSize.y != afterSize.height)) {
      Log.w(TAG, "Tamaño de previsualización cambiado por el sistema: " +
          afterSize.width + "x" + afterSize.height);
      bestPreviewSize.x = afterSize.width;
      bestPreviewSize.y = afterSize.height;
    }
  }

  Point getBestPreviewSize() {
    return bestPreviewSize;
  }

  Point getPreviewSizeOnScreen() {
    return previewSizeOnScreen;
  }

  Point getCameraResolution() {
    return cameraResolution;
  }

  Point getScreenResolution() {
    return screenResolution;
  }

  int getCWNeededRotation() {
    return cwNeededRotation;
  }

  boolean getTorchState(Camera camera) {
    if (camera != null) {
      Camera.Parameters parameters = camera.getParameters();
      if (parameters != null) {
        String flashMode = parameters.getFlashMode();
        return Camera.Parameters.FLASH_MODE_ON.equals(flashMode)
            || Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode);
      }
    }
    return false;
  }

  void setTorch(Camera camera, boolean newSetting) {
    Camera.Parameters parameters = camera.getParameters();
    doSetTorch(parameters, newSetting, false);
    camera.setParameters(parameters);
  }

  private void initializeTorch(Camera.Parameters parameters, SharedPreferences prefs, boolean safeMode) {
    boolean currentSetting = FrontLightMode.readPref(prefs) == FrontLightMode.ON;
    doSetTorch(parameters, currentSetting, safeMode);
  }

  private void doSetTorch(Camera.Parameters parameters, boolean newSetting, boolean safeMode) {
    CameraConfigurationUtils.setTorch(parameters, newSetting);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    if (!safeMode && !prefs.getBoolean(PreferencesActivity.KEY_DISABLE_EXPOSURE, true)) {
      CameraConfigurationUtils.setBestExposure(parameters, newSetting);
    }
  }
}
