package com.philriesch.android.voicerec.recorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * @SVN $Id: WaveRecorder.java 119 2014-12-24 13:17:14Z phil $
 * @author Andy Lulciuc <tue85777@temple.edu>
 * Refactored by:
 * @author Phil Riesch <phil@philriesch.com>
 *
 */
public class WaveRecorder {

	//Begin private fields for this class
	private AudioRecord recorder;

	private static final int RECORDER_BPP = 16;
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = "test.wav";
	private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_CHANNELS_INT = 1;

	File f;

	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	short[] buffer;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	private boolean isIOException = false;
	private boolean isFileNotFoundException = false;
	private Exception caught_exception = null;
	
	private String save_dir;
	private String cache_dir;
	
	public WaveRecorder (String savedir, String cachedir) {
		save_dir = savedir;
		cache_dir = cachedir;
		int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, 
				RECORDER_AUDIO_ENCODING);
		int buffercount = 4088 / bufferSize;
		if (buffercount < 1) {
			buffercount = 1;
		}
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				RECORDER_SAMPLERATE, 
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, 
				44100);
	}
	
	public void Start (final String filepath, final String file)
		throws IllegalStateException, IOException {
		buffer = new short[4088];
		recorder.startRecording();
		isRecording = true;
		isIOException = false;
		isFileNotFoundException = false;
		caught_exception = null;
		recordingThread = new Thread(new Runnable() 
		{
			@Override
			public void run() {
				try {
					writeAudioDataToFile(getTempFilename());
				} 
				catch (FileNotFoundException e) {
					isIOException = true;
					caught_exception = e;
				}
				catch (IOException e) {
					isFileNotFoundException = true;
					caught_exception = e;
				}
			}
		}, "AudioRecorder Thread");
		recordingThread.start();
	}
	
	public File Stop () throws FileNotFoundException, IOException {
		stopRecording();
		if (isIOException) {
			throw (IOException)caught_exception;
		}
		else if (isFileNotFoundException) {
			throw (FileNotFoundException)caught_exception;
		}
		return f;
	}
	
	public boolean IsRecording () {
		if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private void stopRecording ()
		throws FileNotFoundException, IOException {
		if (recorder != null) {
			isRecording = false;
			recorder.stop();
			recorder.release();
			recorder = null;
			recordingThread = null;
		}
		// copy the recorded file to original copy & delete the recorded copy
		copyWaveFile(getTempFilename(), getFilename());
		deleteTempFile();
	}
	
	private String getFilename () {
		File file = new File(save_dir, AUDIO_RECORDER_FOLDER);
		if (!file.exists()) {
			file.mkdirs();
		}
		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_FILE_EXT_WAV);
	}
	
	private void deleteTempFile () {
		File file = new File(getTempFilename());
		file.delete();
	}
	
	private void copyWaveFile (String inFilename, String outFilename)
		throws FileNotFoundException, IOException {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = RECORDER_CHANNELS_INT;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

		f = new File(outFilename);

		in = new FileInputStream(inFilename);
		out = new FileOutputStream(f);
		totalAudioLen = in.getChannel().size();
		totalDataLen = totalAudioLen + 36;
		WriteWaveFileHeader(out, 
				totalAudioLen, 
				totalDataLen,
				longSampleRate, 
				channels, 
				byteRate);
		byte[] bytes2 = new byte[buffer.length * 2];
		ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN)
		.asShortBuffer().put(buffer);
		while (in.read(bytes2) != -1) {
			out.write(bytes2);
		}
		in.close();
		out.close();
	}
	
	private String getTempFilename () {
		// Creates the temp file to store buffer
		File file = new File(cache_dir, AUDIO_RECORDER_FOLDER);
		if (!file.exists()) {
			file.mkdirs();
		}
		File tempFile = new File(cache_dir, AUDIO_RECORDER_TEMP_FILE);
		if (tempFile.exists())
			tempFile.delete();
		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}
	
	private void writeAudioDataToFile (final String filename)
		throws FileNotFoundException, IOException {
		// Write the output audio in byte
		FileOutputStream os = new FileOutputStream(filename);
		int read = 0;
		while (isRecording) {
			// gets the voice output from microphone to byte format
			recorder.read(buffer, 0, buffer.length);
			if (AudioRecord.ERROR_INVALID_OPERATION != read) {
				// // writes the data to file from buffer
				// // stores the voice buffer
				// to turn shorts back to bytes.
				byte[] bytes2 = new byte[buffer.length * 2];
				ByteBuffer.wrap(bytes2).order(ByteOrder.LITTLE_ENDIAN)
				.asShortBuffer().put(buffer);
				os.write(bytes2);
			}
		}
		os.close();
	}
	
	private void WriteWaveFileHeader (FileOutputStream out,
			long totalAudioLen,
			long totalDataLen,
			long longSampleRate,
			int channels,
			long byteRate) throws IOException {
		byte[] header = new byte[4088];
		header[0]  = 'R'; // RIFF/WAVE header
		header[1]  = 'I';
		header[2]  = 'F';
		header[3]  = 'F';
		header[4]  = (byte) (totalDataLen & 0xff);
		header[5]  = (byte) ((totalDataLen >> 8) & 0xff);
		header[6]  = (byte) ((totalDataLen >> 16) & 0xff);
		header[7]  = (byte) ((totalDataLen >> 24) & 0xff);
		header[8]  = 'W';
		header[9]  = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) RECORDER_CHANNELS_INT;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (RECORDER_CHANNELS_INT * RECORDER_BPP / 8); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 4088);
	}
	
}
