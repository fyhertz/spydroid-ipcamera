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

package net.mkp.librtp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;


public class SessionDescriptor {

	private ArrayList<String> tracks = new ArrayList<String>();
	
	public SessionDescriptor() {
		
	}
	
	public void addH264Track(String profile, String b64sps, String b64pps) {
		
		tracks.add(	"m=video 5006 RTP/AVP 96\r\n" +
					"b=RR:0\r\n" +
					"a=rtpmap:96 H264/90000\r\n" +
					"a=fmtp:96 packetization-mode=1;profile-level-id="+profile+";sprop-parameter-sets="+b64sps+","+b64pps+";\r\n" );
		
	}
	
	public void addAMRNBTrack() {
		
		tracks.add(	"m=audio 5004 RTP/AVP 96\r\n" +
					"b=AS:128\r\n" +
					"b=RR:0\r\n" +
					"a=rtpmap:96 AMR/8000\r\n" +
					"a=fmtp:96 octet-align=1\r\n" );
		
	}
	
	
	/* Generate SDP file */
	public void saveToFile(String path) throws IOException {
		File file = new File(path);
		RandomAccessFile raf = null;
		raf = new RandomAccessFile(file, "rw");
		raf.writeBytes("v=0\r\ns=Unnamed\r\n");
		raf.writeBytes(this.toString());
		raf.close();
	}
	
	
	public Iterator<String> getTrackList() {
		return tracks.iterator();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = tracks.iterator();
		while(it.hasNext()) {
			sb.append(it.next());
		}
		return sb.toString();
	}
	
}
