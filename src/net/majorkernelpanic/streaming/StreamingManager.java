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

package net.majorkernelpanic.streaming;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import net.majorkernelpanic.librtp.AMRNBPacketizer;
import net.majorkernelpanic.librtp.AbstractPacketizer;
import net.majorkernelpanic.librtp.H264Packetizer;
import net.majorkernelpanic.librtp.RtpSocket;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;

/**
 * 
 * 
 * 
 *
 */
public class StreamingManager {

	public final static String TAG = "StreamingManager";
	 
	public StreamingManager() {
		
	}
	
	// Contains a list of all the tracks that has been added
	private HashMap<Integer,Track> trackList = new HashMap<Integer,Track>();
	
	private InetAddress destination;
	
	private class Track {
		public static final int TYPE_AUDIO = 1;
		public static final int TYPE_VIDEO = 2;
		public Track(MediaStreamer mediaStreamer, int trackType) {
			streamer = mediaStreamer;
			type = trackType;
		}
		AbstractPacketizer packetizer;
		public int type, encoder, format, port, ssrc;
		public MediaStreamer streamer;
		public VideoQuality videoQuality;
		public SurfaceHolder surfaceHolder;
		public String descriptor;
	}
	
	public int addH264Track(int videoSource, int destinationPort, String[] params, VideoQuality videoQuality, SurfaceHolder surfaceHolder) {
		
		Track track = new Track(new MediaStreamer(),Track.TYPE_VIDEO);

		track.packetizer = new H264Packetizer();
		track.encoder = MediaRecorder.VideoEncoder.H264;
		track.format = MediaRecorder.OutputFormat.THREE_GPP;
		track.port = destinationPort;
		track.surfaceHolder = surfaceHolder;
		track.videoQuality = videoQuality;
		
		track.descriptor = "m=video "+String.valueOf(track.port)+" RTP/AVP 96\r\n" +
						   "b=RR:0\r\n" +
						   "a=rtpmap:96 H264/90000\r\n" +
						   "a=fmtp:96 packetization-mode=1;profile-level-id="+params[0]+";sprop-parameter-sets="+params[2]+","+params[1]+";\r\n";
		
		configureTrack(track);
		
		return 0;
	}
	
	public void addAMRNBTrack(int audioSource, int destinationPort) {
		
		Track track = new Track(new MediaStreamer(),Track.TYPE_AUDIO);

		track.packetizer = new AMRNBPacketizer();
		track.encoder = MediaRecorder.AudioEncoder.AMR_NB;
		track.format = MediaRecorder.OutputFormat.RAW_AMR;
		track.port = destinationPort;
		
		track.descriptor = "m=audio "+String.valueOf(track.port)+" RTP/AVP 96\r\n" +
						   "b=AS:128\r\n" +
						   "b=RR:0\r\n" +
						   "a=rtpmap:96 AMR/8000\r\n" +
						   "a=fmtp:96 octet-align=1;\r\n";
		
		configureTrack(track);
		
	}
	

	public int getTrackPort(int trackId) {
		return trackList.get(trackId).port;
	}
	
	public int getTrackSSRC(int trackId) {
		return trackList.get(trackId).ssrc;
	}
	
	public String getTrackDescriptor(int trackId) {
		return trackList.get(trackId).descriptor;
	}
	
	public String getSessionDescriptor() {
		String sdp = "";
		Iterator<Track> it = trackList.values().iterator();
		while (it.hasNext()) {
			sdp += it.next().descriptor;
		}
		return sdp;
	}
	
	public void flush() {
		stopAll();
		trackList.clear();
	}
	
	public boolean trackExists(int trackId) {
		return trackList.containsKey(trackId);
	}
	
	public void setDestination(InetAddress destination) {
		this.destination =  destination;
	}

	public synchronized void prepareAll() throws IllegalStateException, IOException {
		
		Iterator<Track> it = trackList.values().iterator();
		
		// Let's start all MediaStreamers
		while (it.hasNext()) {
			MediaStreamer streamer = it.next().streamer;
			streamer.prepare();
		}
		
	}
	
	public synchronized void startAll() throws RuntimeException, IllegalStateException {
		
		Iterator<Track> it = trackList.values().iterator();
		
		// Let's start all MediaStreamers
		while (it.hasNext()) {
			MediaStreamer streamer = it.next().streamer;
			streamer.start();
		}
		
	}
	
	public synchronized void stopAll() {
		
		Iterator<Track> it = trackList.values().iterator();
		
		// Let's stop all MediaStreamers
		while (it.hasNext()) {
			MediaStreamer streamer = it.next().streamer;
			streamer.reset();
		}
		
	}
	
	private synchronized void configureTrack(Track track) {
		
		final MediaStreamer streamer = track.streamer;
		int id = generateId();
		
		switch (track.type) {
			
		case Track.TYPE_AUDIO:
			
			streamer.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			streamer.setOutputFormat(track.format);
			streamer.setAudioEncoder(track.encoder);
			streamer.setAudioChannels(1);
			break;
			
		case Track.TYPE_VIDEO:
			
			streamer.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			streamer.setOutputFormat(track.format);
			streamer.setVideoFrameRate(track.videoQuality.frameRate);
			streamer.setVideoSize(track.videoQuality.resX,track.videoQuality.resY);
			streamer.setVideoEncodingBitRate(track.videoQuality.bitRate);
			streamer.setVideoEncoder(track.encoder);
			streamer.setPreviewDisplay(track.surfaceHolder.getSurface());
			
			track.surfaceHolder.addCallback(new SurfaceHolder.Callback() {
				public void surfaceChanged(SurfaceHolder holder, int format,
						int width, int height) {
					// TODO Auto-generated method stub
					
				}
				public void surfaceCreated(SurfaceHolder holder) {
					// TODO Auto-generated method stub
					
				}
				public void surfaceDestroyed(SurfaceHolder holder) {
					synchronized (streamer) {
						streamer.reset();
					}
				}
			});
			
			break;
			
		}
		
		track.ssrc = new Random().nextInt();
		track.descriptor += "a=control:trackID="+id+"\r\n";
		track.streamer.setPacketizer(track.packetizer);
		track.packetizer.setRtpSocket(new RtpSocket(new byte[65536],destination,track.port));		
		track.packetizer.getRtpSocket().setSSRC(track.ssrc);
		
		trackList.put(id, track);
		
	}
	
	private int generateId() {
		Object[] keys = trackList.keySet().toArray();
		if (keys.length==0) return 0;
		Arrays.sort(keys,Collections.reverseOrder());
		return (Integer)keys[0]+1;
	}
	
}
