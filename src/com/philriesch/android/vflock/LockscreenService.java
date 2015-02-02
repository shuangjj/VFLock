package com.philriesch.android.vflock;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

/**
 * @SVN $Id: LockscreenService.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LockscreenService extends Service {

	private BroadcastReceiver receiver;
	
	private static boolean                      keyguard_ready   = false;
	private static KeyguardManager              keyguard_manager = null;
	private static KeyguardManager.KeyguardLock keyguard_lock    = null;
	private static boolean                      is_locked        = false;
	
	@SuppressWarnings("deprecation")
	public static synchronized void DisableKeyguard () {
		if (keyguard_ready && is_locked) {
			if (keyguard_lock == null) {
				keyguard_lock = keyguard_manager.newKeyguardLock("IN");
			}
			keyguard_lock.disableKeyguard();
			is_locked = false;
		}
	}
	
	@SuppressWarnings("deprecation")
	public static synchronized void EnableKeyguard () {
		if (keyguard_ready && !is_locked) {
			if (keyguard_lock == null) {
				keyguard_lock = keyguard_manager.newKeyguardLock("IN");
			}
			keyguard_lock.reenableKeyguard();
			keyguard_lock = null;
			is_locked = true;
		}
	}
	
	public static synchronized boolean KeyguardBypassed () {
		return is_locked;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate () {
		keyguard_manager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
		keyguard_ready = true;
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		filter.addAction(VflockApplication.SECURITY_FAULT);
		receiver = new LockscreenReceiver();
		registerReceiver(receiver, filter);
		
		super.onCreate();
	}
	
	@Override
	public void onDestroy () {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
}
