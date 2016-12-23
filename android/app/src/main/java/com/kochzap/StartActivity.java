package com.kochzap;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import com.kochzap.history.HistoryActivity;
import com.kochzap.share.ShareActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // getIntent() is a method from the started activity
        Intent myIntent = getIntent(); // gets the previously created intent
        String company = myIntent.getStringExtra("company");
        String scan = myIntent.getStringExtra("scan");

        showAd();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        switch (item.getItemId()) {
            case R.id.menu_start:
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

    private class AdTrack {

        public Activity ctx;
        public AdView adView, adView1, adView2;
        public AdRequest adRequest, adRequest1, adRequest2;

        AdTrack(Activity context, AdView adVw, AdRequest adReq) {
            this.ctx = context;
            this.adView = adVw;
            this.adRequest = adReq;
        }
    }

    private AdTrack adTrack, adTrack1, adTrack2;

    private void showAd() {
        if (adTrack == null || adTrack.ctx != this) {
            // need a new ad
            AdView mAdView = (AdView) findViewById(R.id.BoycottAdView);
            AdRequest adRequest = new AdRequest.Builder()
                    .build();
            mAdView.loadAd(adRequest);
            // OK, load this ad for this context
            adTrack = new AdTrack(this, mAdView, adRequest);
        } else {
            // already fetched the ad for this context, just load it again
            adTrack.adView.loadAd(adTrack.adRequest);
        }
    }
}
