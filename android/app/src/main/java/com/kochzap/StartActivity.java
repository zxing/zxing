package com.kochzap;

/* Prominent notice:
    This file has been modified from the original zxing code for the
    KochZap app.
 */
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ImageView;

import com.google.zxing.Result;
import com.kochzap.history.HistoryActivity;
import com.kochzap.util.Companies;
import com.kochzap.share.ShareActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class StartActivity extends AppCompatActivity implements OnClickListener {

    public ImageView scanBtn;

    private boolean cameraPermission = false;
    final private static int MY_PERMISSIONS_REQUEST_CAMERA = 23;
    private boolean zapPending = false;
    private boolean scanPending = false;

    private Companies cos = new Companies();

    private enum result {
        NONE, FAIL, PASS
    }

    private static result lastResult = result.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        scanBtn = (ImageView) findViewById(R.id.scan_button);
        setKochzapBar();
        scanBtn.setOnClickListener(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back.
            this.finish();
            return true;
        }


        if (cameraPermission) {
                zapPending = false;
                scanPending = false;

                IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                scanIntegrator.initiateScan();
                return true;
        } else {
            zapPending = true;
            scanPending = false;
        }
        // let the system handle it?
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.start, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onClick(View v) {
        //respond to clicks

        if (v.getId() == R.id.scan_button) {
            cameraPermission = checkPermission();
            if (cameraPermission) {
                scanPending = false;
                zapPending = false;
                IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                scanIntegrator.initiateScan();
            } else {
                zapPending = true;
                scanPending = false;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        switch (item.getItemId()) {
            case R.id.menu_start:
                break;
            case R.id.menu_capture:
                cameraPermission = checkPermission();
                if (cameraPermission) {
                    intent.setClassName(this, CaptureActivity.class.getName());
                    startActivity(intent);
                } else {
                    scanPending = true;
                    zapPending = false;
                }
                break;
            case R.id.menu_share:
                intent.setClassName(this, ShareActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.menu_history:
                intent.setClassName(this, HistoryActivity.class.getName());
                startActivityForResult(intent, Constants.HISTORY_REQUEST_CODE);
                break;
            case R.id.menu_settings:
                intent.setClassName(this, PreferencesActivity.class.getName());
                startActivity(intent);
                break;
            case R.id.menu_help:
                intent.setClassName(this, HelpActivity.class.getName());
                startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    private boolean checkPermission() {
        if (cameraPermission) {
            // already got it? No need to repeat.
            return true;
        }

        // not sure if we have permission? CTHen check.
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // From 6.0 (Marshmallow, version 23) on, need to ask permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // No permission yet, either exlain and ask or just ask

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    String needCamera = getString(R.string.need_camera_to_scan);
                    Toast.makeText(this, needCamera, Toast.LENGTH_LONG).show();
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            MY_PERMISSIONS_REQUEST_CAMERA);
                    // MY_PERMISSIONS_REQUEST_CAMERA is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                    return false;

                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            MY_PERMISSIONS_REQUEST_CAMERA);
                    return false;
                }
            } else {
                // Android OS says permission has been granted, we are done here
                return true;
            }
        } else {
            // Android OS before Marshmallow was approved at install, no check needed.
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraPermission = true;

                    if (zapPending) {
                        zapPending = false;
                        scanPending = false;
                        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                        scanIntegrator.initiateScan();
                    } else {
                        if (scanPending) {
                            zapPending = false;
                            scanPending = false;
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            intent.setClassName(this, CaptureActivity.class.getName());
                            startActivity(intent);
                        }
                    }
                }
                return;
            }
        }
    }



    public void onActivityResult(int requestCode, int resultCode, Intent intent) {


        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
/*
            case Constants.REQUEST_INVITE:
                //if (resultCode == RESULT_OK) {
                // Check how many invitations were sent and log a message
                // The ids array contains the unique invitation ids for each invitation sent
                // (one for each contact select by the user). You can use these for analytics
                // as the ID will be consistent on the sending and receiving devices.
                //String[] ids = AppInviteInvitation.getInvitationIds(resultCode, intent);
                //}
                break;

            case RESULT_CANCELED:
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, R.string.pick_account, Toast.LENGTH_SHORT).show();
                break;
*/
            default:
                //retrieve scan result
                IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

                if (scanningResult != null) {
                    //we have a result

                    String company = scanningResult.getContents();
                    if (company != null && company.length() > 5) {
                        company = company.substring(0, 6);

                        if (Companies.containscompany(company)) {
                            lastResult = result.FAIL;
                        } else {
                            lastResult = result.PASS;
                        }
                    } else {
                        note("No scan data received.");
                        lastResult = result.NONE;
                    }
                } else {
                    lastResult = result.NONE;
                    note("No scan data received.");
                }
                setKochzapBar();
                break;
        }
    }

    private void setKochzapBar() {
        switch (lastResult) {
            case FAIL:
                fixTitle(Color.RED, getString(R.string.zap));
                setScanBtn(R.drawable.button_down, R.drawable.button_down_land);
                break;
            case PASS:
                fixTitle(Color.GREEN, getString(R.string.good));
                setScanBtn(R.drawable.button_up, R.drawable.button_up_land);
                break;
            case NONE:
            default:
                fixTitle(Color.BLACK, getString(R.string.app_name));
                setScanBtn(R.drawable.button_fist, R.drawable.button_fist_land);
                break;
        }
    }

    void setScanBtn(int port, int land) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            scanBtn.setImageResource(land);
        } else {
            scanBtn.setImageResource(port);
        }
    }

    private void fixTitle(int color, String titleString) {
        setTitleColor(color);
        setTitle(titleString);
    }



    public void note(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(),
                msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}
