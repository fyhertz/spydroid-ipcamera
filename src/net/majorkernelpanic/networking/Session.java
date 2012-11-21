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

package net.majorkernelpanic.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import net.majorkernelpanic.streaming.Stream;
import net.majorkernelpanic.streaming.audio.AACStream;
import net.majorkernelpanic.streaming.audio.AMRNBStream;
import net.majorkernelpanic.streaming.audio.GenericAudioStream;
import net.majorkernelpanic.streaming.video.H263Stream;
import net.majorkernelpanic.streaming.video.H264Stream;
import net.majorkernelpanic.streaming.video.VideoQuality;
import net.majorkernelpanic.streaming.video.VideoStream;
import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * This class makes use of all the streaming package
 * It represents a streaming session between a client and the phone
 * A stream is designated by the word "track" in this class
 * To add tracks to the session you need to call addVideoTrack() or addAudioTrack()
 */
public class Session {

	public final static String TAG = "Session";

	// Thos two messages will inform the handler if there is streaming going on or not
	public static final int MESSAGE_START = 3;
	public static final int MESSAGE_STOP = 4;
	
	// Available encoders
	public final static int VIDEO_H264 = 1;
	public final static int VIDEO_H263 = 2;
	public final static int AUDIO_AMRNB = 3;
	public final static int AUDIO_ANDROID_AMR = 4;
	public final static int AUDIO_AAC = 5; // Only for ICS

	// Default configuration
	public static VideoQuality defaultVideoQuality = VideoQuality.defaultVideoQualiy.clone();
	private static int defaultVideoEncoder = VIDEO_H264, defaultAudioEncoder = AUDIO_AMRNB;
	private static int defaultCamera = CameraInfo.CAMERA_FACING_BACK;
	
	// Indicates if a session is already streaming audio or video
	private static boolean cameraInUse = false;
	private static Stream videoStream = null;
	private static boolean micInUse = false;
	private static Stream audioStream = null;
	
	private static AtomicInteger startedStreamCount = new AtomicInteger(0);
	private static Handler handler;
	private static SurfaceHolder surfaceHolder;
	private InetAddress destination;
	private Stream[] streamList = new Stream[2];
	
	public Session(InetAddress destination) {
		this.destination = destination;
	}

	/** Indicates whether or not a camera is being used in a session **/
	public static boolean isCameraInUse() {
		return cameraInUse;
	}
	
	/** Indicates whether or not the microphone is being used in a session **/
	public static boolean isMicrophoneInUse() {
		return micInUse;
	}
	
	/** Set the handler that will be used to signal the main thread that a session has started or stopped */
	public static void setHandler(Handler h) {
		handler = h;
	}	
	
	/** Set default video stream quality, it will be used by addVideoTrack */
	public static void setDefaultVideoQuality(VideoQuality quality) {
		defaultVideoQuality = quality;
	}
	
	/** Set the default audio encoder, it will be used by addAudioTrack */
	public static void setDefaultAudioEncoder(int encoder) {
		defaultAudioEncoder = encoder;
	}
	
	/** Set the default video encoder, it will be used by addVideoTrack() */
	public static void setDefaultVideoEncoder(int encoder) {
		defaultVideoEncoder = encoder;
	}
	
