package com.philriesch.android.vflock;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;
import android.view.View;

/**
 * @SVN $Id: AbstractCameraActivity.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public abstract class AbstractCameraActivity extends VflockActivity {

	protected Camera camera;
	
	protected CameraPreview preview = null;
	
	protected AbstractCameraActivity scoperesolve;
	
	@Override
	protected void onPause () {
		super.onPause();
		if (this.camera != null) {
			Log.d("AbstractCameraActivity", "Releasing the camera");
			this.preview.getHolder().removeCallback(this.preview);
			this.camera.stopPreview();
			this.camera.setPreviewCallback(null);
			this.camera.release();
			this.camera = null;
		}
	}
	
	protected abstract void CameraResume ();
	
	public abstract void onCapture (View view);
	
	protected Camera getFrontCamera () {
		try {
			if (!this.hasCameraHardware(this)) {
				Log.e("AbstractCameraActivity", "This device has no cameras");
				throw new RuntimeException("This device has no cameras");
			}
			if (this.findFrontCameraId() != -1) {
				return Camera.open(this.findFrontCameraId());
			}
			else {
				Log.e("AbstractCameraActivity", "This device has no front facing camera");
				throw new RuntimeException("This device has no front facing camera");
			}
		}
		catch (Exception e) {
			Log.e("AbstractCameraActivity", "Could not open front facing camera: " + e.getMessage());
			throw new RuntimeException("Could not open front facing camera: " + e.getMessage());
		}
	}
	
	protected Camera getRearCamera () {
		try {
			if (!this.hasCameraHardware(this)) {
				Log.e("AbstractCameraActivity", "This device has no cameras");
				throw new RuntimeException("This device has no cameras");
			}
			if (this.findRearCameraId() != -1) {
				return Camera.open(this.findRearCameraId());
			}
			else {
				Log.e("AbstractCameraActivity", "This device has no rear facing camera");
				throw new RuntimeException("This device has no rear facing camera");
			}
		}
		catch (Exception e) {
			Log.e("AbstractCameraActivity", "Could not open rear facing camera: " + e.getMessage());
			throw new RuntimeException("Could not open rear facing camera: " + e.getMessage());
		}
	}
	
	private boolean hasCameraHardware (Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private int findFrontCameraId () {
		return findCameraId(CameraInfo.CAMERA_FACING_FRONT);
	}
	
	private int findRearCameraId () {
		return findCameraId(CameraInfo.CAMERA_FACING_BACK);
	}
	
	private int findCameraId (int camera_facing) {
		int cid = -1;
		int cno = Camera.getNumberOfCameras();
		for (int i = 0; i < cno; i++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == camera_facing) {
				cid = i;
				break;
			}
		}
		return cid;
	}
	
}
