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

package net.majorkernelpanic.libstreaming;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import net.majorkernelpanic.librtp.AbstractPacketizer;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

/**
 *  
 *  A MediaRecorder that streams what is recorder using a packetizer
 *  specified with setPacketizer 
 * 
 */
public class MediaStream extends MediaRecorder {

	protected static final String TAG = "MediaStream";
	
	private static int id = 0;
	private LocalServerSocket lss = null;
	private LocalSocket receiver, sender = null;
	protected AbstractPacketizer packetizer = null;
	protected boolean streaming = false;
	protected String sdpDescriptor;

	// If you mode==MODE_DEFAULT the MediaStream will just act as a regular MediaRecorder
	// By default mode = MODE_STREAMING and MediaStream sends every data he receives to the packetizer
	public static final int MODE_STREAMING = 0;
	public static final int MODE_DEFAULT = 1;
	private int mode = MODE_STREAMING;
	
	public MediaStream() {
		super();
		
		receiver = new LocalSocket();
		try {
			lss = new LocalServerSocket("net.majorkernelpanic.librtp-"+id);
			receiver.connect(new LocalSocketAddress("net.majorkernelpanic.librtp-"+id));
			receiver.setReceiveBufferSize(500000);
			receiver.setSendBufferSize(500000);
			sender = lss.accept();
			sender.setReceiveBufferSize(500000);
			sender.setSendBufferSize(500000); 
		} catch (IOException e1) {
			//throw new IOException("Can't create local socket !");
		}
		id++;
		
	}

	public void setDestination(InetAddress dest, int dport) {
		this.packetizer.setDestination(dest, dport);
	}
	
	public void setMode(int mode) throws IllegalStateException {
		if (!streaming) {
			this.mode = mode;
		}
		else {
			throw new IllegalStateException("Can't call setMode() while streaming !");
		}
	}
	
	public AbstractPacketizer getPacketizer() {
		return packetizer;
	}
	
	public void prepare() throws IllegalStateException,IOException {
		if (mode==MODE_STREAMING) setOutputFile(sender.getFileDescriptor());
		super.prepare();
	}
	
	public InputStream getInputStream() throws IOException {
		return receiver.getInputStream();
	}

	public void start() throws IllegalStateException {
		try {
			super.start();
			if (mode==MODE_STREAMING) {
				packetizer.setInputStream(getInputStream());
				packetizer.start();
			}
			streaming = true;
		} catch (IOException e) {
			throw new IllegalStateException("Something happened with the local sockets :/ Start failed");
		} catch (NullPointerException e) {
			throw new IllegalStateException("setPacketizer() should be called before start(). Start failed");
		}
	}
	
	public void stop() {
		if (mode==MODE_STREAMING) packetizer.stop();
		if (streaming) {
			try {
				super.stop();
			}
			catch (IllegalStateException ignore) {}
			finally {
				super.reset();
				streaming = false;
			}
		}
	}
	
	protected void finalize() {
		try {
			sender.close();
			receiver.close();
			lss.close();
		}
		catch (IOException ignore) {}
	}
	
}
