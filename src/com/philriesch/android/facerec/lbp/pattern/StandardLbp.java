package com.philriesch.android.facerec.lbp.pattern;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.philriesch.android.facerec.common.data.StandardImageData;
import com.philriesch.android.facerec.lbp.data.LbpHistogramData;
import com.philriesch.android.facerec.lbp.data.LbpPatternData;
import com.philriesch.android.facerec.lbp.settings.LbpHistogramResolution;
import com.philriesch.android.facerec.lbp.settings.LbpSettings;

/**
 * @SVN $Id: StandardLbp.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class StandardLbp {

	// LBP Encoding Block masks
	// +---+---+---+
	// | A | B | C |  LBP Encoding:
	// +---+---+---+  0xABCDEFGH
	// | H | . | D |
	// +---+---+---+
	// | G | F | E |
	// +---+---+---+
	//
	private static final int LBP_BLOCK_A = 0x08;
	private static final int LBP_BLOCK_B = 0x07;
	private static final int LBP_BLOCK_C = 0x06;
	private static final int LBP_BLOCK_D = 0x05;
	private static final int LBP_BLOCK_E = 0x04;
	private static final int LBP_BLOCK_F = 0x03;
	private static final int LBP_BLOCK_G = 0x02;
	private static final int LBP_BLOCK_H = 0x01;
	
	// State flags
	// 0 0 0 0  n n n n
	//          ^ ^ ^ ^----- Settings changed
	//          | | |------- Image ready
	//          | |--------- Patterns generated
	//          |----------- Histogram generated
	//
	private static final int FLAG_SETTINGS  = 0x01;
	private static final int FLAG_IMAGE     = 0x02;
	private static final int FLAG_PATTERNS  = 0x03;
	private static final int FLAG_HISTOGRAM = 0x04;
	
	private int state_flags;
	
	private LbpSettings settings;
	
	private StandardImageData image_data;
	
	private LbpPatternData pattern_data;
	
	private LbpHistogramData histogram_data;
	
	public StandardLbp (LbpSettings settings) {
		this.Reconfigure(settings);
	}
	
	public void Reconfigure (LbpSettings settings) {
		this.settings = settings;
		this.state_flags |= (1 << FLAG_SETTINGS);
	}
	
	public void NewImage (StandardImageData image_data) {
		this.image_data = image_data;
		this.state_flags |= (1 << FLAG_IMAGE);
	}
	
	public LbpPatternData GeneratePatterns () {
		// Check the status of the flags
		if ((this.state_flags & (1 << FLAG_SETTINGS)) > 0) {
			this.state_flags = this.state_flags & ~(1 << FLAG_SETTINGS);
		}
		if ((this.state_flags & (1 << FLAG_IMAGE)) < 1) {
			throw new RuntimeException("Standard LBP::GeneratePatterns Exception: I don't have valid image data, call ::NewImage first!");
		}
		
		// Generate the LBP pattern output
		Mat resized_input = new Mat(this.settings.image.height,
		                            this.settings.image.width,
		                            CvType.CV_8UC1);
		Mat lbp_output    = new Mat(this.settings.image.height,
		                            this.settings.image.width,
		                            CvType.CV_8UC1);
		Imgproc.resize(this.image_data.gray_mat, resized_input, 
		               new Size(this.settings.image.width,
		                        this.settings.image.height));
		for (int i = 0; i < this.settings.image.height; i++) {
			for (int j = 0; j < this.settings.image.width; j++) {
				int lbp_code = 0;
				if (j > 0) {
					if (i > 0) {
						// Test "A Block"
						if ((resized_input.get(i - 1, j - 1))[0] >= (resized_input.get(i, j))[0]) {
							lbp_code = lbp_code | (1 << LBP_BLOCK_A);
						}
					}
					if (i < this.settings.image.height - 1) {
						// Test "G Block"
						if ((resized_input.get(i + 1, j - 1))[0] >= (resized_input.get(i, j))[0]) {
							lbp_code = lbp_code | (1 << LBP_BLOCK_G);
						}
					}
					// Test "H Block"
					if ((resized_input.get(i, j - 1))[0] >= (resized_input.get(i, j))[0]) {
						lbp_code = lbp_code | (1 << LBP_BLOCK_H);
					}
				}
				if (j < this.settings.image.width - 1) {
					if (i > 0) {
						// Test "C Block"
						if ((resized_input.get(i - 1, j + 1))[0] >= (resized_input.get(i, j))[0]) {
							lbp_code = lbp_code | (1 << LBP_BLOCK_C);
						}
					}
					if (i < this.settings.image.height - 1) {
						// Test "E Block"
						if ((resized_input.get(i + 1, j + 1))[0] >= (resized_input.get(i, j))[0]) {
							lbp_code = lbp_code | (1 << LBP_BLOCK_E);
						}
					}
					// Test "D Block"
					if ((resized_input.get(i, j + 1))[0] >= (resized_input.get(i, j))[0]) {
						lbp_code = lbp_code | (1 << LBP_BLOCK_D);
					}
				}
				if (i > 0) {
					// Test "B Block"
					if ((resized_input.get(i - 1, j))[0] >= (resized_input.get(i, j))[0]) {
						lbp_code = lbp_code | (1 << LBP_BLOCK_B);
					}
				}
				if (i < this.settings.image.height - 1) {
					// Test "F Block"
					if ((resized_input.get(i + 1, j))[0] >= (resized_input.get(i, j))[0]) {
						lbp_code = lbp_code | (1 << LBP_BLOCK_F);
					}
				}
				lbp_output.put(i, j, lbp_code);
			}
		}
		
		// Save and return the new pattern data
		this.pattern_data = new LbpPatternData(this.settings.image, lbp_output);
		this.state_flags |= (1 << FLAG_PATTERNS);
		this.state_flags &= ~(1 << FLAG_HISTOGRAM);
		return this.pattern_data;
	}
	
	public LbpHistogramData GenerateHistogram () {
		// Check the status of the flags
		if ((this.state_flags & (1 << FLAG_SETTINGS)) > 0 ||
		    (this.state_flags & (1 << FLAG_IMAGE)) < 1 ||
		    (this.state_flags & (1 << FLAG_PATTERNS)) < 1) {
			this.GeneratePatterns();
		}
		
		// Calculate the histogram output
		int lbp_height   = this.pattern_data.resolution.height;
		int lbp_width    = this.pattern_data.resolution.width;
		int histo_height = this.settings.histogram.height;
		int histo_width  = this.settings.histogram.width;
		int bin_count    = this.settings.histogram.bins;
		if (histo_height > lbp_height) {
			histo_height = lbp_height;
			this.settings.histogram = new LbpHistogramResolution(histo_width,
			                                                     histo_height,
			                                                     bin_count);
		}
		if (histo_width > lbp_width) {
			histo_width = lbp_width;
			this.settings.histogram = new LbpHistogramResolution(histo_width,
			                                                     histo_height,
			                                                     bin_count);
		}
		Mat histogram_output = new Mat(1,
		                               (histo_width * histo_height * bin_count),
		                               CvType.CV_16UC1);
		for (int i = 0; i < (histo_width * histo_height * bin_count); i++) {
			histogram_output.put(0, i, 0);
		}
		for (int i = 1; i < histo_height + 1; i++) {
			for (int j = 1; j < histo_width + 1; j++) {
				int mstop = (i * lbp_height) / histo_height;
				int mstart = 0;
				int nstop = (j * lbp_width) / histo_width;
				int nstart = 0;
				if (i != 1) {
					mstart = ((i - 1) * lbp_height) / histo_height;
				}
				if (j != 1) {
					nstart = ((j - 1) * lbp_width) / histo_width;
				}
				int bin_offset = ((i - 1) * histo_width * bin_count) + ((j - 1) * bin_count);
				for (int m = mstart; m < mstop; m++) {
					for (int n = nstart; n < nstop; n++) {
						int lbp_code = (int) (this.pattern_data.pattern_mat.get(m, n))[0];
						int local_bin = lbp_code / (255 / (bin_count - 1));
						int exact_bin = bin_offset + local_bin;
						if (exact_bin >= (histo_width * histo_height * bin_count)) {
							throw new RuntimeException("Histogram bin number is out of range: "+String.valueOf(exact_bin));
						}
						int old_histo = (int) (histogram_output.get(0, exact_bin))[0];
						histogram_output.put(0, exact_bin, old_histo + 1);
					}
				}
			}
		}
		
		// Save and return the new histogram data
		this.histogram_data = new LbpHistogramData(this.settings.histogram, histogram_output);
		this.state_flags |= (1 << FLAG_HISTOGRAM);
		return this.histogram_data;
	}
	
}
