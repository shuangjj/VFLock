package com.philriesch.android.vflock.threads;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import org.opencv.ml.CvSVM;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.philriesch.android.facerec.common.data.RectangleRoiData;
import com.philriesch.android.facerec.common.data.StandardImageData;
import com.philriesch.android.facerec.haar.HaarCascade;
import com.philriesch.android.facerec.haar.exception.FaceNotFoundException;
import com.philriesch.android.facerec.haar.settings.HaarCascadeSettings;
import com.philriesch.android.facerec.lbp.data.LbpHistogramData;
import com.philriesch.android.facerec.lbp.model.LbpHistogramSvm;
import com.philriesch.android.facerec.lbp.pattern.StandardLbp;
import com.philriesch.android.facerec.lbp.settings.LbpSettings;
import com.philriesch.android.vflock.threads.FacerecThreadEventInterface;
import com.philriesch.android.vflock.threads.ThreadBadStateException;

/**
 * @SVN $Id: FacerecThread.java 119 2014-12-24 13:17:14Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class FacerecThread implements Runnable {

	// My thread object

	private Thread my_thread;

	// Events callback interface

	private FacerecThreadEventInterface event_interface;

	// Asset access

	private AssetManager asset_manager;

	// Storage directories

	private String save_directory;

	private String cache_directory;

	// Haar objects

	private HaarCascadeSettings haar_cascade_settings;

	private HaarCascade         haar_cascade;

	// LBP objects

	private LbpSettings     lbp_settings;

	private StandardLbp     lbp_pattern_generator;

	private LbpHistogramSvm lbp_decision_model;

	/***************************************************************/
	// Thread state machine constants, variables, and concurrency-protected 
	// objects

	private static final int STATE_IDLE           = 0;
	private static final int STATE_CREATENEWMODEL = 1;
	private static final int STATE_OPENMODEL      = 2;
	private static final int STATE_MODELREADY     = 3;
	private static final int STATE_SAVEMODEL      = 4;
	private static final int STATE_TRAINSTART     = 5;
	private static final int STATE_TRAINPENDING   = 6;
	private static final int STATE_TESTSTART      = 7;
	private static final int STATE_TESTPENDING    = 8;
	private static final int STATE_STOP           = 9;

	private Object            state_lock             = new Object();
	private int               state_val              = STATE_STOP;
	private String            state_username         = null;
	private ArrayList<Bitmap> state_training_bitmaps = null;
	private Bitmap            state_testing_bitmap   = null;

	/***************************************************************/

	// BELOW IS ALL STUFF THAT YOU CAN USE TO CONTROL THE THREAD
	// AND GIVE IT TASKS TO DO

	// Constructor

	public FacerecThread (AssetManager device_assets,
			String save_abs_dir,
			String cache_abs_dir,
			FacerecThreadEventInterface event_interface) throws IOException {
		this.asset_manager = device_assets;
		this.save_directory = save_abs_dir;
		if (!this.save_directory.endsWith("/")) {
			this.save_directory += "/";
		}
		this.cache_directory = cache_abs_dir;
		if (!this.cache_directory.endsWith("/")) {
			this.cache_directory += "/";
		}
		this.event_interface       = event_interface;

		this.haar_cascade_settings = new HaarCascadeSettings(this.HaarClassifierInit());
		this.lbp_settings          = new LbpSettings(this.save_directory);

		this.haar_cascade          = new HaarCascade(this.haar_cascade_settings);
		this.lbp_pattern_generator = new StandardLbp(this.lbp_settings);
		this.lbp_decision_model    = new LbpHistogramSvm(this.lbp_settings);
		
		InflateNegativeImageDatabase();
	}

	// Thread start/stop controls

	public void Start () {
		synchronized (this.state_lock) {
			if (this.state_val == STATE_STOP) {
				this.state_val = STATE_IDLE;
				this.my_thread = new Thread(this, 
						"FaceRecognitionThread."+this.hashCode());
				this.my_thread.start();
			}
		}
	}

	public void Stop () {
		synchronized (this.state_lock) {
			this.state_val = STATE_STOP;
			
		}
	}
	public boolean isRunning() {
		return this.state_val != STATE_STOP;
	}
	// Functions for creating a new face recognition model

	public boolean CanCreateNewModel () {
		boolean result;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_IDLE || this.state_val == STATE_MODELREADY) {
				result = true;
			}
			else {
				result = false;
			}
		}
		return result;
	}

	public void CreateNewModel (String username) throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_IDLE || this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_CREATENEWMODEL;
				this.state_username = username;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}

	// Functions for opening an already existing model

	public boolean CanOpenModel () {
		return this.CanCreateNewModel();
	}

	public void OpenModel (String username) throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_IDLE || this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_OPENMODEL;
				this.state_username = username;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}

	// Functions for saving the current model

	public boolean CanSaveModel () {
		boolean result;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_MODELREADY) {
				result = true;
			}
			else {
				result = false;
			}
		}
		return result;
	}

	public void SaveModel () throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_SAVEMODEL;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}

	// Functions for submitting training bitmaps to be processed by the thread

	public boolean CanTrain () {
		return this.CanSaveModel();
	}

	public void Train (ArrayList<Bitmap> training_bitmaps) throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_TRAINSTART;
				this.state_training_bitmaps = training_bitmaps;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}

	// Functions for submitting a testing bitmap to be processed by the thread

	public boolean CanTest () {
		return this.CanSaveModel();
	}

	public void Test (Bitmap testing_bitmap) throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_TESTSTART;
				this.state_testing_bitmap = testing_bitmap;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}

	/***************************************************************/

	// BELOW IS ALL STUFF THAT IS INTERNAL LOGIC -- DON'T TOUCH!!!

	// Thread main routine

	@Override
	public void run() {
		int tmp_state_val                      = STATE_STOP;
		ArrayList<Bitmap> tmp_training_bitmaps = null;
		Bitmap tmp_testing_bitmap              = null;
		double tmp_test_result                 = 0.0;
		Exception caught_exception             = null;
		boolean running                        = true;
		
		// If this isn't here, then the thread competes with the UI thread (bad!)
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		
		this.event_interface.onThreadStart();
		
		while (running) {

			caught_exception = null;
			
			synchronized(this.state_lock) {
				tmp_state_val = this.state_val;
			}

			switch (tmp_state_val) {

			case STATE_CREATENEWMODEL:
				this.event_interface.onModelCreateStart(this.state_username);
				synchronized (this.state_lock) {
					this.lbp_decision_model.CreateNewModel(this.state_username);
					this.state_val = STATE_MODELREADY;
				}
				this.event_interface.onModelCreateComplete(this.state_username);
				break;

			case STATE_OPENMODEL:
				this.event_interface.onModelOpenStart(this.state_username);
				synchronized (this.state_lock) {
					this.lbp_decision_model.OpenModel(this.state_username);
					this.state_val = STATE_MODELREADY;
				}
				this.event_interface.onModelOpenComplete(this.state_username);
				break;

			case STATE_SAVEMODEL:
				this.event_interface.onModelSaveStart(this.state_username);
				synchronized (this.state_lock) {
					this.lbp_decision_model.SaveModel(this.state_username);
					this.state_val = STATE_MODELREADY;
				}
				this.event_interface.onModelSaveComplete(this.state_username);
				break;

			case STATE_TRAINSTART:
				this.event_interface.onTrainStart();
				
				synchronized (this.state_lock) {
					tmp_training_bitmaps = this.state_training_bitmaps;
					this.state_val = STATE_TRAINPENDING;
				}
				
				try {
					this.LbpTrainingLogic(tmp_training_bitmaps);
					caught_exception = null;
				}
				catch (Exception e) {
					caught_exception = e;
				}

				if (caught_exception == null) {
					synchronized (this.state_lock) {
						this.state_val = STATE_MODELREADY;
					}
					this.event_interface.onTrainComplete();
				}
				else {
					if (caught_exception instanceof FaceNotFoundException) {
						this.event_interface.onFaceNotFoundException((FaceNotFoundException)caught_exception);
					}
					else if (caught_exception instanceof IOException) {
						this.event_interface.onIOException((IOException)caught_exception);
					}
					else {
						this.event_interface.onRuntimeException(caught_exception);
					}
				}
				break;

			case STATE_TESTSTART:
				this.event_interface.onTestStart();
				synchronized (this.state_lock) {
					tmp_testing_bitmap = this.state_testing_bitmap;
					this.state_val = STATE_TESTPENDING;
				}
				tmp_test_result = 0.0;
				try {
					tmp_test_result = this.LbpTestingLogic(tmp_testing_bitmap);
					caught_exception = null;
				}
				catch (Exception e) {
					caught_exception = e;
				}
				synchronized (this.state_lock) {
					this.state_val = STATE_MODELREADY;
				}
				if (caught_exception == null) {
					if (tmp_test_result < 0) {
						this.event_interface.onTestComplete(true, tmp_test_result);
					}
					else {
						this.event_interface.onTestComplete(false, tmp_test_result);
					}
				}
				else {
					if (caught_exception instanceof FaceNotFoundException) {
						this.event_interface.onFaceNotFoundException((FaceNotFoundException)caught_exception);
					}
					else if (caught_exception instanceof IOException) {
						this.event_interface.onIOException((IOException)caught_exception);
					}
					else {
						this.event_interface.onRuntimeException(caught_exception);
					}
				}
				break;

			case STATE_STOP:
				running = false;
				break;

			default:
				break;

			}

		}
		this.event_interface.onThreadStop();
		
	}

	// Function for intializing the haar classifier

	private String HaarClassifierInit () throws IOException {
		File cascade_cache = new File(this.cache_directory + "haarcascade_frontalface_alt2.xml");
		if (!cascade_cache.exists()) {
			InputStream in = this.asset_manager.open("haarcascade_frontalface_alt2.xml");
			FileOutputStream out = new FileOutputStream(cascade_cache);
			byte[] buff = new byte[2048];
			int k = 0;
			while ((k = in.read(buff)) != -1) {
				out.write(buff, 0, k);
			}
			out.flush();
			out.close();
			in.close();
		}
		return this.cache_directory + "haarcascade_frontalface_alt2.xml";
	}
	
	// Function for inflating the negative image database
	
	private void InflateNegativeImageDatabase () throws IOException {
		File caltech_negative_db = new File(this.cache_directory + "caltech-negative-faces-1/");
		File caltech_negative_db_item = null;
		if (!caltech_negative_db.exists()) {
			caltech_negative_db.mkdir();
		}
		TarInputStream in = new TarInputStream(
				new BufferedInputStream(this.asset_manager.open("caltech_negative_faces_1.tar")));
		TarEntry entry = null;
		while ((entry = in.getNextEntry()) != null) {
			caltech_negative_db_item = new File(this.cache_directory + "caltech-negative-faces-1/" + entry.getName());
			if (!caltech_negative_db_item.exists()) {
				FileOutputStream out = new FileOutputStream(caltech_negative_db_item);
				byte[] buff = new byte[1024];
				int k = 0;
				while ((k = in.read(buff)) != -1) {
					out.write(buff, 0, k);
				}
				out.flush();
				out.close();
			}
		}
		in.close();
	}
	
	// Generates a new random negative image set to use when training the SVM
	
	private ArrayList<StandardImageData> GenerateNewNegativeImageSet () {
		// Get ready
		long set_select = 0;
		int next_set;
		int next_pic;
		String next_filename;
		File next_file;
		Random r = new Random();
		ArrayList<StandardImageData> negative_set = new ArrayList<StandardImageData>();
		
		// Build a new negative set
		while (negative_set.size() < 10) {
			next_set = r.nextInt(19) + 1;
			if ((set_select & (1 << next_set)) > 0) {
				continue;
			}
			set_select |= 1 << next_set;
			while (true) {
				next_pic = r.nextInt(6) + 1;
				next_filename = this.cache_directory + "caltech-negative-faces-1/";
				switch (next_set) {
				case 1:
					next_filename += "a0";
					break;
				case 2:
					next_filename += "b0";
					break;
				case 3:
					next_filename += "c0";
					break;
				case 4:
					next_filename += "d0";
					break;
				case 5:
					next_filename += "e0";
					break;
				case 6:
					next_filename += "f0";
					break;
				case 7:
					next_filename += "g0";
					break;
				case 8:
					next_filename += "h0";
					break;
				case 9:
					next_filename += "i0";
					break;
				case 10:
					next_filename += "j0";
					break;
				case 11:
					next_filename += "k0";
					break;
				case 12:
					next_filename += "l0";
					break;
				case 13:
					next_filename += "m0";
					break;
				case 14:
					next_filename += "n0";
					break;
				case 15:
					next_filename += "o0";
					break;
				case 16:
					next_filename += "p0";
					break;
				case 17:
					next_filename += "q0";
					break;
				case 18:
					next_filename += "r0";
					break;
				case 19:
					next_filename += "s0";
					break;
				default:
					throw new RuntimeException("Random number generator yielded invalid number");
				}
				next_filename += next_pic + ".jpg";
				next_file = new File(next_filename);
				if (next_file.exists()) {
					negative_set.add(StandardImageData.FilenameRead(next_filename));
					break;
				}
			}
		}
		
		// Return the set
		return negative_set;
	}

	// LBP Training Logic

	private void LbpTrainingLogic (ArrayList<Bitmap> training_bitmaps) throws FaceNotFoundException {
		// Convert the positive training bitmaps to cv standard image data
		ArrayList<StandardImageData> training_images_pos = new ArrayList<StandardImageData>();
		for (int i = 0; i < training_bitmaps.size(); i++) {
			training_images_pos.add(StandardImageData.BitmapConvert(training_bitmaps.get(i)));
		}
		
		// Select some negative images from the negative image database
		ArrayList<StandardImageData> training_images_neg = GenerateNewNegativeImageSet();

		// Convert the positive training images into histograms
		RectangleRoiData face_perimeter = null;
		StandardImageData isolated_face = null;
		LbpHistogramData this_histogram = null;
		ArrayList<LbpHistogramData> training_histo_pos = new ArrayList<LbpHistogramData>();
		for (int i = 0; i < training_images_pos.size(); i++) {
			// Find the face
			face_perimeter = this.haar_cascade.FindFace(training_images_pos.get(i));

			// Face find success callback
			this.event_interface.onFaceFind(training_images_pos.get(i), face_perimeter);

			// Isolate the face
			isolated_face = HaarCascade.CropFace(training_images_pos.get(i), face_perimeter);

			// Generate LBP patterns and histogram
			this.lbp_pattern_generator.NewImage(isolated_face);
			this.lbp_pattern_generator.GeneratePatterns();
			this_histogram = this.lbp_pattern_generator.GenerateHistogram();
			training_histo_pos.add(this_histogram);
		}
		
		// Convert the negative training images into hostograms
		// ...You'll notice that we don't isolate the face for these -- when the negative
		// face database was generated the faces were already pre-cropped, so we don't have
		// to do that step.
		ArrayList<LbpHistogramData> training_histo_neg = new ArrayList<LbpHistogramData>();
		for (int i = 0; i < training_images_neg.size(); i++) {
			// Generate LBP patterns and histogram
			this.lbp_pattern_generator.NewImage(training_images_neg.get(i));
			this.lbp_pattern_generator.GeneratePatterns();
			this_histogram = this.lbp_pattern_generator.GenerateHistogram();
			training_histo_neg.add(this_histogram);
		}

		// Train the model
		this.lbp_decision_model.Train(training_histo_pos, training_histo_neg);
	}

	// LBP Testing Logic

	private double LbpTestingLogic (Bitmap testing_bitmap) throws FaceNotFoundException {
		// Convert the bitmap to cv standard image data
		StandardImageData testing_image = StandardImageData.BitmapConvert(testing_bitmap);

		// Find the face
		RectangleRoiData face_perimeter = this.haar_cascade.FindFace(testing_image);

		// Face find success callback
		this.event_interface.onFaceFind(testing_image, face_perimeter);

		// Isolate the face
		StandardImageData isolated_face = HaarCascade.CropFace(testing_image, face_perimeter);

		// Generate LBP patterns and histogram
		this.lbp_pattern_generator.NewImage(isolated_face);
		this.lbp_pattern_generator.GeneratePatterns();
		LbpHistogramData test_histogram = this.lbp_pattern_generator.GenerateHistogram();

		// Test is against the trained model
		double test_result = this.lbp_decision_model.Test(test_histogram);

		// Return the decision function as a result
		return test_result;
	}

}
