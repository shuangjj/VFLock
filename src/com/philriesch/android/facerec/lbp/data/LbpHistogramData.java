package com.philriesch.android.facerec.lbp.data;

import org.opencv.core.Mat;
import com.philriesch.android.facerec.lbp.settings.LbpHistogramResolution;

/**
 * @SVN $Id: LbpHistogramData.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LbpHistogramData {

	public final LbpHistogramResolution resolution;
	
	public final Mat histogram_mat;
	
	public LbpHistogramData (LbpHistogramResolution resolution, Mat histogram_mat) {
		this.resolution = resolution;
		this.histogram_mat = histogram_mat;
	}
	
}
