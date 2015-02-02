package com.philriesch.android.vflock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

/**
 * @SVN $Id: VflockActivity.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public abstract class VflockActivity extends Activity {

	@SuppressLint("InlinedApi")
	protected void makeFullscreen () {
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		                          WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if (Build.VERSION.SDK_INT < 19) {
			this.getWindow().getDecorView()
			.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		}
		else {
			this.getWindow().getDecorView()
			.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE);
		}
	}
	
}
