package net.mkp.spydroid;

import java.io.IOException;
import java.net.InetAddress;

import net.mkp.spydroid.librtp.AMRNBPacketizer;
import net.mkp.spydroid.librtp.H264Packetizer;

import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

/*
 * 
 * 
 */

public class CameraStreamer {

	private MediaStreamer sound = null, video = null;
	private AMRNBPacketizer sstream = null;
	private H264Packetizer vstream = null;
	
	public void setup(SurfaceHolder holder, String ip) throws IOException {
	
		// AUDIO
		
		sound = new MediaStreamer();
		
		sound.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		sound.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		sound.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
		try {
			sound.prepare();
		} catch (IOException e) {
			throw new IOException("Can't stream sound :(");
		}
		
		try {
			sstream = new AMRNBPacketizer(sound.getOutputStream(), InetAddress.getByName(ip));
		} catch (IOException e) {
			Log.e(SpydroidActivity.LOG_TAG,"Unknown host");
			throw new IOException("Can't resolve host :(");
		}
		
		// VIDEO
		
		video = new MediaStreamer();
		
		video.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		video.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		video.setVideoFrameRate(20);
		video.setVideoSize(640, 480);
		video.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		video.setPreviewDisplay(holder.getSurface());
		
		try {
			video.prepare();
		} catch (IOException e) {
			throw new IOException("Can't stream video :(");
		}
		
		try {
			vstream = new H264Packetizer(video.getOutputStream(), InetAddress.getByName(ip));
		} catch (IOException e) {
			Log.e(SpydroidActivity.LOG_TAG,"Unknown host");
			throw new IOException("Can't resolve host :(");
		}
		
	}
	
	public void start() {
	
		// Start sound streaming
		sound.start();
		sstream.startStreaming();
		
		// Start video streaming
		video.start();
		vstream.startStreaming();
		
	}
	
	public void stop() {
	
		// Stop sound streaming
		sstream.stopStreaming();
		sound.stop();
	
		// Stop video streaming
		vstream.stopStreaming();
		video.stop();
		
	}
	
}
