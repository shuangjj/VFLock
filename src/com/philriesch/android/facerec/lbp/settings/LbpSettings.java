package com.philriesch.android.facerec.lbp.settings;

import org.opencv.ml.CvSVM;
import org.opencv.ml.CvSVMParams;

/**
 * @SVN $Id: LbpSettings.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class LbpSettings {

	public LbpImageResolution image;
	
	public LbpHistogramResolution histogram;
	
	public CvSVMParams svm_parameters;
	
	public final String filesystem_root;
	
	public LbpSettings (String filesystem_root) {
		this.image = new LbpImageResolution(256, 256);
		this.histogram = new LbpHistogramResolution(12, 12, 5);
		this.svm_parameters = new CvSVMParams();
		this.svm_parameters.set_svm_type(CvSVM.C_SVC);
		this.svm_parameters.set_kernel_type(CvSVM.LINEAR);
		this.svm_parameters.set_C(2.67);
		this.svm_parameters.set_gamma(5.383);
		this.filesystem_root = filesystem_root;
	}
	
}
