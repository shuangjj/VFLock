package com.philriesch.android.facerec.lbp.settings;

/**
 * @SVN $Id: LbpHistogramResolution.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LbpHistogramResolution {

	public final int width;
	
	public final int height;
	
	public final int bins;
	
	public LbpHistogramResolution (int width, int height, int bins) {
		this.width = width;
		this.height = height;
		this.bins = bins;
	}
}
