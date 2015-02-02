package com.philriesch.android.facerec.common.data;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;

/**
 * @SVN $Id: StandardImageData.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class StandardImageData {
	
	public final Mat color_mat;
	
	public final Mat gray_mat;
	
	public StandardImageData (Mat color_mat, Mat gray_mat) {
		this.color_mat = color_mat;
		this.gray_mat = gray_mat;
	}
	
	public static StandardImageData BitmapConvert (Bitmap bitmap) {
		Mat color_mat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC3);
		Mat gray_mat  = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
		Utils.bitmapToMat(bitmap, color_mat);
		Imgproc.cvtColor(color_mat, gray_mat, Imgproc.COLOR_RGB2GRAY);
		return new StandardImageData(color_mat, gray_mat);
	}
	
	public static StandardImageData FilenameRead (String filename) {
		Mat color_mat = Highgui.imread(filename, Highgui.CV_LOAD_IMAGE_COLOR);
		Mat gray_mat  = new Mat(color_mat.width(), color_mat.height(), CvType.CV_8UC1);
		Imgproc.cvtColor(color_mat, gray_mat, Imgproc.COLOR_RGB2GRAY);
		return new StandardImageData(color_mat, gray_mat);
	}
	
}
