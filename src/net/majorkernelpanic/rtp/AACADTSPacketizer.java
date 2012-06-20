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

import java.io.IOException;

import android.util.Log;

/**
 * RFC 3640  
 *
 * This packetizer must be fed with an InputStream containing ADTS AAC. 
 * AAC will basically be rewrapped in an RTP stream and sent over the network.
 * This packetizer only implements the aac-hbr mode (High Bit-rate AAC) and
 * each packet only carry a single and complete AAC access unit.
 * 
 */
public class AACADTSPacketizer extends AbstractPacketizer implements Runnable {

	private final static String TAG = "AACADTSPacketizer";
	
	public AACADTSPacketizer() {
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
		
		// Adts header fields that we need to parse
		boolean protection;
		int frameLength, ts=0;

		try {
			while (running) {

				// Parse adts header (ADTS packets start with a 7 or 9 byte long header)
				is.read(buffer,rtphl,7);
				// The protection bit indicates whether or not the header contains the two extra bytes
				protection = (buffer[rtphl+1]&0x01)>0 ? true : false;
				frameLength = (buffer[rtphl+3]&0x03) << 11 | 
							  (buffer[rtphl+4]&0xFF) << 3 | 
							  (buffer[rtphl+5]&0xFF) >> 5 ;
				frameLength -= (protection ? 7 : 9);
				
				//Log.d(TAG,"frameLength: "+frameLength+" protection: "+protection);
				
				// Read CRS if any
				if (!protection) is.read(buffer,rtphl,2);
				
				// Read frame
				is.read(buffer,rtphl+4,frameLength);
				
				// AU-headers-length field: contains the size in bits of a AU-header
				buffer[rtphl] = 0;
				buffer[rtphl+1] = 0x10; // 13+3 = 16 bits -> 13bits for AU-size and 3bits for AU-Index / AU-Index-delta 
				
				// AU-size
				buffer[rtphl+2] = (byte) (frameLength>>5);
				buffer[rtphl+3] = (byte) (frameLength<<3);
				
				// AU-Index
				buffer[rtphl+3] &= 0xF8;
				buffer[rtphl+3] |= 0x00;
				
				socket.markNextPacket();
				ts += 1024;
				socket.updateTimestamp(ts);
				socket.send(rtphl+frameLength+4);

			}
		} catch (IOException e) {
			Log.e(TAG,"IOException: "+e.getMessage());
			e.printStackTrace();
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG,"ArrayIndexOutOfBoundsException: "+e.getMessage());
			e.printStackTrace();
		} finally {
			running = false;
		}
		
	}

}
