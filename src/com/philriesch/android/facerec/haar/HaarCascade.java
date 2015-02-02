package com.philriesch.android.facerec.haar;

import java.io.IOException;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import com.philriesch.android.facerec.common.data.RectangleRoiData;
import com.philriesch.android.facerec.common.data.StandardImageData;
import com.philriesch.android.facerec.haar.exception.FaceNotFoundException;
import com.philriesch.android.facerec.haar.settings.HaarCascadeSettings;

/**
 * SVN $Id: HaarCascade.java 116 2014-12-14 22:17:17Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class HaarCascade {

	private HaarCascadeSettings settings;
	
	private CascadeClassifier classifier;
	
	public HaarCascade (HaarCascadeSettings settings) throws IOException {
		this.settings = settings;
		this.classifier = new CascadeClassifier(this.settings.classifier_file);
		if (this.classifier.empty()) {
			throw new IOException("Cascade classifier failed to load.");
		}
	}
	
	public RectangleRoiData FindFace (StandardImageData input_image) throws FaceNotFoundException {
		MatOfRect results = new MatOfRect();
		this.classifier.detectMultiScale(
				input_image.gray_mat,
				results,
				this.settings.scale_factor,
				2,
				2,
				new Size(10, 10),
				new Size(500, 500));
		Rect[] resultrect = results.toArray();
		if (resultrect.length < 1) {
			throw new FaceNotFoundException();
		}
		int xa = resultrect[0].x;
		int ya = resultrect[0].y;
		int xb = resultrect[0].x + resultrect[0].width;
		int yb = resultrect[0].y + resultrect[0].height;
		if (resultrect[0].width > resultrect[0].height) {
			yb = resultrect[0].y + resultrect[0].width;
		}
		else {
			xb = resultrect[0].x + resultrect[0].height;
		}
		return new RectangleRoiData(xa, ya, xb, yb);
	}
	
	public static StandardImageData CropFace (StandardImageData input_image,
	                                          RectangleRoiData face_area) {
		Mat color_mat = input_image.color_mat;
		Mat gray_mat  = input_image.gray_mat;
		Mat color_face = color_mat.submat(new Rect(
				new Point(face_area.XA(), face_area.YA()),
				new Point(face_area.XB(), face_area.YB())));
		Mat gray_face  =  gray_mat.submat(new Rect(
				new Point(face_area.XA(), face_area.YA()),
				new Point(face_area.XB(), face_area.YB())));
		return new StandardImageData(color_face, gray_face);
	}
	
}
