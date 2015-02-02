package com.philriesch.android.vflock;

import java.io.IOException;
import java.util.ArrayList;

import com.philriesch.android.facerec.common.data.RectangleRoiData;
import com.philriesch.android.facerec.common.data.StandardImageData;
import com.philriesch.android.facerec.haar.exception.FaceNotFoundException;
import com.philriesch.android.vflock.threads.FacerecThread;
import com.philriesch.android.vflock.threads.FacerecThreadEventInterface;
import com.philriesch.android.vflock.threads.ThreadBadStateException;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class FaceAuthService extends Service implements FacerecThreadEventInterface {
	private FacerecThread face_recognition_thread;

	
	public static final int MSG_START_FACE_AUTH = 1;
	public static final int MSG_REGISTER_CLIENT = 2;
	public static final int MSG_UNREGISTER_CLIENT = 3;
	
	public static final int MSG_AUTH_SUCCESS = 10;
	public static final int MSG_AUTH_FAIL = 11;

	private static final String LOG_TAG = "faceAuthService";
	
	private Messenger mclient = null;
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_FACE_AUTH:
                    Log.d(LOG_TAG, "Authenticating...");
                    authenticateFace((Bitmap) msg.obj);
                    break;
                case MSG_REGISTER_CLIENT:
                	mclient = msg.replyTo;
                	break;
                case MSG_UNREGISTER_CLIENT:
                	mclient = null;
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    private void authenticateFace(Bitmap bmp) {
    	// Rotate the image...
		Matrix mat = new Matrix();
		mat.postRotate(270f);
		// Tell the facerec thread to load the user model
		try {
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
			face_recognition_thread.Stop();
			while(face_recognition_thread.isRunning()) ;
			face_recognition_thread.Start();
			// Authenticate again
			authenticateFace(bmp);
		}
    }
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
	@Override
	public IBinder onBind(Intent intent) {
		Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
		
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
        return mMessenger.getBinder();
        
	
	}
	

	@Override
	public void onThreadStart() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onThreadStop() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onModelCreateStart(String username) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onModelCreateComplete(String username) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onModelOpenStart(String username) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onModelOpenComplete(String username) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onModelSaveStart(String username) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onModelSaveComplete(String username) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onFaceFind(StandardImageData image, RectangleRoiData facerect) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onTrainStart() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onTrainComplete() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onTestStart() {
		
		
	}
	@Override
	public void onTestComplete(boolean result, double df) {
		sendResult(mclient, result, df);
		face_recognition_thread.Stop();
		
		
	}
	private void sendResult(Messenger client, boolean success, double df) {
		if(mclient == null) {
			Log.e(LOG_TAG, "Service not connected");
		}
		if(success) {
			
            	try {
            		Log.d(LOG_TAG, "Test succeeded!");
    				mclient.send(Message.obtain(null, MSG_AUTH_SUCCESS, 0, 0));
    			} catch (RemoteException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
			
		} else {
			try {
				Log.d(LOG_TAG, "Test failed!");
				mclient.send(Message.obtain(null, MSG_AUTH_FAIL, 0, 0));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	@Override
	public void onFaceNotFoundException(FaceNotFoundException e) {
		// TODO Auto-generated method stub
		Log.e(LOG_TAG, "Face not found");
		face_recognition_thread.Stop();
		sendResult(mclient, false, 0);
	}
	@Override
	public void onIOException(IOException e) {
		// TODO Auto-generated method stub
		Log.e(LOG_TAG, "IO error");
		sendResult(mclient, false, 0);
	}
	@Override
	public void onRuntimeException(Exception e) {
		// TODO Auto-generated method stub
		Log.e(LOG_TAG, "Runtime exception");
		sendResult(mclient, false, 0);
		
	}

}
