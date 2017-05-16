package com.barcode;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void onClick (View view){
		IntentIntegrator Integrator = new IntentIntegrator(this);
		Integrator.initiateScan();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent){
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if(scanResult != null){
			String barcode;
			String type;
			
			barcode = scanResult.getContents();
			type = scanResult.getFormatName();
			
			TextView etBarcodeName = (TextView) findViewById(R.id.barcodeName);
			TextView etBarcodeType = (TextView) findViewById(R.id.barcodeType);
			
			etBarcodeName.setText(barcode);
			etBarcodeType.setText(type);
		}
	}

}
