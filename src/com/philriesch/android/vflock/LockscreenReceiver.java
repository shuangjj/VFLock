package com.philriesch.android.vflock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * @SVN $Id: LockscreenReceiver.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LockscreenReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_SCREEN_ON)) {
			Log.d("LockscreenReceiver", "ACTION_SCREEN_ON");
			VflockApplication.SecurityScreenStateOn();
			startLockscreen(context, intent);
		}
		else if (action.equals(VflockApplication.SECURITY_FAULT)) {
			Log.d("LockscreenReceiver", "SECURITY_FAULT");
			Toast.makeText(context, "Phone is locked. Please authenticate to continue.", Toast.LENGTH_LONG).show();
			startLockscreen(context, intent);
		}
		else if (action.equals(Intent.ACTION_SCREEN_OFF) ||
				 action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.d("LockscreenReceiver", "ACTION_SCREEN_OFF | ACTION_BOOT_COMPLETED");
			VflockApplication.SecurityScreenStateOff();
			VflockApplication.SecurityLockStateOn();
			LockscreenService.EnableKeyguard();
		}
	}
	
	private void startLockscreen (Context context, Intent intent) {
		Intent authintent = new Intent(context, LockscreenActivity.class);
		authintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(authintent);
	}
	
}
