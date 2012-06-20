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

package net.majorkernelpanic.streaming.video;

import java.io.IOException;

import net.majorkernelpanic.rtp.H263Packetizer;
import net.majorkernelpanic.rtp.H264Packetizer;
import net.majorkernelpanic.streaming.MediaStream;
import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;

public class H263Stream extends VideoStream {
	
	public H263Stream(int cameraId) {
		super(cameraId);
		setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		this.packetizer = new H263Packetizer();
	}
	
	public String generateSessionDescriptor() throws IllegalStateException,
			IOException {

		return "m=video "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 H263-1998/90000\r\n";
		
	}

}
