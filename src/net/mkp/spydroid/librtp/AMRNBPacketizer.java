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

package net.mkp.spydroid.librtp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;

import net.mkp.spydroid.SpydroidActivity;
import android.util.Log;

/*
 *   RFC 3267
 *   
 *   AMR Streaming over RTP
 *   
 *   
 *   Must be fed with an InputStream containing raw amr nb
 *   Stream must begin with 6 bytes long header: "#!AMR.", it will be skipped
 *   
 */

public class AMRNBPacketizer extends AbstractPacketizer {
	
	private long ts = 0;
	
	private final int amrhl = 6; // Header length
	private final int amrps = 32;   // Packet size
	
	public AMRNBPacketizer(InputStream fis, InetAddress dest) throws SocketException {
		super(fis, dest, 5004);
	}

	public void run() {
	
		// Skip raw amr header
		fill(rtphl,amrhl);
		
		buffer[rtphl] = (byte) 0xF0;
		rsock.markAllPackets();
		
		while (running) {
			
			fill(rtphl+1,amrps);
			rsock.updateTimestamp(ts); ts+=160;
			rsock.send(rtphl+amrps+1);
			
		}
		
	}

	
	private int fill(int offset,int length) {
		
		int sum = 0, len;
		
		while (sum<length) {
			try { 
				len = fis.read(buffer, offset+sum, length-sum);
				if (len<0) {
					Log.e(SpydroidActivity.LOG_TAG,"Read error");
				}
				else sum+=len;
			} catch (IOException e) {
				stopStreaming();
				return sum;
			}
		}
		
		return sum;
			
	}
	
	
}
