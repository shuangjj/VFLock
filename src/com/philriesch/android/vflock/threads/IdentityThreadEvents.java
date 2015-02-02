package com.philriesch.android.vflock.threads;

/**
 * @SVN $Id: IdentityThreadEvents.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public abstract interface IdentityThreadEvents {

	public void onThreadStart         ();
	
	public void onThreadStop          ();
	
	public void onModelCreateStart    (String username);
	
	public void onModelCreateComplete (String username);
	
	public void onModelOpenStart      (String username);
	
	public void onModelOpenComplete   (String username);
	
	public void onModelSaveStart      (String username);
	
	public void onModelSaveComplete   (String username);
	
}