	/** Set the Surface required by MediaRecorder to record video */
	public static void setSurfaceHolder(SurfaceHolder sh) {
		surfaceHolder = sh;
		surfaceHolder.addCallback(new SurfaceHolder.Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {
			}
			public void surfaceCreated(SurfaceHolder holder) {
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.d(TAG,"Surface destroyed !!");
				if (cameraInUse) {
					cameraInUse = false;
					videoStream.stop();
					if ( audioStream==null || (audioStream != null && !audioStream.isStreaming()) )
							handler.obtainMessage(Session.MESSAGE_STOP).sendToTarget();
				}
			}
			
		});
	}
	
	/** Add the default video track with default configuration */
	public void addVideoTrack() throws IllegalStateException, IOException {
		addVideoTrack(defaultVideoEncoder,defaultCamera,defaultVideoQuality,false);
	}
	
	/** Add video track with specified quality and encoder */
	public synchronized void addVideoTrack(int encoder, int camera, VideoQuality videoQuality, boolean flash) throws IllegalStateException, IOException {
		if (cameraInUse) throw new IllegalStateException("Camera already in use by another client");
		Stream stream = null;
		VideoQuality.merge(videoQuality,defaultVideoQuality);
		
		switch (encoder) {
		case VIDEO_H264:
			Log.d(TAG,"Video streaming: H.264");
			stream = new H264Stream(camera);
			break;
		case VIDEO_H263:
			Log.d(TAG,"Video streaming: H.263");
			stream = new H263Stream(camera);
			break;
		}
		
		if (stream != null) {
			Log.d(TAG,"Quality is: "+videoQuality.resX+"x"+videoQuality.resY+"px "+videoQuality.frameRate+"fps, "+videoQuality.bitRate+"bps");
			((VideoStream) stream).setVideoQuality(videoQuality);
			((VideoStream) stream).setPreviewDisplay(surfaceHolder.getSurface());
			((VideoStream) stream).setFlashState(flash);
			stream.setDestination(destination, 5006);
			streamList[0] = stream;
			videoStream = stream;
			cameraInUse = true;
		}
	}
	
	/** Add default audio track with default configuration */
	public void addAudioTrack() {
		addAudioTrack(defaultAudioEncoder);
	}
	
	/** Add audio track with specified encoder */
	public synchronized void addAudioTrack(int encoder) {
		if (micInUse) throw new IllegalStateException("Microphone already in use by another client");
		Stream stream = null;
		
		switch (encoder) {
		case AUDIO_AMRNB:
			Log.d(TAG,"Audio streaming: AMR");
			stream = new AMRNBStream();
			break;
		case AUDIO_ANDROID_AMR:
			Log.d(TAG,"Audio streaming: GENERIC");
			stream = new GenericAudioStream();
			break;
		case AUDIO_AAC:
			if (Integer.parseInt(android.os.Build.VERSION.SDK)<14) throw new IllegalStateException("This phone does not support AAC :/");
			Log.d(TAG,"Audio streaming: AAC (experimental)");
			stream = new AACStream();
			break;
		}
		
		if (stream != null) {
			stream.setDestination(destination, 5004);
			streamList[1] = stream;
			audioStream = stream;
			micInUse = true;
		}
		
	}
	
	/** Return a session descriptor that can be stored in a file or sent to a client with RTSP
	 * @return The session descriptor
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public String getSessionDescriptor() throws IllegalStateException, IOException {
		String sessionDescriptor = "";
		// Prevent two different sessions from using the same peripheral at the same time
		for (int i=0;i<streamList.length;i++) {
			if (streamList[i] != null) {
				if (!streamList[i].isStreaming()) {
					sessionDescriptor += streamList[i].generateSessionDescriptor();
					sessionDescriptor += "a=control:trackID="+i+"\r\n";
				} else {
					throw new IllegalStateException("Make sure all streams are stopped before calling getSessionDescriptor()");
				}
			}
		}
		return sessionDescriptor;
	}
	
	public boolean trackExists(int id) {
		return streamList[id]!=null;
	}
	
	public int getTrackDestinationPort(int id) {
		return streamList[id].getDestinationPort();
	}

	public int getTrackLocalPort(int id) {
		return streamList[id].getLocalPort();
	}
	
	public void setTrackDestinationPort(int id, int port) {
		streamList[id].setDestination(destination,port);
	}
	
	public int getTrackSSRC(int id) {
		return streamList[id].getSSRC();
	}
	
	/** The destination address for all the streams of the session
	 * @param destination The destination address
	 */
	public void setDestination(InetAddress destination) {
		this.destination =  destination;
	}

	/** Start stream with id trackId */
	public void start(int trackId) throws IllegalStateException, IOException {
		String type = trackId==0 ? "Video stream" : "Audio stream";
		Stream stream = streamList[trackId];
		if (stream!=null && !stream.isStreaming()) {
			stream.prepare();
			stream.start();
			if (startedStreamCount.addAndGet(1)==1) handler.obtainMessage(Session.MESSAGE_START).sendToTarget();
		}
	}

	/** Start existing streams */
	public void startAll() throws IllegalStateException, IOException {
		for (int i=0;i<streamList.length;i++) {
			start(i);
		}
	}

	/** Stop existing streams */
	public void stopAll() {
		for (int i=0;i<streamList.length;i++) {
			if (streamList[i] != null && streamList[i].isStreaming()) {
				streamList[i].stop();
				if (startedStreamCount.addAndGet(-1)==0) handler.obtainMessage(Session.MESSAGE_STOP).sendToTarget();
			}
		}
	}
	
	/** Delete all existing tracks & release associated resources */
	public void flush() {
		for (int i=0;i<streamList.length;i++) {
			if (streamList[i] != null) {
				streamList[i].release();
				if (i == 0) cameraInUse = false;
				else micInUse = false;
			}
		}
	}
	
}
