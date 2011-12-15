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

package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.io.InputStream;

import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

/* 
 *  Just a MediaRecorder that writes in a local socket instead of a file
 *  so that you can modify data on-the-fly with getInputStream()
 * 
 */

public class MediaStreamer extends MediaRecorder{

	private static int id = 0;
	
	private LocalServerSocket lss = null;
	private LocalSocket receiver, sender = null;
	
	public void prepare() throws IllegalStateException,IOException {
		
		receiver = new LocalSocket();
		try {
			lss = new LocalServerSocket("librtp-"+id);
			receiver.connect(new LocalSocketAddress("librtp-"+id));
			receiver.setReceiveBufferSize(500000);
			receiver.setSendBufferSize(500000);
			sender = lss.accept();
			sender.setReceiveBufferSize(500000);
			sender.setSendBufferSize(500000); 
			id++;
		} catch (IOException e1) {
			throw new IOException("Can't create local socket !");
		}
		
		setOutputFile(sender.getFileDescriptor());
		
		try {
			super.prepare();
		} catch (IllegalStateException e) {
			closeSockets();
			throw e;
		} catch (IOException e) {
			closeSockets();
			throw e;
		}
		
	}
	
	public InputStream getInputStream() {
		
		InputStream out = null;
		
		try {
			out = receiver.getInputStream();
		} catch (IOException e) {
		}

		return out;
		
	}

	
	public void stop() {
		closeSockets();
		super.stop();
	}
	
	private void closeSockets() {
		try {
			lss.close();
			sender.close();
			receiver.close();
		}
		catch (IOException e) {
			Log.e(SpydroidActivity.LOG_TAG,"Error while attempting to close local sockets");
		}
		lss = null; sender = null; receiver = null;
	}
	
}
