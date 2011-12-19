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

public class H264Packetizer2 extends AbstractPacketizer {
	
	private final int packetSize = 1400;
	private long oldtime = SystemClock.elapsedRealtime(), delay = 10;
	private long latency, oldlat = oldtime;
	private int available, naluLength = 0, nbNalu = 0, len = 0;
	private SimpleFifo fifo = new SimpleFifo(500000);
	
	public H264Packetizer2(InputStream fis, InetAddress dest, int port) throws SocketException {
		super(fis, dest, port);
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
			len = 0;
		}
		catch (IOException e)  {
			return;
		}


		while (running) { 
		 
			/* If there are NAL units in the FIFO ready to be sent, we send one */
			send();
			
			/* 
			 * If the camera has delivered new NAL units we copy them in the FIFO
			 * Then, the delay between two send call is latency/nbNalu with: 
			 * latency: how long it took to the camera to output new data
			 * nbNalu: number of NAL units in the FIFO
			 */
			fillFifo();
			
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				return;
			}
			
		}
		
		
	}
	
	/*
	 * Read a NAL unit in the FIFO and send it
	 * If it is too big, we split it in FU-A units (RFC 3984)
	 */
	private void send() {
		
		int sum = 1, len = 0, naluLength;
		
		if (nbNalu == 0) return; 
		
		/* Read nal unit length (4 bytes) and nal unit header (1 byte) */
		len = fifo.read(buffer, rtphl, 5);
		naluLength = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
		
		//Log.d(SpydroidActivity.LOG_TAG,"- Nal unit length: " + naluLength);
		
		rsock.updateTimestamp(SystemClock.elapsedRealtime()*90);
		
		/* Small nal unit => Single nal unit */
		if (naluLength<=packetSize-rtphl-2) {
			
			buffer[rtphl] = buffer[rtphl+4];
			len = fifo.read(buffer, rtphl+1,  naluLength-1  );
			rsock.markNextPacket();
			rsock.send(naluLength+rtphl);
			
			//Log.e(SpydroidActivity.LOG_TAG,"----- Single NAL unit read:"+len+" header:"+printBuffer(rtphl,rtphl+3));
			
		}
		
		/* Large nal unit => Split nal unit */
		else {
		
			/* Set FU-A indicator */
			buffer[rtphl] = 28;
			buffer[rtphl] += (buffer[rtphl+4] & 0x60) & 0xFF; // FU indicator NRI
			//buffer[rtphl] += 0x80;
			
			/* Set FU-A header */
			buffer[rtphl+1] = (byte) (buffer[rtphl+4] & 0x1F);  // FU header type
			buffer[rtphl+1] += 0x80; // Start bit
			
			 
	    	while (sum < naluLength) {
	    		
	    		if (!running) break;
				len = fifo.read(buffer, rtphl+2,  naluLength-sum > packetSize-rtphl-2 ? packetSize-rtphl-2 : naluLength-sum  ); sum += len;
				if (len<0) break;
				
				/* Last packet before next nal */
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl+1] += 0x40;
					rsock.markNextPacket();
				}
					
				rsock.send(len+rtphl+2);
				
				/* Switch start bit */
				buffer[rtphl+1] = (byte) (buffer[rtphl+1] & 0x7F); 
				
				//Log.d(SpydroidActivity.LOG_TAG,"--- FU-A unit, end:"+(boolean)(sum>=naluLength));
				
	    	}
	    	
		}
		
		nbNalu--;
		
		Log.i(SpydroidActivity.LOG_TAG,"NAL UNIT SENT "+nbNalu);
		
	}
	
	private void fillFifo() {
		
		try {
		
			if (fis.available()>4) {
				nbNalu = naluLength-len == 0 ? nbNalu : nbNalu+1;
			}
			else return;
			
			while ((available = fis.available()) >= 4) {
					
				fis.read(buffer,rtphl,naluLength-len);
				fifo.write(buffer, rtphl, naluLength-len);
				
				/* Read nal unit and copy it in the fifo */
				len = fis.read(buffer, rtphl, 4);
				naluLength = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
				len = fis.read(buffer, rtphl+4, naluLength);
				fifo.write(buffer, rtphl, len+4);
				
				if (len==naluLength) nbNalu++;
						
				//Log.d(SpydroidActivity.LOG_TAG,"available: "+available+", len: "+len+", naluLength: "+naluLength);
				
				if (fis.available()<4) {
					
					long now = SystemClock.elapsedRealtime();
					latency = now - oldlat;
					oldlat = now;
					delay = latency/nbNalu;
					
					Log.i(SpydroidActivity.LOG_TAG,"latency: "+latency+", nbNalu: "+nbNalu+", delay: "+delay+" avfifo: "+fifo.available() );
					
				}
				
			}
				
		}
		
		catch (IOException e) {
			return;
		}
	}



}