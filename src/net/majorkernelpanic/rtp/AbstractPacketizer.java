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

package net.majorkernelpanic.rtp;

import java.io.InputStream;
import java.net.InetAddress;

/**
 * 
 * Each packetizer inherits from this one and therefore uses RTP and UDP
 *
 */
abstract public class AbstractPacketizer {
	
	protected static final int rtphl = RtpSocket.RTP_HEADER_LENGTH;
	
	protected RtpSocket socket = null;
	protected InputStream is = null;
	protected boolean running = false;
	protected byte[] buffer;
	
	public AbstractPacketizer() {
		socket = new RtpSocket();
		buffer = socket.getBuffer();
	}	
	
	public AbstractPacketizer(InputStream fis) {
		super();
		this.is = fis;
	}
	
	public RtpSocket getRtpSocket() {
		return socket;
	}
	
	public void setInputStream(InputStream fis) {
		this.is = fis;
	}
	
	public void setDestination(InetAddress dest, int dport) {
		socket.setDestination(dest, dport);
	}
	
	public abstract void stop();
	public abstract void start();
	
    // Useful for debug
    protected String printBuffer(int start,int end) {
    	String str = "";
    	for (int i=start;i<end;i++) str+=","+Integer.toHexString(buffer[i]&0xFF);
    	return str;
    }
	
}
