package com.philriesch.android.vflock;

import java.io.IOException;
import java.util.ArrayList;

import org.opencv.android.OpenCVLoader;

import com.philriesch.android.facerec.common.data.RectangleRoiData;
import com.philriesch.android.facerec.common.data.StandardImageData;
import com.philriesch.android.facerec.haar.exception.FaceNotFoundException;
import com.philriesch.android.vflock.threads.FacerecThread;
import com.philriesch.android.vflock.threads.FacerecThreadEventInterface;
import com.philriesch.android.vflock.threads.ThreadBadStateException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * @SVN $Id: FacerecActivity.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class FacerecActivity extends AbstractCameraActivity 
                             implements FacerecThreadEventInterface {

	public static final String LOG_TAG = "FacerecActivity";
	public static final String EXTRA_ENROLLMODE = 
	"com.philriesch.android.vflock.FacerecActivity.EXTRA_ENROLLMODE";
	
	public static final String EXTRA_AUTHENTICATEMODE = 
	"com.philriesch.android.vflock.FacerecActivity.EXTRA_AUTHENTICATEMODE";
	
	public static final String EXTRA_ENROLLRESULT = 
	"com.philriesch.android.vflock.FacerecActivity.EXTRA_ENROLLRESULT";
	
	public static final String EXTRA_AUTHENTICATERESULT =
	"com.philriesch.android.vflock.FacerecActivity.EXTRA_AUTHENTICATERESULT";
	
	public static final String EXTRA_AUTHENTICATEDF =
	"com.philriesch.android.vflock.FacerecActivity.EXTRA_AUTHENTICATEDF";
	
	private String current_mode;
	
	private FacerecThread face_recognition_thread;
	
	private static final int ENROLL_PICTURE_TOTAL = 4;
	private ArrayList<Bitmap> enroll_picture_set;
	private int enroll_picture_count;
	private String alert_msg;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.scoperesolve = (AbstractCameraActivity)this;
		
		Intent intent = getIntent();
		if (intent.getBooleanExtra(EXTRA_ENROLLMODE, false)) {
			Log.d("FacerecActivity", "Entering enroll mode.");
			current_mode = EXTRA_ENROLLMODE;
			enroll_picture_set = new ArrayList<Bitmap>();
			enroll_picture_count = 0;
		}
		else {
			Log.d("FacerecActivity", "Entering authenticate mode.");
			current_mode = EXTRA_AUTHENTICATEMODE;
			//makeFullscreen();
		}
		
		try {
			// Start the face rec thread...
			face_recognition_thread = new FacerecThread(getAssets(), 
			                                            getFilesDir().getAbsolutePath(), 
			                                            getCacheDir().getAbsolutePath(), 
			                                            this);
			face_recognition_thread.Start();
		}
		catch (Exception e) {
			Log.e("FacerecActivity", "EXCEPTION ON FACEREC THREAD START: " + e.getMessage());
		}
		
		setContentView(R.layout.activity_facerec);
	}
	
	@Override
	protected void onDestroy () {
		super.onDestroy();
		
		face_recognition_thread.Stop();
	}
	
	@Override
	protected void onResume () {
		super.onResume();
		/*
		if (current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			VflockApplication.SecurityAuthStateOn();
		}
		*/
		CameraResume();
	}
	
	@Override
	protected void onPause () {
		super.onPause();
		
		if (current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			VflockApplication.SecurityAuthStateOff();
		}
		
		
	}
	
	/*@Override
	public void onBackPressed () {
		return;
	}*/

	@Override
	protected void CameraResume() {
		if(this.camera != null) this.camera.release();
		
		this.camera = getFrontCamera();
		this.camera.setDisplayOrientation(90);
		
		
		FrameLayout preview_frame = (FrameLayout) findViewById(R.id.FacerecCameraOutput);
		if(this.preview != null) preview_frame.removeView(this.preview);
		this.preview = new CameraPreview(this, this.camera);
		preview_frame.addView(this.preview);
		
		this.camera.startPreview();
	}

	@Override
	public void onCapture(View view) {
		if (current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			onFacerecAuthCapture();
		}
		else {
			onFacerecEnrollCapture();
		}
	}
	
	private void onFacerecEnrollCapture () {
		this.scoperesolve = this;
		this.camera.takePicture(null, null, new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] imagedata, Camera camera) {
				enroll_picture_set.add(BitmapFactory.decodeByteArray(imagedata, 0, imagedata.length));
				enroll_picture_count++;
				
				if (enroll_picture_count < ENROLL_PICTURE_TOTAL) {
					if (enroll_picture_count % 2 > 0) {
						Toast.makeText(getApplicationContext(), "Training image captured. Turn to the right and take another.", Toast.LENGTH_LONG).show();
					}
					else {
						Toast.makeText(getApplicationContext(), "Training image captured. Turn to the left and take another.", Toast.LENGTH_LONG).show();
					}
					camera.startPreview();
				}
				else {
					AlertDialog.Builder alert = new AlertDialog.Builder((FacerecActivity)scoperesolve);
					alert.setMessage("Face recognition enrollment takes several minutes to complete. Please do not click back or close the application during enrollment. The application will continue to voice enrollment when face enrollment is complete. Press Ok to start.");
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ArrayList<Bitmap> training_bitmaps = new ArrayList<Bitmap>();
							Matrix mat = new Matrix();
							Bitmap bmp = null;
							mat.postRotate(270f);
							for (int i = 0; i < ENROLL_PICTURE_TOTAL; i++) {
								bmp = enroll_picture_set.get(i);
								training_bitmaps.add(Bitmap.createBitmap(bmp,
										0,
										0,
										bmp.getWidth(),
										bmp.getHeight(),
										mat,
										true));
							}
							try {
								if(!face_recognition_thread.isRunning()) face_recognition_thread.Start();
								face_recognition_thread.CreateNewModel("user");
								// Wait for the thread to become ready
								while (!face_recognition_thread.CanTrain()) continue;
								// Feed the image to the thread
								face_recognition_thread.Train(training_bitmaps);
								// Wait for the thread to become ready
								while (!face_recognition_thread.CanSaveModel() && 
										face_recognition_thread.isRunning()) continue;
								if(face_recognition_thread.isRunning()) {
									// Save the model
									face_recognition_thread.SaveModel();
								} else {
									ExceptionAlertAndReset(alert_msg);
								}
							}
							catch (ThreadBadStateException e) {
								Log.e("FacerecActivity", "Thread bad state exception encountered during enrollment!");
							}
						}
						
					});
					alert.show();
					
				}
			}
			
		});
	}
	
	private void onFacerecAuthCapture () {
		this.scoperesolve = this;
		this.camera.takePicture(null, null, new PictureCallback () {

			@Override
			public void onPictureTaken(byte[] imagedata, Camera camera) {
				Bitmap bmp = BitmapFactory.decodeByteArray(imagedata, 
				                                           0, 
				                                           imagedata.length);
				// Rotate the image...
				Matrix mat = new Matrix();
				mat.postRotate(270f);
				// Tell the facerec thread to load the user model
				try {
					if(!face_recognition_thread.isRunning()) face_recognition_thread.Start();
					face_recognition_thread.OpenModel("user");
					// Wait for the thread to become ready
					while (!face_recognition_thread.CanTest()) continue;
					// Feed the image to the thread
					face_recognition_thread.Test(Bitmap.createBitmap(bmp, 
							0, 
							0, 
							bmp.getWidth(), 
							bmp.getHeight(), 
							mat, 
							true));
					
				}
				catch (ThreadBadStateException e) {
					Log.e("FacerecActivity", "Thread bad state exception encountered during authentication!");
				}
			}
			
		});
	}
	
	/*************************************************************************/
	
	// BELOW ARE ALL OF THE FACE RECOGNITION THREAD EVENT CALLBACKS
	// THESE USUALLY HAPPEN ON ANOTHER THREAD THAT IS ->NOT<- THE UI THREAD,
	// SO MAKE SURE UI ACTIONS ARE GETTING SENT TO THE UI THREAD!

	@Override
	public void onThreadStart() {
		Log.d("FacerecActivity", "Facerec thread started successfully");
	}

	@Override
	public void onThreadStop() {
		Log.d("FacerecActivity", "Facerec thread stopped successfully");
	}

	@Override
	public void onModelCreateStart(String username) {}

	@Override
	public void onModelCreateComplete(String username) {
		Log.d("FacerecActivity", "Facerec model create was successful for \""+username+"\"");
	}

	@Override
	public void onModelOpenStart(String username) {}

	@Override
	public void onModelOpenComplete(String username) {
		Log.d("FacerecActivity", "Facerec model open was successful for \""+username+"\"");
	}

	@Override
	public void onModelSaveStart(String username) {}

	@Override
	public void onModelSaveComplete(String username) {
		Log.d("FacerecActivity", "Facerec model save was successful for \""+username+"\"");
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				// Successful enrollment. Return back to the enroll activity.
				Toast.makeText(getApplicationContext(), "Settings saved.", Toast.LENGTH_LONG).show();
				Intent returnintent = new Intent();
				returnintent.putExtra(EXTRA_ENROLLRESULT, "hello, enrolled");
				if (getParent() == null) {
					setResult(Activity.RESULT_OK, returnintent);
				}
				else {
					getParent().setResult(Activity.RESULT_OK, returnintent);
				}
				face_recognition_thread.Stop();
				finish();
			}
			
		});
	}

	@Override
	public void onFaceFind(StandardImageData image, RectangleRoiData facerect) {}

	@Override
	public void onTrainStart() {
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Training face recognition...Please wait, this takes some time.", Toast.LENGTH_LONG).show();
			}
			
		});
	}

	@Override
	public void onTrainComplete() {
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Face recognition training is complete.", Toast.LENGTH_LONG).show();
			}
			
		});
	}

	@Override
	public void onTestStart() {
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), "Authenticating face...", Toast.LENGTH_LONG).show();
			}
			
		});
	}

	@Override
	public void onTestComplete(final boolean result, final double df) {
		Log.d("FacerecActivity", "Facerec test complete");
		runOnUiThread(new Runnable () {

			@Override
			public void run() {
				Intent returnintent = new Intent((FacerecActivity)scoperesolve, LockscreenActivity.class);
				if (result) {
					Log.d("FacerecActivity", "Face recognition DF: "+df+" (PASS)");
					Toast.makeText(getApplicationContext(), "Face recognized.", Toast.LENGTH_LONG).show();
					returnintent.putExtra(EXTRA_AUTHENTICATERESULT, true);
					returnintent.putExtra(EXTRA_AUTHENTICATEDF, df);
				}
				else {
					Log.d("FacerecActivity", "Face recognition DF: "+df+" (FAIL)");
					Toast.makeText(getApplicationContext(), "Face not recognized.", Toast.LENGTH_LONG).show();
					returnintent.putExtra(EXTRA_AUTHENTICATERESULT, false);
					returnintent.putExtra(EXTRA_AUTHENTICATEDF, df);
				}
				if (getParent() == null) {
					setResult(Activity.RESULT_OK, returnintent);
				}
				else {
					getParent().setResult(Activity.RESULT_OK, returnintent);
				}
				face_recognition_thread.Stop();
				finish();
			}
			
		});
	}

	@Override
	public void onFaceNotFoundException(FaceNotFoundException e) {
		Log.e("FacerecActivity", "Face not found exception");
		face_recognition_thread.Stop();
		if(current_mode.equals(EXTRA_AUTHENTICATEMODE)) {
			alert_msg = "An error occured while authenticating: cannot find your face. "
					+ "Please hold device one arm length away from your face and try again. "
					+ "Press Ok to try again.";
			ExceptionAlertAndReset(alert_msg);
			Log.d(LOG_TAG, alert_msg);
		} else {
			alert_msg = "An error occured while enrolling: cannot find your face."
					+ "Please hold device one arm length away from your face and try again. "
					+ "Press Ok to try again.";
			
		}
	}

	@Override
	public void onIOException(IOException e) {
		Log.e("FacerecActivity", "IO Exception encountered: " + e.getMessage());
		alert_msg = "An unexpected error has occured. Press Ok to try again.";
	}

	@Override
	public void onRuntimeException(Exception e) {
		Log.e("FacerecActivity", "Runtime exception: " + e.getMessage());
		if (e.getMessage().contains("(-5) The SVM should be trained first in function CvSVM::predict")) {
			ExceptionAlertSystemUntrained();
		}
		else {
			alert_msg = "An unexpected error has occurred. Press Ok to try again.";
			//ExceptionAlertAndReset("An unexpected error has occurred. Press Ok to try again.");
		}
	}
	
	private void ExceptionAlertAndReset (final String message) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder((FacerecActivity)scoperesolve);
				alert.setMessage(message);
				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (current_mode.equals(EXTRA_ENROLLMODE)) {
							enroll_picture_set.clear();
							enroll_picture_count = 0;
							Log.d(LOG_TAG, "Picture cleared, restarted");
							Log.d(LOG_TAG, String.format("count: %d, size: %d", enroll_picture_count, 
									enroll_picture_set.size()));
						}
						
						CameraResume();
					}
					
				});
				alert.show();
			}
			
		});
	}
	
	private void ExceptionAlertSystemUntrained () {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder((FacerecActivity)scoperesolve);
				alert.setMessage("There are no users enrolled on this device. You must enroll before using this device. Press Ok to begin enrollment.");
				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// I know...I know, I'm unlocking the phone without authenticating. That sounds
						// super insecure. But if we are executing this code, then there was no user
						// enrolled on this device. So was it ever really secure in the first place?
						getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
						VflockApplication.SecurityLockStateOff();
						LockscreenService.DisableKeyguard();
						// Start the enrollment code
						Intent enrollintent = new Intent((FacerecActivity)scoperesolve, EnrollActivity.class);
						enrollintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						((FacerecActivity)scoperesolve).startActivity(enrollintent);
					}
					
				});
				alert.show();
			}
			
		});
	}

}
