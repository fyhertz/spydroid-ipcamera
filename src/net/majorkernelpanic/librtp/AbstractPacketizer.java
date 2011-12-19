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

package net.majorkernelpanic.librtp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;


abstract public class AbstractPacketizer extends Thread implements Runnable{
	
	protected SmallRtpSocket rsock = null;
	protected InputStream fis = null;
	protected boolean running = false;
	
	protected byte[] buffer = new byte[16384];	
	
	protected final int rtphl = 12; // Rtp header length
	
	
	public AbstractPacketizer(InputStream fis, InetAddress dest, int port) throws SocketException {
		
		this.fis = fis;
		this.rsock = new SmallRtpSocket(dest, port, buffer);
	}
	
	public void startStreaming() {
		running = true;
		start();
	}

	public void stopStreaming() {
		try {
			fis.close();
		} catch (IOException e) {
			
		}
		running = false;
	}
	
	abstract public void run();
	
    // Useful for debug
    protected String printBuffer(int start,int end) {
            String str = "";
            for (int i=start;i<end;i++) str+=","+Integer.toHexString(buffer[i]&0xFF);
            return str;
    }
	
}
