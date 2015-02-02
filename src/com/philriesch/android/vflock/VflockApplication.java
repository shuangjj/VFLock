package com.philriesch.android.vflock;

import org.opencv.android.OpenCVLoader;

import com.philriesch.android.vflock.threads.SecurityManagementThread;
import com.philriesch.android.vflock.threads.SecurityManagementEventInterface;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

/**
 * @SVN $Id: VflockApplication.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class VflockApplication extends Application
	implements SecurityManagementEventInterface {

	public static String SECURITY_FAULT = 
			"com.philriesch.android.vflock.SECURITY_FAULT";
	
	private static SecurityManagementThread security_management;
	
	public static void SecurityScreenStateOff () {
		if (security_management != null) {
			security_management.ScreenStateOff();
		}
	}
	
	public static void SecurityScreenStateOn () {
		if (security_management != null) {
			security_management.ScreenStateOn();
		}
	}
	
	public static void SecurityLockStateOff () {
		if (security_management != null) {
			security_management.LockStateOff();
		}
	}
	
	public static void SecurityLockStateOn () {
		if (security_management != null) {
			security_management.LockStateOn();
		}
	}
	
	public static void SecurityAuthStateOff () {
		if (security_management != null) {
			security_management.AuthStateOff();
		}
	}
	
	public static void SecurityAuthStateOn () {
		if (security_management != null) {
			security_management.AuthStateOn();
		}
	}
	
	@Override
	public void onCreate () {
		super.onCreate();
		
		// Initialize OpenCV
		OpenCVLoader.initDebug();
		
		security_management = new SecurityManagementThread(this);
		security_management.Start();
	}
	
	@Override
	public void onTerminate () {
		super.onTerminate();
		
		security_management.Stop();
	}

	@Override
	public void onSecurityFault() {
		Intent faultintent = new Intent();
		faultintent.setAction(SECURITY_FAULT);
		sendBroadcast(faultintent);
	}
	
}
