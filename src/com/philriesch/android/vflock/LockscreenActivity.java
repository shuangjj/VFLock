package com.philriesch.android.vflock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * @SVN $Id: LockscreenActivity.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LockscreenActivity extends VflockActivity {
	
	public static final int REQUEST_AUTH_FACE  = 97;
	
	public static final int REQUEST_AUTH_VOICE = 99;
	
	@Override
	public void onAttachedToWindow () {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		makeFullscreen();
		startService(new Intent(this, LockscreenService.class));
		
		setContentView(R.layout.activity_lockscreen);
	}
	
	@Override
	protected void onResume () {
		super.onResume();
		
		Log.d("LockscreenActivity", "On Resume");
		
		VflockApplication.SecurityAuthStateOn();
	}
	
	@Override
	protected void onPause () {
		super.onPause();
		
		VflockApplication.SecurityAuthStateOff();
	}
	
	@Override
	public void onBackPressed () {
		return;
	}
	
	public void onUnlockScreen (View view) {
		Log.d("LockscreenActivity", "Starting face recognition authentication");
		Intent authfacerecintent = new Intent(this, FacerecActivity.class);
		authfacerecintent.putExtra(FacerecActivity.EXTRA_AUTHENTICATEMODE, true);
		startActivityForResult(authfacerecintent, REQUEST_AUTH_FACE);
	}
	
	@Override
	public void onActivityResult (int request_code, int result_code, Intent intent) {
		super.onActivityResult(request_code, result_code, intent);
		if (request_code == REQUEST_AUTH_FACE) {
			if (result_code < 1) {
				Log.d("LockscreenActivity", "Finished face recognition authentication");
				boolean facerec_result = intent.getBooleanExtra(FacerecActivity.EXTRA_AUTHENTICATERESULT, false);
				if (facerec_result) {
					Log.d("LockscreenActivity", "Starting voice recognition authentication");
					Intent authvoicerecintent = new Intent(this, VoicerecActivity.class);
					authvoicerecintent.putExtra(VoicerecActivity.EXTRA_AUTHENTICATEMODE, true);
					startActivityForResult(authvoicerecintent, REQUEST_AUTH_VOICE);
				}
				else {
					Toast.makeText(getApplicationContext(), "Access Denied.", Toast.LENGTH_LONG).show();
				}
			}
		}
		else if (request_code == REQUEST_AUTH_VOICE) {
			if (result_code < 1) {
				Log.d("LockscreenActivity", "Finished voice recognition enrollment");
				boolean voicerec_result = intent.getBooleanExtra(VoicerecActivity.EXTRA_AUTHENTICATERESULT, false);
				if (voicerec_result) {
					Toast.makeText(getApplicationContext(), "Access Granted.", Toast.LENGTH_LONG).show();
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
					VflockApplication.SecurityLockStateOff();
					LockscreenService.DisableKeyguard();
					finish();
				}
				else {
					Toast.makeText(getApplicationContext(), "Access Denied.", Toast.LENGTH_LONG).show();
				}
			}
		}
	}

}
