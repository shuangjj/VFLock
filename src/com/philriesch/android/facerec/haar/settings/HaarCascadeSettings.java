package com.philriesch.android.facerec.haar.settings;

/**
 * SVN $Id: HaarCascadeSettings.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class HaarCascadeSettings {

	public String classifier_file;
	
	public double scale_factor;
	
	public HaarCascadeSettings (String classifier_file) {
		this.classifier_file = classifier_file;
		this.scale_factor = 1.3;
	}
	
}
