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

import net.majorkernelpanic.spydroid.SpydroidActivity;
import android.os.SystemClock;
import android.util.Log;

/*
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
	
	private final int packetSize = 1400;
	private long oldtime = SystemClock.elapsedRealtime(), delay = 0, avdelay = 0, avnal = 0;
	private long latency, oldlat = oldtime, tleft = 0;
	private int available, naluLength, oldavailable, bleft, utype = 0;
	
	public H264Packetizer(InputStream fis, InetAddress dest, int port) throws SocketException {
		super(fis, dest, port);
	}
	
	public void run() {
		
		int sum, len = 0;
        
		try {
		
			// Skip all atoms preceding mdat atom
			while (true) {
				fis.read(buffer,rtphl,8);
				if (buffer[rtphl+4] == 'm' && buffer[rtphl+5] == 'd' && buffer[rtphl+6] == 'a' && buffer[rtphl+7] == 't') break;
				len = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
				if (len<=0) return;
				//Log.e(SpydroidActivity.LOG_TAG,"Atom skipped: "+printBuffer(rtphl+4,rtphl+8)+" size: "+len);
				fis.read(buffer,rtphl,len-8);
			} 
			
			// Some phones do not set length correctly when stream is not seekable, still we need to skip the header
			if (len<=0) {
				while (true) {
					while (fis.read() != 'm');
					fis.read(buffer,rtphl,3);
					if (buffer[rtphl] == 'd' && buffer[rtphl+1] == 'a' && buffer[rtphl+2] == 't') break;
				}
			}
		
		}
		
		catch (IOException e)  {
			return;
		}
		
		while (running) { 
		 
			// Read nal unit length (4 bytes) and nal unit header (1 byte)
			len = fill(rtphl, 5);
			naluLength = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
			
			//Log.e(SpydroidActivity.LOG_TAG,"- Nal unit length: " + naluLength);
			
			rsock.updateTimestamp(SystemClock.elapsedRealtime()*90);
			
			sum = 1;
			
			// RFC 3984, packetization mode = 1
			
			utype = buffer[rtphl+4]&0x1F;
			//Log.d(SpydroidActivity.LOG_TAG,"NAL UNIT TYPE: "+(buffer[rtphl+4]&0x1F));
			
			// Small nal unit => Single nal unit
			if (naluLength<=packetSize-rtphl-2) {
				
				buffer[rtphl] = buffer[rtphl+4];
				if (!running) break;
				len = fill(rtphl+1,  naluLength-1  );
				if (len<0) break;
				rsock.markNextPacket();
				send(naluLength+rtphl,false);
				
				//Log.e(SpydroidActivity.LOG_TAG,"----- Single NAL unit read:"+len+" header:"+printBuffer(rtphl,rtphl+3));
				
			}
			// Large nal unit => Split nal unit
			else {
			
				// Set FU-A indicator
				buffer[rtphl] = 28;
				buffer[rtphl] += (buffer[rtphl+4] & 0x60) & 0xFF; // FU indicator NRI
				//buffer[rtphl] += 0x80;
				
				// Set FU-A header
				buffer[rtphl+1] = (byte) (buffer[rtphl+4] & 0x1F);  // FU header type
				buffer[rtphl+1] += 0x80; // Start bit
				
				 
		    	while (sum < naluLength) {
		    		
		    		if (!running) break;
					len = fill( rtphl+2,  naluLength-sum > packetSize-rtphl-2 ? packetSize-rtphl-2 : naluLength-sum  ); sum += len;
					if (len<0) break;
					
					// Last packet before next nal
					if (sum >= naluLength) {
						// End bit on
						buffer[rtphl+1] += 0x40;
						rsock.markNextPacket();
					}
						
					send(len+rtphl+2,true);
					
					// Switch start bit 
					buffer[rtphl+1] = (byte) (buffer[rtphl+1] & 0x7F); 
					
					//Log.d(SpydroidActivity.LOG_TAG,"--- FU-A unit, end:"+(boolean)(sum>=naluLength));
					
		    	}
		    	
			}
			
		}
		
		Log.d(SpydroidActivity.LOG_TAG,"Thread over !!!");
		
	}
	
	private int fill(int offset,int length) {
		
		int sum = 0, len;
		long time;
		
		while (sum<length) {
			try { 
				
				available = fis.available();
				len = fis.read(buffer, offset+sum, length-sum);
				//Log.d(SpydroidActivity.LOG_TAG,"Data read: "+fis.available()+","+len);
				
				if (oldavailable<available) {
					
					bleft = available-oldavailable;
					
					time = SystemClock.elapsedRealtime();
					latency = time - oldlat;
					tleft = latency;
					oldlat = time;
					
					Log.d(SpydroidActivity.LOG_TAG,"latency: "+latency+", buffer: "+bleft);
					Log.d(SpydroidActivity.LOG_TAG,"Delay: "+delay+" available: "+fis.available()+", oldavailable: "+oldavailable);

				}
				
				oldavailable = available;
				
				if (len<0) {
					Log.e(SpydroidActivity.LOG_TAG,"Read error");
					return -1;
				}
				else sum+=len;
				
			} catch (IOException e) {
				Log.e(SpydroidActivity.LOG_TAG,"Read try failed");
				return -1;
			}
		} 
		
		return sum;
			
	}
	
	private void send(int size, boolean split) {
		
		long now = SystemClock.elapsedRealtime(), res = 0;
		
		if (rsock.isMarked()) {
			
			if (available>0) res = (tleft*naluLength)/available;
			//if (available>0) res = (tleft*naluLength)/bleft;
			if (utype != 5) {
				if (0==avdelay) avdelay = res;
				else avdelay = (99*avdelay+res)/100;
				avnal = (99*avnal+naluLength)/100;
				//if (res>0 && (avdelay<=0 || res<2*avdelay)) delay = res+1;
				if (res>0) delay = res;
			}
			
			Log.d(SpydroidActivity.LOG_TAG,"a: "+available+", av: "+avnal+", nal: "+naluLength+", t: "+tleft+", aver: "+avdelay+", del: "+delay+", res: "+res+", r2: "+(tleft*naluLength)/bleft);
			
			tleft -= delay;
			bleft -= naluLength;

			if (now-oldtime<delay)
				try {
					Thread.sleep(delay-(now-oldtime));
				} catch (InterruptedException e) {}
				
		}
		
		oldtime = SystemClock.elapsedRealtime();
		rsock.send(size);
		
	}

}
