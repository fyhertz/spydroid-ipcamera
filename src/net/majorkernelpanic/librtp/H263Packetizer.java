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

import android.os.SystemClock;
import android.util.Log;

public class H263Packetizer extends AbstractPacketizer implements Runnable {

	public final static String TAG = "H263Packetizer";
	
	private final static int MAXPACKETSIZE = 1400;
	
	public H263Packetizer() {
		super();
	}
	
	public void start() {
		if (!running) {
			running = true;
			new Thread(this).start();
		}
	}

	public void stop() {
		running = false;
	}

	public void run() {
		long time, duration = 0, ts = 0;
		int i = 0, j = 0;
		boolean start = true;
		
		try {
			skipHeader();
		} catch (IOException e) {
			Log.e(TAG,"Couldn't skip mp4 header :/");
			return;
		}
		
		// Two byte header
		buffer[rtphl] = 0;
		buffer[rtphl+1] = 0;
		
		while (running) {
			time = SystemClock.elapsedRealtime();
			if (fill(rtphl+j+2,MAXPACKETSIZE-rtphl-j-2)<0) return;
			duration += SystemClock.elapsedRealtime() - time;
			//Log.d(TAG,"available: "+is.available()+" duration: "+duration);
			j = 0;
			for (i=rtphl+2;i<MAXPACKETSIZE-1;i++) {
				if (buffer[i]==0 && buffer[i+1]==0) {
					j=i;
					break;
				}
			}
			buffer[rtphl] = 0;
			if (j>0) {
				// We have found the end of the frame
				//Log.d(TAG,"---> End of frame");
				ts+= duration; duration = 0;
				socket.updateTimestamp(ts*90);
				socket.markNextPacket();
				socket.send(j);
				System.arraycopy(buffer,j+2,buffer,rtphl+2,MAXPACKETSIZE-j-2); 
				j = MAXPACKETSIZE-j-2;
				start = true;
			} else {
				// This packet only contains a fragment of frame
				if (start) {
					buffer[rtphl] = 4;
					start = false;
				}
				socket.send(MAXPACKETSIZE);
			}
		}
		
		Log.d(TAG,"Packetizer stopped !");
			
	}

	private int fill(int offset,int length) {
		
		int sum = 0, len;
		
		while (sum<length) {
			try { 
				len = is.read(buffer, offset+sum, length-sum);
				if (len<0) {
					Log.e(TAG,"End of stream");
					return -1;
				}
				else sum+=len;
			} catch (IOException e) {
				stop();
				return sum;
			}
		}
		
		return sum;
			
	}
	
	// The InputStream may start with a header that we need to skip
	private void skipHeader() throws IOException {

		int len = 0;
		
		// Skip all atoms preceding mdat atom
		while (true) {
			is.read(buffer,rtphl,8);
			if (buffer[rtphl+4] == 'm' && buffer[rtphl+5] == 'd' && buffer[rtphl+6] == 'a' && buffer[rtphl+7] == 't') break;
			len = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
			if (len<8 || len>1000) {
				Log.e(TAG,"Malformed header :/ len: "+len+" available: "+is.available());
				break;
			}
			Log.d(TAG,"Atom skipped: "+printBuffer(rtphl+4,rtphl+8)+" size: "+len);
			is.read(buffer,rtphl,len-8);
		}
		
		// Some phones do not set length correctly when stream is not seekable, still we need to skip the header
		if (len<=0 || len>1000) {
			while (true) {
				while (is.read() != 'm');
				is.read(buffer,rtphl,3);
				if (buffer[rtphl] == 'd' && buffer[rtphl+1] == 'a' && buffer[rtphl+2] == 't') break;
			}
		}
		len = 0;
		
	}
	
}
