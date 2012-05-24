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
		public Track(MediaStream stream, boolean enabled) {
			this.enabled = enabled;
			this.stream = stream;
		}
		MediaStream stream;
		boolean enabled = true;
	}
	
	public StreamingManager(Context context) {
		this.context = context;
	}
	
	// Contains a list of all the tracks that has been added
	private HashMap<Integer,Track> trackList = new HashMap<Integer,Track>();
	
	/** 
	 * Starts a new session: all previous tracks are removed from the current session, 
	 * you can then call "add" methods to add some tracks to the new session
	 */
	public void startNewSession() {
		Iterator<Track> it = trackList.values().iterator();
		while (it.hasNext()) {
			it.next().enabled = false;
		}
	}
	
	public void addH264Track(int videoSource, int destinationPort, VideoQuality videoQuality, SurfaceHolder surfaceHolder) throws IllegalStateException, IOException {
		
		Track track = findTrackByClass(H264Stream.class);
		H264Stream stream;
		if (track==null) {
			stream = new H264Stream(context,0);
			trackList.put(generateId(), new Track(stream,true));
		}
		else {
			stream = (H264Stream) track.stream;
			track.enabled = true;
		}
		stream.setDestination(destination, destinationPort);
		stream.setVideoQuality(videoQuality);
		stream.setPreviewDisplay(surfaceHolder);
		
	}
	
	public void addAMRNBTrack(int destinationPort) {
		
		Track track = findTrackByClass(AMRNBStream.class);
		AMRNBStream stream;
		if (track==null) {
			stream = new AMRNBStream();
			trackList.put(generateId(), new Track(stream,true));
		}
		else {
			stream = (AMRNBStream) track.stream;
			track.enabled = true;
		}
		stream.setDestination(destination, destinationPort);
		
	}
	
	public void setFlashState(boolean state) {
		Iterator<Track> it = trackList.values().iterator();
		while (it.hasNext()) {
			MediaStream stream = it.next().stream;
			if (stream.getClass().equals(H264Stream.class)) {
				((H264Stream)stream).setFlashState(state);
			}
		}
	}
	
	public int getTrackPort(int trackId) {
		return trackList.get(trackId).stream.getDestinationPort();
	}
	
	public int getTrackSSRC(int trackId) {
		return trackList.get(trackId).stream.getPacketizer().getRtpSocket().getSSRC();
	}
	
	/** Return a session descriptor that can be stored in a file or sent to a client with RTSP */
	public String getSessionDescriptor() throws IllegalStateException, IOException {
		String sdp = "";
		Track t;
		int i;
		Iterator<Integer> it = trackList.keySet().iterator();
		while (it.hasNext()) {
			i = it.next(); t = trackList.get(i);
			if (t.enabled) {
				sdp += t.stream.generateSdpDescriptor();
				sdp += "a=control:trackID="+i+"\r\n";
			}
		}
		return sdp;
	}
	
	/** Delete all existing tracks & release associated resources */
	public synchronized void flush() {
		Iterator<Track> it = trackList.values().iterator();
		while (it.hasNext()) {
			MediaStream stream = it.next().stream;
			stream.stop();
			stream.release();
		}
		trackList.clear();
	}
	
	public boolean trackExists(int trackId) {
		return trackList.containsKey(trackId);
	}
	
	public void setDestination(InetAddress destination) {
		this.destination =  destination;
	}
	
	/** Start all streams of the session */
	public void startAll() throws RuntimeException, IllegalStateException, IOException {
		Iterator<Track> it = trackList.values().iterator();
		Track t;
		while (it.hasNext()) {
			t = it.next();
			if (t.enabled && !t.stream.isStreaming()) {
				t.stream.prepare();
				t.stream.start();
			}
		}
		
	}
	
	/** Stop existing streams */
	public void stopAll() {
		Iterator<Track> it = trackList.values().iterator();
		while (it.hasNext()) {
			MediaStream stream = it.next().stream;
			stream.stop();
		}
	}
	
	private Track findTrackByClass(Class<?> aClass) {
		Iterator<Track> it = trackList.values().iterator();
		while (it.hasNext()) {
			Track t = it.next();
			MediaStream stream = t.stream;
			if (stream.getClass().equals(aClass)) {
				return t;
			}
		}
		return null;
	}
	
	private int generateId() {
		Object[] keys = trackList.keySet().toArray();
		if (keys.length==0) return 0;
		Arrays.sort(keys,Collections.reverseOrder());
		return (Integer)keys[0]+1;
	}
	
}
