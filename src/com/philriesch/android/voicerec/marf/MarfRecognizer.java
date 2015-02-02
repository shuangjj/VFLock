package com.philriesch.android.voicerec.marf;

import marf.MARF;
import marf.util.MARFException;

/**
 * @SVN $Id: MarfRecognizer.java 119 2014-12-24 13:17:14Z phil $
 * @author Andy Lulciuc <tue85777@temple.edu>
 * Refactored by:
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class MarfRecognizer {

	public static final void Train (String pstrFilename, int id) throws MARFException {
		MARF.setSampleFile(pstrFilename);
		MARF.setCurrentSubject(id);
		MARF.train();
	}
	
	public static final int Predict (String pstrFilename) throws MARFException {
		MARF.setSampleFile(pstrFilename);
		MARF.recognize();
		return MARF.queryResultID();
	}
	
	public static final double GetLastOutcome () throws MARFException {
		return MARF.getResult().getOutcome();
	}
	
	public static final void SetDefaultConfig () throws MARFException {
		MARF.setPreprocessingMethod(MARF.BAND_STOP_FFT_FILTER);
		MARF.setFeatureExtractionMethod(MARF.FFT);
		MARF.setClassificationMethod(MARF.COSINE_SIMILARITY_MEASURE);
		MARF.setDumpSpectrogram(false);
		MARF.setSampleFormat(MARF.WAV);
	}
	
}
