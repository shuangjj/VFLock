package com.philriesch.android.vflock.threads;

/**
 * @SVN $Id: SecurityManagementThread.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class SecurityManagementThread implements Runnable {
	
	private boolean running;
	
	private boolean screen_state;
	
	private boolean lock_state;
	
	private boolean auth_state;
	
	private SecurityManagementEventInterface callback;
	
	private Thread my_thread;
	
	public SecurityManagementThread (SecurityManagementEventInterface callback) {
		this.callback = callback;
		running       = false;
		screen_state  = false;
		lock_state    = false;
		auth_state    = false;
	}
	
	public void ScreenStateOff () {
		screen_state = false;
	}
	
	public void ScreenStateOn () {
		screen_state = true;
	}
	
	public void LockStateOff () {
		lock_state = false;
	}
	
	public void LockStateOn () {
		lock_state = true;
	}
	
	public void AuthStateOff () {
		auth_state = false;
	}
	
	public void AuthStateOn () {
		auth_state = true;
	}
	
	public void Start () {
		this.running = true;
		this.my_thread = new Thread(this,
				"SecurityManagementThread."+this.hashCode());
		this.my_thread.start();
	}
	
	public void Stop () {
		this.running = false;
	}

	@Override
	public void run() {
		while (running) {
			try {
				if (screen_state) {
					Thread.sleep(3000);
					if (screen_state && lock_state && !auth_state) {
						callback.onSecurityFault();
					}
				}
				else {
					Thread.sleep(3000);
				}
			}
			catch (InterruptedException e) {}
		}
	}

}
