/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.majorkernelpanic.libstreaming;

import java.io.IOException;
import java.net.InetAddress;

import net.majorkernelpanic.libstreaming.audio.AMRNBStream;
import net.majorkernelpanic.libstreaming.audio.GenericAudioStream;
import net.majorkernelpanic.libstreaming.video.H263Stream;
import net.majorkernelpanic.libstreaming.video.H264Stream;
import net.majorkernelpanic.libstreaming.video.VideoQuality;
import net.majorkernelpanic.libstreaming.video.VideoStream;
import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * 
 */
public class StreamManager {

	public final static String TAG = "StreamingManager";
	
	// Encoders available
	public final static int VIDEO_H264 = 1;
	public final static int VIDEO_H263 = 2;
	public final static int AUDIO_AMRNB = 3;
	public final static int AUDIO_ANDROID_AMR = 4;
	
	private final Context context;
	private InetAddress destination;
	private SurfaceHolder surfaceHolder;
	private int defaultVideoEncoder = VIDEO_H264;
	private boolean defaultSoundEnabled = true;
	public VideoQuality defaultVideoQuality = VideoQuality.defaultVideoQualiy.clone();
	private int defaultCamera = CameraInfo.CAMERA_FACING_BACK;
	
	public StreamManager(Context context) {
		this.context = context;
	}
	
	private Stream videoStream = null;
	private Stream audioStream = null;
	
	/** */
	public void startNewSession() {
		flush();
	}
	
	/** */
	public void setDefaultVideoQuality(VideoQuality quality) {
		defaultVideoQuality = quality;
	}
	
	/** */
	public void setDefaultSoundOption(boolean enable) {
		defaultSoundEnabled = enable;
	}
	
	/** */
	public void setDefaultVideoEncoder(int encoder) {
		defaultVideoEncoder = encoder;
	}
	
	/** */
	public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
		this.surfaceHolder = surfaceHolder;
	}
	
	public void addVideoTrack(int destinationPort) throws IllegalStateException, IOException {
		addVideoTrack(defaultVideoEncoder,defaultCamera,destinationPort,defaultVideoQuality);
	}
	
	public void addVideoTrack(int encoder, int camera, int destinationPort, VideoQuality videoQuality) throws IllegalStateException, IOException {
		VideoQuality.merge(videoQuality,defaultVideoQuality);
		
		switch (encoder) {
		case VIDEO_H264:
			Log.d(TAG,"Video streaming: H.264");
			videoStream = new H264Stream(context,camera);
			break;
		case VIDEO_H263:
			Log.d(TAG,"Video streaming: H.263");
			videoStream = new H263Stream(context,camera);
			break;
		}
		
		if (videoStream != null) {
			Log.d(TAG,"Quality is: "+videoQuality.resX+"x"+videoQuality.resY+" "+videoQuality.frameRate+" fps, "+videoQuality.bitRate+" bps");
			videoStream.setDestination(destination, destinationPort);
			((VideoStream) videoStream).setVideoQuality(videoQuality);
			((VideoStream) videoStream).setPreviewDisplay(surfaceHolder);
		}
		
	}
	
	public void addAudioTrack(int destinationPort) {
		if (defaultSoundEnabled) addAudioTrack(AUDIO_AMRNB, destinationPort);
	}
	
	public void addAudioTrack(int encoder, int destinationPort) {
		
		switch (encoder) {
		case AUDIO_AMRNB:
			Log.d(TAG,"Audio streaming: AMR");
			audioStream = new AMRNBStream();
			break;
		case AUDIO_ANDROID_AMR:
			Log.d(TAG,"Audio streaming: GENERIC");
			audioStream = new GenericAudioStream();
			break;
		}
		
		if (audioStream != null) audioStream.setDestination(destination, destinationPort);
		
	}
	
	public void setFlashState(boolean state) {
		if (videoStream != null) {
			((VideoStream)videoStream).setFlashState(state);
		}
		else {
			Log.e(TAG,"setFlashState() failed !");
		}
	}
	
	/** Return a session descriptor that can be stored in a file or sent to a client with RTSP */
	public String getSessionDescriptor() throws IllegalStateException, IOException {
		String sdp = "";
		if (videoStream != null) {
			sdp += videoStream.generateSdpDescriptor();
			sdp += "a=control:trackID=0\r\n";
		}
		if (audioStream != null) {
			sdp += audioStream.generateSdpDescriptor();
			sdp += "a=control:trackID=1\r\n";
		}
		return sdp;
	}
	
	public boolean trackExists(int id) {
		return id==0?videoStream!=null:audioStream!=null;
	}
	
	public int getTrackPort(int id) {
		return id==0?videoStream.getDestinationPort():audioStream.getDestinationPort();
	}
	
	public int getTrackSSRC(int id) {
		return id==0?videoStream.getSSRC():audioStream.getSSRC();
	}
	
	public void setDestination(InetAddress destination) {
		this.destination =  destination;
	}
	
	/** Start all streams of the session */
	public void startAll() throws RuntimeException, IllegalStateException, IOException {
		if (videoStream != null) {
			videoStream.prepare();
			videoStream.start();
		}
		if (audioStream != null) {
			audioStream.prepare();
			audioStream.start();
		}
	}
	
	/** Stop existing streams */
	public void stopAll() {
		if (videoStream != null) videoStream.stop();
		if (audioStream != null) audioStream.stop();
	}
	
	/** Delete all existing tracks & release associated resources */
	public synchronized void flush() {
		if (videoStream != null) {
			videoStream.release();
			videoStream = null;
		}
		if (audioStream != null) {
			audioStream.release();
			audioStream = null;
		}
	}
	
}
