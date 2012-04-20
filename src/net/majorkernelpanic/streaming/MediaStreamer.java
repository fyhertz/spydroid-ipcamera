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

package net.majorkernelpanic.streaming;

import java.io.IOException;
import java.io.InputStream;
import net.majorkernelpanic.librtp.AbstractPacketizer;
import net.majorkernelpanic.spydroid.SpydroidActivity;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

/**
 *  
 *  A MediaRecorder that streams what is recorder using a packetizer
 *  specified with setPacketizer 
 * 
 */
public class MediaStreamer extends MediaRecorder {

	private static int id = 0;
	
	private LocalServerSocket lss = null;
	private LocalSocket receiver, sender = null;
	private AbstractPacketizer packetizer = null;
	
	public MediaStreamer() {
		
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
	
	public void setPacketizer(AbstractPacketizer packetizer) {
		this.packetizer = packetizer;
	}

	public AbstractPacketizer getPacketizer() {
		return packetizer;
	}
	
	public void prepare() throws IllegalStateException,IOException {
		
		setOutputFile(sender.getFileDescriptor());
		
		try {
			super.prepare();
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
		
	}
	
	public InputStream getInputStream() {
		
		InputStream in = null;
		
		try {
			in = receiver.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return in;
		
	}

	public void start() {
		super.start();
		packetizer.setInputStream(getInputStream());
		packetizer.start();
	}
	
	public void stop() {
		packetizer.stop();
		super.stop();
	}
	
	protected void finalize() {
		try {
			sender.close();
			receiver.close();
			lss.close();
		}
		catch (IOException e) {
			Log.e(SpydroidActivity.TAG,"Error while attempting to close local sockets");
		}
	}
	
}
