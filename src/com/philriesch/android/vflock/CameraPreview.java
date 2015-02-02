package com.philriesch.android.vflock;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @SVN $Id: CameraPreview.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	public static final String LOG_TAG = "CameraPreview";
	private SurfaceHolder holder;
	
	private Camera camera;
	private List<Camera.Size> supportedSizes;
	public CameraPreview (Context context, Camera camera) {
		super(context);
		this.camera = camera;
		
		this.holder = getHolder();
		this.holder.addCallback(this);
	}
	
	public void surfaceCreated (SurfaceHolder holder) {
		try {
			this.camera.setPreviewDisplay(holder);
			this.camera.startPreview();
		}
		catch (IOException e) {
			Log.e("CameraPreview", e.getMessage());
			throw new RuntimeException("IOException in CameraPreview: " + e.getMessage());
		}
	}
	
	public void surfaceDestroyed (SurfaceHolder holder) {
		try {
			this.holder.removeCallback(this);
			this.camera.stopPreview();
			this.camera.setPreviewCallback(null);
			this.holder = null;
			this.camera = null;
			Log.d(LOG_TAG, "Surface destroyed");
		}
		catch (Exception e) {}
	}
	
	public void surfaceChanged (SurfaceHolder holder, int format, int w, int h) {
		if (this.holder.getSurface() == null) {
			return;
		}
		try {
			this.camera.stopPreview();
		}
		catch (Exception e) {}
		
		// Print supported camera sizes
		Camera.Parameters parameters = camera.getParameters();
		Camera.Size cursize = parameters.getPictureSize();
		Camera.Size preSize = parameters.getPreviewSize();
		Log.d(LOG_TAG, String.format("Current size/ w: %d, h: %d", cursize.width, cursize.height));
		Log.d(LOG_TAG, String.format("Preview size/ w: %d, h: %d", preSize.width, preSize.height));
		supportedSizes = parameters.getSupportedPictureSizes();
		for(Camera.Size size: supportedSizes) {
			Log.d(LOG_TAG, String.format("w: %d, h: %d", size.width, size.height));
		}
		parameters.setPictureSize(320, 240);
		parameters.setPreviewSize(320,  240);
		try {
			camera.setParameters(parameters);
			Camera.Size cgSize = camera.getParameters().getPictureSize();
			Camera.Size cgPreSize = camera.getParameters().getPreviewSize();
			Log.d(LOG_TAG, String.format("Changed size/ w: %d, h: %d", cgSize.width, cgSize.height));
			Log.d(LOG_TAG, String.format("Changed preview size/ w: %d, h: %d", cgPreSize.width, cgPreSize.height));
			
			this.camera.setPreviewDisplay(this.holder);
			this.camera.startPreview();
		}
		catch (IOException e) {
			Log.e("CameraPreview", e.getMessage());
			throw new RuntimeException("IOException in CameraPreview: " + e.getMessage());
		}
	}
	
}
