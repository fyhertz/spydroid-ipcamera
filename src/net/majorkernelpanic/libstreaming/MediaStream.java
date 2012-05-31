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
import android.util.Log;

/**
 *  A MediaRecorder that streams what it records using a packetizer
 *  specified with setPacketizer 
 *  Use it just like a regular MediaRecorder except for setOutputFile()
 */
public abstract class MediaStream extends MediaRecorder implements Stream {

	protected static final String TAG = "MediaStream";
	
	private static int id = 0;
	private int socketId;
	private LocalServerSocket lss = null;
	private LocalSocket receiver, sender = null;
	protected AbstractPacketizer packetizer = null;
	protected boolean streaming = false;
	protected String sdpDescriptor;

	// If you mode==MODE_DEFAULT the MediaStream will just act as a regular MediaRecorder
	// By default mode = MODE_STREAMING and MediaStream sends every data he receives to the packetizer
	public static final int MODE_STREAMING = 0;
	public static final int MODE_DEFAULT = 1;
	protected int mode = MODE_STREAMING;
	
	public MediaStream() {
		super();
		
		try {
			lss = new LocalServerSocket("net.majorkernelpanic.librtp-"+id);
		} catch (IOException e1) {
			//throw new IOException("Can't create local socket !");
		}
		socketId = id;
		id++;
		
	}

	public void setDestination(InetAddress dest, int dport) {
		this.packetizer.setDestination(dest, dport);
	}
	
	public int getDestinationPort() {
		return this.packetizer.getRtpSocket().getPort();
	}
	
	public int getLocalPort() {
		return this.packetizer.getRtpSocket().getLocalPort();
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
	
	public boolean isStreaming() {
		return streaming;
	}
	
	public void prepare() throws IllegalStateException,IOException {
		if (mode==MODE_STREAMING) {
			createSockets();
			setOutputFile(sender.getFileDescriptor());
		}
		super.prepare();
	}
	
	public void start() throws IllegalStateException {
		try {
			super.start();
			if (mode==MODE_STREAMING) {
				packetizer.setInputStream(receiver.getInputStream());
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
			catch (RuntimeException ignore) {}
			finally {
				super.reset();
				streaming = false;
				closeSockets();
			}
		}
	}
	
	public int getSSRC() {
		return getPacketizer().getRtpSocket().getSSRC();
	}
	
	public abstract String generateSdpDescriptor()  throws IllegalStateException, IOException;
	
	private void createSockets() throws IOException {
		receiver = new LocalSocket();
		receiver.connect( new LocalSocketAddress("net.majorkernelpanic.librtp-" + socketId ) );
		receiver.setReceiveBufferSize(500000);
		receiver.setSendBufferSize(500000);
		sender = lss.accept();
		sender.setReceiveBufferSize(500000);
		sender.setSendBufferSize(500000); 
	}
	
	private void closeSockets() {
		try {
			sender.close();
			receiver.close();
		} catch (IOException ignore) {}
	}
	
	public void release() {
		stop();
		try {
			lss.close();
		}
		catch (IOException ignore) {}
		super.release();
	}
	
}
