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

import net.majorkernelpanic.rtp.AMRNBPacketizer;
import net.majorkernelpanic.streaming.MediaStream;
import android.media.MediaRecorder;

/**
 * This will stream AMRNB from the mic over RTP
 * Just call setDestination(), prepare() & start()
 */
public class AMRNBStream extends MediaStream {

	public AMRNBStream() {
		super();
		
		this.packetizer = new AMRNBPacketizer();
		
	}

	public void prepare() throws IllegalStateException, IOException {
		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		setAudioChannels(1);
		super.prepare();
	}
	
	public String generateSessionDescriptor() {
		return "m=audio "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				   "b=AS:128\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 AMR/8000\r\n" +
				   "a=fmtp:96 octet-align=1;\r\n";
	}
	
}
