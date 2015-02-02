package com.philriesch.android.vflock.threads;

import java.io.IOException;

import marf.util.MARFException;

/**
 * @SVN $Id: VoicerecThreadEventInterface.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public interface VoicerecThreadEventInterface extends IdentityThreadEvents {
	
	public void onTrainStart       ();
	
	public void onTrainComplete    ();
	
	public void onTestStart        ();
	
	public void onTestComplete     (boolean result, 
	                                double prob);
	
	public void onMARFException    (MARFException e);
	
	public void onIOException      (IOException e);
	
	public void onRuntimeException (Exception e);
	
}
