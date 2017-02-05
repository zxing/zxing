package com.kochzap;

/* Prominent notice:
    This file has been modified from the original zxing code for the
    KochZap app.
 */
import android.content.Intent;
import android.content.res.Configuration;
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
import com.kochzap.history.HistoryActivity;
import com.kochzap.share.Companies;
import com.kochzap.share.ShareActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class StartActivity extends AppCompatActivity implements OnClickListener {

    private ImageView thumb1;
    private ImageView thumb2;

    private static int tUp = R.drawable.button_up;
    private static int tDown = R.drawable.button_down;

    public ImageView scanBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // getIntent() is a method from the started activity
        Intent myIntent = getIntent(); // gets the previously created intent
        String company = myIntent.getStringExtra("company");
        String scan = myIntent.getStringExtra("scan");

        scanBtn = (ImageView) findViewById(R.id.scan_button);

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
        menuInflater.inflate(R.menu.capture, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onClick(View v){
        //respond to clicks

        if(v.getId()== R.id.scan_button){
              //scan
            scanBtn.setImageResource(R.drawable.fist);
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
                            scanBtn.setImageResource(tDown);
                        } else {
                            scanBtn.setImageResource(tUp);
                        }
                    } else {
                        note("No scan data received.");
                    }
                } else {
                    note("No scan data received.");
                }
                break;
        }
    }

    public void note(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(),
                msg, Toast.LENGTH_SHORT);
        toast.show();
    }
}
