package com.philriesch.android.vflock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.preference.PreferenceActivity;
/**
 * @SVN $Id: EnrollActivity.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class EnrollActivity extends PreferenceActivity {

	public static final int REQUEST_ENROLL_FACE  = 96;
	public static final int REQUEST_ENROLL_VOICE = 98;
    Button btnEnroll;	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_enroll);
        btnEnroll = (Button)findViewById(R.id.btnEnroll);
		btnEnroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                enroll(); 
            }
        });
	}
	
	public void enroll () {
		Log.d("EnrollActivity", "Starting face recognition enrollment");
		Intent enrollfacerecintent = new Intent(this, FacerecActivity.class);
		enrollfacerecintent.putExtra(FacerecActivity.EXTRA_ENROLLMODE, true);
		//startActivityForResult(enrollfacerecintent, REQUEST_ENROLL_FACE);
	}

	@Override
	public void onActivityResult (int request_code, int result_code, Intent intent) {
		super.onActivityResult(request_code, result_code, intent);
        /*
		if (request_code == REQUEST_ENROLL_FACE) {
			if (result_code < 1) {
				Log.d("EnrollActivity", "Finished face recognition enrollment");
				Log.d("EnrollActivity", "Starting voice recognition enrollment");
				Intent enrollvoicerecintent = new Intent(this, VoicerecActivity.class);
				enrollvoicerecintent.putExtra(VoicerecActivity.EXTRA_ENROLLMODE, true);
				startActivityForResult(enrollvoicerecintent, REQUEST_ENROLL_VOICE);
			}
		}
		else if (request_code == REQUEST_ENROLL_VOICE) {
			if (result_code < 1) {
				Log.d("EnrollActivity", "Finished voice recognition enrollment");
				finish();
			}
		}
        */
	}

}
