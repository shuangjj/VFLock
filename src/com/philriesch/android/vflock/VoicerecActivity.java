package com.philriesch.android.vflock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import marf.Storage.MainContext;
import marf.util.MARFException;

import com.philriesch.android.vflock.threads.ThreadBadStateException;
import com.philriesch.android.vflock.threads.VoicerecThread;
import com.philriesch.android.vflock.threads.VoicerecThreadEventInterface;
import com.philriesch.android.voicerec.recorder.RecorderEventInterface;
import com.philriesch.android.voicerec.recorder.WaveRecorder;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.Toast;
import android.os.Build;

/**
 * @SVN $Id: VoicerecActivity.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class VoicerecActivity extends VflockActivity
                              implements VoicerecThreadEventInterface,
                                         RecorderEventInterface {

	public static final String EXTRA_ENROLLMODE = 
	"com.philriesch.android.vflock.VoicerecActivity.EXTRA_ENROLLMODE";
	
	public static final String EXTRA_AUTHENTICATEMODE = 
	"com.philriesch.android.vflock.VoicerecActivity.EXTRA_AUTHENTICATEMODE";
	
	public static final String EXTRA_ENROLLRESULT =
	"com.philriesch.android.vflock.VoicerecActivity.EXTRA_ENROLLRESULT";
	
	public static final String EXTRA_AUTHENTICATERESULT =
	"com.philriesch.android.vflock.VoicerecActivity.EXTRA_AUTHENTICATERESULT";
	
	public static final String EXTRA_AUTHENTICATEPROB =
	"com.philriesch.android.vflock.VoicerecActivity.EXTRA_AUTHENTICATEPROB";
	
	private String current_mode;
	
	private VoicerecThread voice_recognition_thread;
	
	private VoicerecActivity scoperesolve;
	
	private File tmp_wavefile;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		scoperesolve = this;
		
		Intent intent = getIntent();
		if (intent.getBooleanExtra(EXTRA_ENROLLMODE, false)) {
			Log.d("VoicerecActivity", "Entering enroll mode.");
			current_mode = EXTRA_ENROLLMODE;
		}
		else {
			Log.d("VoicerecActivity", "Entering authenticate mode.");
			current_mode = EXTRA_AUTHENTICATEMODE;
			makeFullscreen();
		}
		
		try {
			MainContext marfcontext = new MainContext();
			marfcontext.setContext(getApplicationContext());
			voice_recognition_thread = new VoicerecThread(getAssets(),
					marfcontext.getContext().getFilesDir().getAbsolutePath(),
					marfcontext.getContext().getCacheDir().getAbsolutePath(),
					this);
			voice_recognition_thread.Start();
		}
		catch (Exception e) {
			Log.e("VoicerecActivity", "EXCEPTION ON VOICEREC THREAD START: " + e.getMessage());
		}
		
		setContentView(R.layout.activity_voicerec);
	}
	
	@Override
	protected void onDestroy () {
		super.onDestroy();
		
		voice_recognition_thread.Stop();
	}
	
	@Override
	protected void onResume () {
		super.onResume();
		
		if (current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			VflockApplication.SecurityAuthStateOn();
		}
	}
	
	@Override
	protected void onPause () {
		super.onPause();
		
		if (current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			VflockApplication.SecurityAuthStateOff();
		}
	}
	
	@Override
	public void onBackPressed () {
		return;
	}
	
	private void MakeNewRecording () {
		final WaveRecorder recorder = new WaveRecorder(getFilesDir().getAbsolutePath(), 
				getCacheDir().getAbsolutePath());
		tmp_wavefile = null;
		try {
			Log.d("VoicerecActivity", "Starting new wave recording");
			recorder.Start(getCacheDir().getAbsolutePath(), "user.wav");
			
			final ProgressDialog progress = new ProgressDialog(this);
			progress.setTitle("Loading");
			progress.setMessage("Loading...");
			progress.show();
			
			final Chronometer chrono = new Chronometer(this);
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Recording your voice...");
			alert.setView(chrono);
			chrono.setPadding(15, 5, 5, 5);
			final AlertDialog a = alert.create();
			
			// 1 second delay to fix a bug in Andy's code...
			final Handler delayone = new Handler();
			delayone.postDelayed(new Runnable () {
				
				@Override
				public void run () {
					progress.dismiss();
					a.show();
					chrono.start();
				}
				
			}, 1000);
			
			// Create a 5 second recording
			final Handler delaytwo = new Handler();
			delaytwo.postDelayed(new Runnable () {
				
				@Override
				public void run () {
					chrono.stop();
					a.cancel();
					try {
						Log.d("VoicerecActivity", "Finished new wave recording");
						tmp_wavefile = recorder.Stop();
						scoperesolve.onRecordingComplete();
					} 
					catch (FileNotFoundException e) {
						Log.e("VoicerecActivity", "File Not Found Exception encountered: " + e.getMessage());
						throw new RuntimeException("VoicerecActivity: File Not Found Exception encountered: " + e.getMessage());
					} 
					catch (IOException e) {
						Log.e("VoicerecActivity", "IO Exception encountered: " + e.getMessage());
						throw new RuntimeException("VoicerecActivity: IO Exception encountered: " + e.getMessage());
					}
				}
				
			}, 5000);
		} 
		catch (IllegalStateException e) {
			Log.e("VoicerecActivity", "Illegal State Exception encountered: " + e.getMessage());
			throw new RuntimeException("VoicerecActivity: Illegal State Exception encountered: " + e.getMessage());
		} 
		catch (IOException e) {
			Log.e("VoicerecActivity", "IO Exception encountered: " + e.getMessage());
			throw new RuntimeException("VoicerecActivity: IO Exception encountered: " + e.getMessage());
		}
	}
	
	public void onRecord (View view) {
		if (current_mode.equals(EXTRA_ENROLLMODE)) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage("Voice recognition enrollment takes about a minute to complete. Speak clearly into device microphone for five seconds. Please do not click back or close the application during enrollment. Enroll application will close when training is complete. Press Ok to start.");
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener () {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					MakeNewRecording();
				}

			});
			alert.show();
		}
		else {
			MakeNewRecording();
		}
	}
	
	private void onVoicerecEnrollRecord () {
		try {
			voice_recognition_thread.CreateNewModel();
			// Wait for the thread to become ready
			while (!voice_recognition_thread.CanTrain()) continue;
			// Feed the recording to the thread for training
			voice_recognition_thread.Train(tmp_wavefile);
		}
		catch (ThreadBadStateException e) {
			Log.e("VoicerecActivity", "Thread bad state exception encountered during enrollment!");
		}
	}
	
	private void onVoicerecAuthRecord () {
		try {
			voice_recognition_thread.Test(tmp_wavefile);
		}
		catch (ThreadBadStateException e) {
			Log.e("VoicerecActivity", "Thread bad state exception encountered during authentication!");
		}
	}
	
	/*************************************************************************/
	
	// BELOW IS THE RECORDER EVENT CALLBACK, WHICH IS CALLED ONCE A RECORDING
	// HAS BEEN SUCCESSFULLY MADE
	
	public void onRecordingComplete () {
		if (current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			onVoicerecAuthRecord();
		}
		else {
			onVoicerecEnrollRecord();
		}
	}
	
	/*************************************************************************/
	
	// BELOW ARE ALL OF THE FACE RECOGNITION THREAD EVENT CALLBACKS
	// THESE USUALLY HAPPEN ON ANOTHER THREAD THAT IS ->NOT<- THE UI THREAD,
	// SO MAKE SURE UI ACTIONS ARE GETTING SENT TO THE UI THREAD!
	
	@Override
	public void onThreadStart() {
		Log.d("VoicerecActivity", "Voicerec thread started successfully");
	}

	@Override
	public void onThreadStop() {
		Log.d("VoicerecThread", "Voicerec thread stopped successfully");
	}

	@Override
	public void onModelCreateStart(String username) {}

	@Override
	public void onModelCreateComplete(String username) {
		Log.d("VoicerecActivity", "Voicerec model create was successful");
	}

	@Override
	public void onModelOpenStart(String username) {}

	@Override
	public void onModelOpenComplete(String username) {
		Log.d("VoicerecActivity", "Voicerec model was opened successfully");
	}

	@Override
	public void onModelSaveStart(String username) {}

	@Override
	public void onModelSaveComplete(String username) {}

	@Override
	public void onTrainStart() {
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Training voice recognition...Please wait.", Toast.LENGTH_LONG).show();
			}
			
		});
	}

	@Override
	public void onTrainComplete() {
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Voice recognition training is complete.", Toast.LENGTH_LONG).show();
				Intent returnintent = new Intent(scoperesolve, EnrollActivity.class);
				returnintent.putExtra(EXTRA_ENROLLRESULT, true);
				if (getParent() == null) {
					setResult(Activity.RESULT_OK, returnintent);
				}
				else {
					getParent().setResult(Activity.RESULT_OK, returnintent);
				}
				voice_recognition_thread.Stop();
				finish();
			}
			
		});
	}

	@Override
	public void onTestStart() {
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Authenticating voice...", Toast.LENGTH_LONG).show();
			}
			
		});
	}

	@Override
	public void onTestComplete(final boolean result, final double prob) {
		Log.d("VoicerecActivity", "Voicerec test complete");
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Intent returnintent = new Intent(scoperesolve, LockscreenActivity.class);
				if (result) {
					Log.d("VoicerecActivity", "Voice recognition probability: "+prob+" (PASS)");
					Toast.makeText(getApplicationContext(), "Voice recognized.", Toast.LENGTH_LONG).show();
					returnintent.putExtra(EXTRA_AUTHENTICATERESULT, true);
					returnintent.putExtra(EXTRA_AUTHENTICATEPROB, prob);
				}
				else {
					Log.d("VoicerecActivity", "Voice recognition probability: "+prob+" (FAIL)");
					Toast.makeText(getApplicationContext(), "Voice not recognized.", Toast.LENGTH_LONG).show();
					returnintent.putExtra(EXTRA_AUTHENTICATERESULT, false);
					returnintent.putExtra(EXTRA_AUTHENTICATEPROB, prob);
				}
				if (getParent() == null) {
					setResult(Activity.RESULT_OK, returnintent);
				}
				else {
					getParent().setResult(Activity.RESULT_OK, returnintent);
				}
				voice_recognition_thread.Stop();
				finish();
			}
			
		});
	}

	@Override
	public void onMARFException(MARFException e) {
		Log.e("VoicerecActivity", "MARF Exception encountered: " + e.getMessage());
		throw new RuntimeException("VoicerecActivity: MARF Exception encountered: " + e.getMessage());
	}

	@Override
	public void onIOException(IOException e) {
		Log.e("VoicerecActivity", "IO Exception encountered: " + e.getMessage());
		throw new RuntimeException("VoicerecActivity: IO Exception encountered: " + e.getMessage());
	}

	@Override
	public void onRuntimeException(Exception e) {
		Log.e("VoicerecActivity", "Runtime exception: " + e.getMessage());
		throw (RuntimeException)e;
	}

}
