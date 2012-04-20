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

/**
 * 
 *   RFC 3984
 *   
 *   H264 Streaming over RTP
 *   
 *   Must be fed with an InputStream containing raw h.264
 *   NAL units must be preceded by their length (4 bytes)
 *   Stream must start with mpeg4 or 3gpp header, it will be skipped
 *   
 */
public class H264Packetizer extends AbstractPacketizer {
	
	private final static String TAG = "H264Packetizer";
	
	private final int packetSize = 1400;
	private long time, delay, cts;
	private int oldavailable = 0, available, len = 0, delta;
	private boolean skip = true;
	
	public H264Packetizer() {
		super();
	}
	
	public void run() {
		
		/*
		 * Here we just skip the mpeg4 header
		 */
		try {
			
			// Skip all atoms preceding mdat atom
			while (true) {
				fis.read(buffer,rtphl,8);
				if (buffer[rtphl+4] == 'm' && buffer[rtphl+5] == 'd' && buffer[rtphl+6] == 'a' && buffer[rtphl+7] == 't') break;
				len = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
				if (len<=7) break;
				//Log.e(SpydroidActivity.LOG_TAG,"Atom skipped: "+printBuffer(rtphl+4,rtphl+8)+" size: "+len);
				fis.read(buffer,rtphl,len-8);
			}
			
			// Some phones do not set length correctly when stream is not seekable, still we need to skip the header
			if (len<=0 || len>1000) {
				while (true) {
					while (fis.read() != 'm');
					fis.read(buffer,rtphl,3);
					if (buffer[rtphl] == 'd' && buffer[rtphl+1] == 'a' && buffer[rtphl+2] == 't') break;
				}
			}
			len = 0;
		}
		catch (IOException e)  {
			return;
		}

		cts = SystemClock.elapsedRealtime();
		
		try {
			
			delta = 1; 
			oldavailable = fis.available();
			time = SystemClock.elapsedRealtime();
			
			while (running) { 
	
				send();
				
				available = fis.available();
				
				Log.d(TAG,"available: "+available);
				
				if (available - oldavailable>0 || available==0) {
					
					skip = false;
					delay = SystemClock.elapsedRealtime() - time;
					delta = available - oldavailable;
					time = SystemClock.elapsedRealtime();

					Log.e("TAG","delay: "+delay+" delta: "+delta);
					
				}	
				
				oldavailable = available;
				
			}
		
		} catch (IOException e) {
			return;
		}
		
		
	}
	
	
	
	/*
	 * Reads a NAL unit and sends it
	 * If it is too big, we split it in FU-A units (RFC 3984)
	 */
	private void send() {
		
		int sum = 1, len = 0, naluLength;
		
		
		/* Read nal unit length (4 bytes) and nal unit header (1 byte) */
		if ( ( len = fill(rtphl, 5) ) < 0 ) {running = false; return;}
		naluLength = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
		
		//Log.d("SPYDROID","- Nal unit length: " + naluLength+ " deltaTS: "+(delay*naluLength/delta));
		
		if (delta==0) delta = naluLength;
		cts += delay*naluLength/delta;
		
		try {
			Thread.sleep(4*delay*naluLength/delta/5);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 
		}
		
		rsock.updateTimestamp(cts*90);
		
		/* Small nal unit => Single nal unit */
		if (naluLength<=packetSize-rtphl-2) {
			
			buffer[rtphl] = buffer[rtphl+4];
			len = fill(rtphl+1, naluLength-1);
			rsock.markNextPacket();
			if (!skip) rsock.send(naluLength+rtphl);
			
			//Log.e(SpydroidActivity.LOG_TAG,"----- Single NAL unit read:"+len+" header:"+printBuffer(rtphl,rtphl+3));
			
		}
		
		/* Large nal unit => Split nal unit */
		else {
		
			/* Set FU-A indicator */
			buffer[rtphl] = 28;
			buffer[rtphl] += (buffer[rtphl+4] & 0x60) & 0xFF; // FU indicator NRI
			
			/* Set FU-A header */
			buffer[rtphl+1] = (byte) (buffer[rtphl+4] & 0x1F);  // FU header type
			buffer[rtphl+1] += 0x80; // Start bit
			
			 
	    	while (sum < naluLength) {
	    		
				len = fill(rtphl+2, naluLength-sum > packetSize-rtphl-2 ? packetSize-rtphl-2 : naluLength-sum ); sum += len;
				if (len<0) {running = false; return;}
				
				/* Last packet before next NAL */
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl+1] += 0x40;
					rsock.markNextPacket();
				}
					
				if (!skip) rsock.send(len+rtphl+2);
				
				/* Switch start bit */
				buffer[rtphl+1] = (byte) (buffer[rtphl+1] & 0x7F); 
				
				//Log.d(SpydroidActivity.LOG_TAG,"--- FU-A unit, end:"+(boolean)(sum>=naluLength));
				
	    	}
	    	
		}
		
		//Log.i(SpydroidActivity.LOG_TAG,"NAL UNIT SENT "+nbNalu);
		
	}

	private int fill(int offset, int length) {
		
		int sum = 0, len = 0;
		
		try {
			while (sum<length) {
				len = fis.read(buffer,offset+sum,length-sum);
				sum += len;
				if (len==-1) return -1;
			}
		} catch (IOException e) {
			return -1;
		}
		
		return sum;
		
	}
	
}