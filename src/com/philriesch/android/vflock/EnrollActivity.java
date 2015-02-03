package com.philriesch.android.vflock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.preference.PreferenceActivity;
/**
 * @SVN $Id: EnrollActivity.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class EnrollActivity extends Activity implements OnClickListener {

	public static final int REQUEST_ENROLL_FACE  = 96;
	public static final int REQUEST_ENROLL_VOICE = 98;
	public static final int REQUEST_TEST_FACE = 100;
    Button btnEnroll;	
    Button btnAuth;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_enroll);
		
        btnEnroll = (Button)findViewById(R.id.btnEnroll);
		btnEnroll.setOnClickListener(this);
		
		btnAuth = (Button)findViewById(R.id.btnAuth);
		btnAuth.setOnClickListener(this);
	}
	
	public void enroll () {
		Log.d("EnrollActivity", "Starting face recognition enrollment");
		Intent enrollfacerecintent = new Intent(this, FacerecActivity.class);
		enrollfacerecintent.putExtra(FacerecActivity.EXTRA_ENROLLMODE, true);
		startActivityForResult(enrollfacerecintent, REQUEST_ENROLL_FACE);
	}
	private void authenticate() {
		Intent enrollvoicerecintent = new Intent(this, FacerecActivity.class);
		enrollvoicerecintent.putExtra(FacerecActivity.EXTRA_AUTHENTICATEMODE, true);
		startActivityForResult(enrollvoicerecintent, REQUEST_TEST_FACE);
	}
	@Override
	public void onActivityResult (int request_code, int result_code, Intent intent) {
		super.onActivityResult(request_code, result_code, intent);
        
		if (request_code == REQUEST_ENROLL_FACE) {
			if (result_code < 1) {
				Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
		
		else if (request_code == REQUEST_TEST_FACE) {
			if (result_code < 1) {
				Boolean auth_success = intent.getExtras().getBoolean(FacerecActivity.EXTRA_AUTHENTICATERESULT);
				if(auth_success) {
					Toast.makeText(this, "Authenticated!", Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
				}
				finish();
			}
		}
        
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch(arg0.getId()) {
		case R.id.btnEnroll:
			enroll();
			break;
		case R.id.btnAuth:
			authenticate();
			break;
		default:
				;
		}
		
	}

}
