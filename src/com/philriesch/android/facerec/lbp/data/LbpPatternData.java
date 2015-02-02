package com.philriesch.android.facerec.lbp.data;

import org.opencv.core.Mat;
import com.philriesch.android.facerec.lbp.settings.LbpImageResolution;

/**
 * @SVN $Id: LbpPatternData.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LbpPatternData {

	public final LbpImageResolution resolution;
	
	public final Mat pattern_mat;
	
	public LbpPatternData (LbpImageResolution resolution, Mat pattern_mat) {
		this.resolution = resolution;
		this.pattern_mat = pattern_mat;
	}
	
}
