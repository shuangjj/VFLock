package com.philriesch.android.vflock.threads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import marf.MARF;
import marf.util.MARFException;

import com.philriesch.android.voicerec.marf.MarfRecognizer;

import android.content.res.AssetManager;
import android.util.Log;

/**
 * @SVN $Id: VoicerecThread.java 120 2014-12-29 02:52:00Z phil $
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class VoicerecThread implements Runnable {

	// My thread object
	
	private Thread my_thread;
	
	// Events callback interface
	
	private VoicerecThreadEventInterface event_interface;
	
	// Asset access
	
	private AssetManager asset_manager;
	
	// Storage directories
	
	private String save_directory;
	
	private String cache_directory;
	
	/***************************************************************/
	// Thread state machine constants, variables, and concurrency-protected 
	// objects
	
	private static final int STATE_IDLE           = 0;
	private static final int STATE_CREATENEWMODEL = 1;
	private static final int STATE_MODELREADY     = 3;
	private static final int STATE_TRAINSTART     = 5;
	private static final int STATE_TRAINPENDING   = 6;
	private static final int STATE_TESTSTART      = 7;
	private static final int STATE_TESTPENDING    = 8;
	private static final int STATE_STOP           = 9;
	
	private Object           state_lock           = new Object();
	private int              state_val            = STATE_STOP;
	private File             state_training_file  = null;
	private File             state_testing_file   = null;
	
	/***************************************************************/
	
	// BELOW IS ALL STUFF THAT YOU CAN USE TO CONTROL THE THREAD
	// AND GIVE IT TASKS TO DO
	
	// Constructor
	
	public VoicerecThread (AssetManager device_assets,
			String save_abs_dir,
			String cache_abs_dir,
			VoicerecThreadEventInterface event_interface) throws IOException {
		this.asset_manager = device_assets;
		this.save_directory = save_abs_dir;
		if (!this.save_directory.endsWith("/")) {
			this.save_directory += "/";
		}
		this.cache_directory = cache_abs_dir;
		if (!this.cache_directory.endsWith("/")) {
			this.cache_directory += "/";
		}
		this.event_interface = event_interface;
	}
	
	// Thread start/stop controls
	
	public void Start () {
		synchronized (this.state_lock) {
			if (this.state_val == STATE_STOP) {
				this.state_val = STATE_IDLE;
				this.my_thread = new Thread(this,
						"VoiceRecognitionThread."+this.hashCode());
				this.my_thread.start();
			}
		}
	}
	
	public void Stop () {
		synchronized (this.state_lock) {
			this.state_val = STATE_STOP;
		}
	}
	
	// Functions for creating a new voice recognition model
	
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
	
	public void CreateNewModel () throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_IDLE || this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_CREATENEWMODEL;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}
	
	// Functions for submitting a training file to be processed by the thread
	
	public boolean CanTrain () {
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
	
	public void Train (File training_file) throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_TRAINSTART;
				this.state_training_file = training_file;
			}
			else {
				e = new ThreadBadStateException();
			}
		}
		if (e != null) {
			throw e;
		}
	}
	
	// Functions for submitting a testing file to be processed by the thread
	
	public boolean CanTest () {
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
	
	public void Test (File testing_file) throws ThreadBadStateException {
		ThreadBadStateException e = null;
		synchronized (this.state_lock) {
			if (this.state_val == STATE_MODELREADY) {
				this.state_val = STATE_TESTSTART;
				this.state_testing_file = testing_file;
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
		int tmp_state_val          = STATE_STOP;
		File tmp_training_file     = null;
		File tmp_testing_file      = null;
		double tmp_test_result     = 0.0;
		boolean running            = true;
		Exception caught_exception = null;
		
		// If this isn't here, then the thread competes with the UI thread (bad!)
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
				
		this.event_interface.onThreadStart();
		
		while (running) {
			
			caught_exception = null;
			
			synchronized (this.state_lock) {
				tmp_state_val = this.state_val;
			}
			
			switch (tmp_state_val) {
			
			case STATE_IDLE:
				this.event_interface.onModelOpenStart(null);
				synchronized (this.state_lock) {
					try {
						InflateMarfTrainingSet();
						MarfRecognizer.SetDefaultConfig();
					} catch (MARFException e) {
						caught_exception = e;
					} catch (IOException e) {
						caught_exception = e;
					}
					this.state_val = STATE_MODELREADY;
				}
				if (caught_exception == null) {
					this.event_interface.onModelOpenComplete(null);
				}
				else if (caught_exception instanceof MARFException) {
					this.event_interface.onMARFException((MARFException)caught_exception);
				}
				else if (caught_exception instanceof IOException) {
					this.event_interface.onIOException((IOException)caught_exception);
				}
				else {
					this.event_interface.onRuntimeException(caught_exception);
				}
				break;
			
			case STATE_CREATENEWMODEL:
				this.event_interface.onModelCreateStart(null);
				synchronized (this.state_lock) {
					File g = new File(this.save_directory + "marf.Storage.TrainingSet.700.0.0.113.301.512.gzbin");
					g.delete();
					this.state_val = STATE_IDLE;
				}
				this.event_interface.onModelCreateComplete(null);
				break;
				
			case STATE_TRAINSTART:
				this.event_interface.onTrainStart();
				synchronized (this.state_lock) {
					tmp_training_file = this.state_training_file;
					this.state_val = STATE_TRAINPENDING;
				}
				try {
					this.MarfTrainingLogic(tmp_training_file);
				} catch (MARFException e) {
					caught_exception = e;
				}
				synchronized (this.state_lock) {
					this.state_val = STATE_MODELREADY;
				}
				if (caught_exception == null) {
					this.event_interface.onTrainComplete();
				}
				else {
					this.event_interface.onMARFException((MARFException)caught_exception);
				}
				break;
				
			case STATE_TESTSTART:
				this.event_interface.onTestStart();
				synchronized (this.state_lock) {
					tmp_testing_file = this.state_testing_file;
					this.state_val = STATE_TESTPENDING;
				}
				try {
					tmp_test_result = this.MarfTestingLogic(tmp_testing_file);
				}
				catch (MARFException e) {
					caught_exception = e;
				}
				synchronized (this.state_lock) {
					this.state_val = STATE_MODELREADY;
				}
				if (caught_exception == null) {
					if (tmp_test_result > 0.75) {
						this.event_interface.onTestComplete(true, tmp_test_result);
					}
					else {
						this.event_interface.onTestComplete(false, tmp_test_result);
					}
				}
				else {
					this.event_interface.onMARFException((MARFException)caught_exception);
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
	
	// Function for inflating the MARF training set via the device
	// asset manager
	
	private void InflateMarfTrainingSet () throws IOException {
		// MARF always looks in the app save directory for the storage file whose
		// filename matches its configuration settings...kinda dumb but whoever
		// wrote MARF hardcoded it to do that...whatever.
		File g = new File(this.save_directory + "marf.Storage.TrainingSet.700.0.0.113.301.512.gzbin");
		if (!g.exists()) {
			InputStream  is = this.asset_manager.open("marf.Storage.TrainingSet.700.0.0.113.301.512.gzbin");
			OutputStream os = new FileOutputStream(g);
			byte buffer[] = new byte[1024];
			int length = 0;
			
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
			
			os.flush();
			os.close();
			is.close();
		}
	}
	
	// MARF Training Logic
	
	private void MarfTrainingLogic (File training_file) throws MARFException {
		MarfRecognizer.Train(training_file.getAbsolutePath(), 31);
	}
	
	// MARF Testing Logic
	
	private double MarfTestingLogic (File testing_file) throws MARFException {
		int id = MarfRecognizer.Predict(testing_file.getAbsolutePath());
		if (id == 31) {
			Log.d("VoicerecThread", "Voice recognition profile match: "+id+" (PASS)");
			return MarfRecognizer.GetLastOutcome();
		}
		else {
			Log.d("VoicerecThread", "Voice recognition profile match: "+id+" (FAIL)");
			return 0.0;
		}
	}

}
