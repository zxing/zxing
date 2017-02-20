package com.kochzap;

/* Prominent notice:
    This file has been modified from the original zxing code for the
    KochZap app.
 */
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
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
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setClassName(this, CaptureActivity.class.getName());
        startActivity(intent);
        return true;
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
            IntentIntegrator scanIntegrator = new IntentIntegrator(this);
            scanIntegrator.initiateScan();
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
                intent.setClassName(this, CaptureActivity.class.getName());
                startActivity(intent);
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
