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

package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

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

	// Message types for UI thread
	public static final int MESSAGE_START = 3;
	public static final int MESSAGE_STOP = 4;
	
	// Available encoders
	public final static int VIDEO_H264 = 1;
	public final static int VIDEO_H263 = 2;
	public final static int AUDIO_AMRNB = 3;
	public final static int AUDIO_ANDROID_AMR = 4;
	public final static int AUDIO_AAC = 5; // Only for ICS

	// Default configuration
	private static int defaultVideoEncoder = VIDEO_H264, defaultAudioEncoder = AUDIO_AMRNB;
	private static VideoQuality defaultVideoQuality = VideoQuality.defaultVideoQualiy.clone();
	private static int defaultCamera = CameraInfo.CAMERA_FACING_BACK;
	private static SurfaceHolder surfaceHolder;
	
	// Indicates if a session is already streaming audio or video
	private static boolean cameraInUse = false;
	private static boolean micInUse = false;
	
	// Prevent two different sessions from using the same peripheral at the same time
	private static final Object LOCK = new Object();
	
	class Track {
		public static final int AUDIO = 1;
		public static final int VIDEO = 2;
		public Track(final Stream stream, final int type) {
			this.stream = stream;
			this.type = type;
		}
		public Stream stream;
		public int type;
	}
	
	private ArrayList<Track> tracks = new ArrayList<Track>(); 
	private InetAddress destination;
	private Handler handler;
	
	public Session(InetAddress destination, Handler handler) {
		this.destination = destination;
		this.handler = handler;
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
	}
	
	/** Add the default video track with default configuration */
	public void addVideoTrack() throws IllegalStateException, IOException {
		addVideoTrack(defaultVideoEncoder,defaultCamera,defaultVideoQuality,false);
	}
	
	/** Add default audio track with default configuration */
	public void addVideoTrack(int encoder, int camera, VideoQuality videoQuality, boolean flash) throws IllegalStateException, IOException {
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
			((VideoStream) stream).setPreviewDisplay(surfaceHolder);
			((VideoStream) stream).setFlashState(flash);
			stream.setDestination(destination, 5006);
			tracks.add(new Track(stream,Track.VIDEO));
		}
	}
	
	public void addAudioTrack() {
		addAudioTrack(defaultAudioEncoder);
	}
	
	public void addAudioTrack(int encoder) {
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
			Log.d(TAG,"Audio streaming: AAC (experimental)");
			stream = new AACStream();
			break;
		}
		
		if (stream != null) {
			stream.setDestination(destination, 5004);
			tracks.add(new Track(stream,Track.AUDIO));
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
		synchronized (LOCK) {
			for (int i=0;i<tracks.size();i++) {
				Track track = tracks.get(i);
				if ((track.type == Track.VIDEO && !cameraInUse) || (track.type == Track.AUDIO && !micInUse)) {
					sessionDescriptor += track.stream.generateSessionDescriptor();
					sessionDescriptor += "a=control:trackID="+i+"\r\n";
				}
			}
		}
		return sessionDescriptor;
	}
	
	public boolean trackExists(int id) {
		try{
			tracks.get(id);
			return true;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
	}
	
	public int getTrackDestinationPort(int id) {
		return tracks.get(id).stream.getDestinationPort();
	}

	public int getTrackLocalPort(int id) {
		return tracks.get(id).stream.getLocalPort();
	}
	
	public void setTrackDestinationPort(int id, int port) {
		tracks.get(id).stream.setDestination(destination,port);
	}
	
	public int getTrackSSRC(int id) {
		return tracks.get(id).stream.getSSRC();
	}
	
	/** The destination address for all the streams of the session
	 * @param destination The destination address
	 */
	public void setDestination(InetAddress destination) {
		this.destination =  destination;
	}
	
	/** Start stream with id trackId 
	 * @throws IOException, RuntimeException
	 * @throws IllegalStateException */
	public void start(int trackId) throws IllegalStateException, IOException, RuntimeException {
		Track track = tracks.get(trackId);
		Stream stream = track.stream;
		if (stream!=null && !stream.isStreaming()) {
			// Prevent two different sessions from using the same peripheral at the same time
			synchronized (LOCK) {
				if (track.type == Track.VIDEO) {
					if (!cameraInUse) {
						stream.prepare();
						stream.start();
						cameraInUse = true;
					}
				}
				if (track.type == Track.AUDIO) {
					if (!micInUse) {
						stream.prepare();
						stream.start();
						micInUse = true;
					}
				}
			}
		}
		handler.obtainMessage(MESSAGE_START).sendToTarget();
	}

	/** Start existing streams 
	 * @throws IOException, RuntimeException 
	 * @throws IllegalStateException */
	public void startAll() throws IllegalStateException, IOException, RuntimeException {
		for (int i=0;i<tracks.size();i++) {
			start(i);
		}
	}
	
	/** Stop existing streams */
	public void stopAll() {
		for (Iterator<Track> it = tracks.iterator();it.hasNext();) {
			it.next().stream.stop();
		}
		handler.obtainMessage(MESSAGE_STOP).sendToTarget();
	}
	
	/** Delete all existing tracks & release associated resources */
	public void flush() {
		for (Iterator<Track> it = tracks.iterator();it.hasNext();) {
			Track track = it.next();
			track.stream.release();
			if (track.type == Track.VIDEO) cameraInUse = false;
			else if (track.type == Track.AUDIO) micInUse = false;
		}
	}
	
}
