package com.kochzap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // getIntent() is a method from the started activity
        Intent myIntent = getIntent(); // gets the previously created intent
        String company = myIntent.getStringExtra("company");
        String scan = myIntent.getStringExtra("scan");


    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setClassName(this, CaptureActivity.class.getName());
        startActivity(intent);
        return true;
    }

}
