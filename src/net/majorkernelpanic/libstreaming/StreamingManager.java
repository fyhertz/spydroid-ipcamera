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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import net.majorkernelpanic.libmp4.MP4Config;
import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * 
 */
public class StreamingManager {

	public final static String TAG = "StreamingManager";
	 
	private final Context context;
	private InetAddress destination;
	
	private class Track {
		public String descriptor;
		public MediaStream stream;
	}
	
	public StreamingManager(Context context) {
		this.context = context;
	}
	
	// Contains a list of all the tracks that has been added
	private HashMap<Integer,Track> trackList = new HashMap<Integer,Track>();
	
	public void addH264Track(int videoSource, int destinationPort, VideoQuality videoQuality, SurfaceHolder surfaceHolder) throws IllegalStateException, IOException {
		
		H264Stream stream = new H264Stream(context);
		stream.setDestination(destination, destinationPort);
		stream.setVideoQuality(videoQuality);
		stream.setPreviewDisplay(surfaceHolder);
		stream.testH264();
		
		Track track = new Track();
		track.stream = stream;
		
		MP4Config config = stream.getMP4Config();
		track.descriptor = "m=video "+String.valueOf(destinationPort)+" RTP/AVP 96\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 H264/90000\r\n" +
				   "a=fmtp:96 packetization-mode=1;profile-level-id="+config.getProfileLevel()+";sprop-parameter-sets="+config.getB64SPS()+","+config.getB64PPS()+";\r\n";
		
		trackList.put(generateId(), track);
		
	}
	
	public void addAMRNBTrack(int destinationPort) {
		
		AMRNBStream stream = new AMRNBStream();
		stream.setDestination(destination, destinationPort);
		
		Track track = new Track();
		track.stream = new AMRNBStream();
		track.descriptor = "m=audio "+String.valueOf(destinationPort)+" RTP/AVP 96\r\n" +
				   "b=AS:128\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 AMR/8000\r\n" +
				   "a=fmtp:96 octet-align=1;\r\n";
		
		trackList.put(generateId(), track);
		
	}
	

	public int getTrackPort(int trackId) {
		return trackList.get(trackId).stream.getPacketizer().getRtpSocket().getLocalPort();
	}
	
	public int getTrackSSRC(int trackId) {
		return trackList.get(trackId).stream.getPacketizer().getRtpSocket().getSSRC();
	}
	
	public String getTrackDescriptor(int trackId) {
		return trackList.get(trackId).descriptor;
	}
	
	public String getSessionDescriptor() {
		String sdp = "";
		Track t;
		int i;
		Iterator<Integer> it = trackList.keySet().iterator();
		while (it.hasNext()) {
			i = it.next(); t = trackList.get(i);
			sdp += t.descriptor;
			sdp += "a=control:trackID="+i;
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

	public void prepareAll() throws IllegalStateException, IOException {
		Iterator<Track> it = trackList.values().iterator();
		
		// Let's prepare all MediaStreamers
		while (it.hasNext()) {
			MediaStream stream = it.next().stream;
			stream.prepare();
		}
		
	}
	
	public void startAll() throws RuntimeException, IllegalStateException {
		Iterator<Track> it = trackList.values().iterator();
		
		// Let's start all MediaStreamers
		while (it.hasNext()) {
			MediaStream stream = it.next().stream;
			stream.start();
		}
		
	}
	
	public void stopAll() {
		Iterator<Track> it = trackList.values().iterator();
		
		// Let's stop all MediaStreamers
		while (it.hasNext()) {
			MediaStream stream = it.next().stream;
			stream.stop();
		}
		
	}
	
	private int generateId() {
		Object[] keys = trackList.keySet().toArray();
		if (keys.length==0) return 0;
		Arrays.sort(keys,Collections.reverseOrder());
		return (Integer)keys[0]+1;
	}
	
}
