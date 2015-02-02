package com.philriesch.android.vflock.threads;

import java.io.IOException;

import com.philriesch.android.facerec.common.data.RectangleRoiData;
import com.philriesch.android.facerec.common.data.StandardImageData;
import com.philriesch.android.facerec.haar.exception.FaceNotFoundException;

/**
 * @SVN $Id: FacerecThreadEventInterface.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public interface FacerecThreadEventInterface extends IdentityThreadEvents {

	public void onFaceFind              (StandardImageData image,
			                             RectangleRoiData  facerect);
	
	public void onTrainStart            ();
	
	public void onTrainComplete         ();
	
	public void onTestStart             ();
	
	public void onTestComplete          (boolean result,
			                             double  df);
	
	public void onFaceNotFoundException (FaceNotFoundException e);
	
	public void onIOException           (IOException e);
	
	public void onRuntimeException      (Exception e);
	
}
