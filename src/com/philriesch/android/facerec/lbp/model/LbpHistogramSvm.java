package com.philriesch.android.facerec.lbp.model;

import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.ml.CvSVM;

import com.philriesch.android.facerec.lbp.data.LbpHistogramData;
import com.philriesch.android.facerec.lbp.settings.LbpSettings;

/**
 * SVN $Id: LbpHistogramSvm.java 117 2014-12-15 22:22:37Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 */
public class LbpHistogramSvm {

	private LbpSettings settings;
	
	private String username;
	
	private CvSVM svm;
	
	public LbpHistogramSvm (LbpSettings settings) {
		this.Reconfigure(settings);
	}
	
	public void Reconfigure (LbpSettings settings) {
		this.settings = settings;
	}
	
	public void CreateNewModel (String username) {
		this.username = username;
		this.svm = new CvSVM();
	}
	
	private String getModelFilename () {
		return this.settings.filesystem_root+"/"+this.username+".xml";
	}
	
	public void OpenModel (String username) {
		this.CreateNewModel(username);
		this.svm.load(this.getModelFilename());
	}
	
	public void SaveModel (String username) {
		this.svm.save(this.getModelFilename());
	}
	
	public void Train (ArrayList<LbpHistogramData> positive_histograms,
	                   ArrayList<LbpHistogramData> negative_histograms) {
		// Correctness checking of the training sets
		int positive_set_size = positive_histograms.size();
		int negative_set_size = 0;
		if (negative_histograms != null) {
			negative_set_size = negative_histograms.size();
		}
		int training_set_size = positive_set_size + negative_set_size;
		if (training_set_size == 0) {
			throw new RuntimeException("Training histogram ArrayLists are both empty!");
		}
		int histogram_size = this.settings.histogram.width * this.settings.histogram.height * this.settings.histogram.bins;
		
		// Construct a training set and a labels set from the positive and
		// negative sets
		Mat training_set = new Mat(training_set_size, histogram_size, CvType.CV_32FC1);
		Mat labels_set   = new Mat(training_set_size, 1, CvType.CV_32FC1);
		int current_bin  = 0;
		for (int i = 0; i < positive_set_size; i++) {
			for (int j = 0; j < histogram_size; j++) {
				training_set.put(current_bin, 
				                 j, 
				                 (positive_histograms.get(i)).histogram_mat.get(0, j));
			}
			labels_set.put(current_bin, 0, 1.0);
			current_bin++;
		}
		if (negative_set_size > 0) {
			for (int i = 0; i < negative_set_size; i++) {
				for (int j = 0; j < histogram_size; j++) {
					training_set.put(current_bin,
							j,
							(negative_histograms.get(i)).histogram_mat.get(0, j));
				}
				labels_set.put(current_bin, 0, -1.0);
				current_bin++;
			}
		}
		
		// Train the SVM
		if (!this.svm.train(training_set, labels_set, new Mat(), new Mat(), this.settings.svm_parameters)) {
			throw new RuntimeException("SVM training failed!");
		}
	}
	
	public double Test (LbpHistogramData test_histogram) {
		// Build the testing set
		int histogram_size = this.settings.histogram.width * this.settings.histogram.height * this.settings.histogram.bins;
		Mat testing_set = new Mat(1, histogram_size, CvType.CV_32FC1);
		for (int i = 0; i < histogram_size; i++) {
			testing_set.put(0, 
			                i, 
			                test_histogram.histogram_mat.get(0, i));
		}
		
		// Test the SVM
		return this.svm.predict(testing_set, true);
	}

}
