/*
 * Copyright (C) 2011-2012 GUIGUI Simon, fyhertz@gmail.com
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

package net.majorkernelpanic.streaming.audio;

import java.io.IOException;

import net.majorkernelpanic.rtp.AACADTSPacketizer;
import net.majorkernelpanic.streaming.MediaStream;
import android.media.MediaRecorder;

/**
 * This will stream AMRNB from the mic over RTP
 * Just call setDestination(), prepare() & start()
 */
public class AACStream extends MediaStream {

	public AACStream() {
		super();
		
		AACADTSPacketizer packetizer = new AACADTSPacketizer(); 
		this.packetizer = packetizer;
		
	}

	public void prepare() throws IllegalStateException, IOException {
		
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		
		// This is completely experimental: AAC_ADTS is not yet visible in the android developer documentation
		// Recording AAC ADTS works on my galaxy SII with this tiny trick
		setOutputFormat(6);
		setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		setAudioChannels(1);
		setAudioSamplingRate(8000);
		
		super.prepare();
	}
	
	/* streamtype ?
	 * profile-level-id ?
	 * config ?
	 * Profile ?
	 */
	
	public String generateSessionDescriptor() {
		return "m=audio "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				"b=RR:0\r\n" +
				"a=rtpmap:96 mpeg4-generic/8000\r\n" +
				"a=fmtp:96 streamtype=5; profile-level-id=15; mode=AAC-hbr; config=1588; SizeLength=13; IndexLength=3; IndexDeltaLength=3; Profile=1;\r\n";
	}
	
}
