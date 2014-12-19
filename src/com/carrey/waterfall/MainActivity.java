package com.carrey.waterfall;

import com.carrey.waterfall.waterfall.WaterFall;

import android.os.Bundle;
import android.app.Activity;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		WaterFall waterFall = (WaterFall) findViewById(R.id.waterfall);
		waterFall.setup();
	}

}
