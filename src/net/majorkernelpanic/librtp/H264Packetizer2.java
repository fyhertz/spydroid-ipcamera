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
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

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
	
	private final static String TAG = "H264Packetizer";
	
	private final int packetSize = 1400;
	private long oldtime = SystemClock.elapsedRealtime(), duration, delay, ts = oldtime;
	private int available = 0, oldavailable = 0, size, naluLength = 0, cursor = 0;
	private SimpleFifo fifo = new SimpleFifo(500000);
	private Semaphore nbChunks = new Semaphore(0);
	private LinkedList<Chunk> chunks = new LinkedList<Chunk>();
	private Chunk chunk = null;
	
	
	private class Chunk {
		public Chunk(int size,long duration) {
			this.size= size;
			this.duration = duration;
			
		}
		public int size;
		public long duration;
	}
	
	public H264Packetizer2() {
		super();
	}
	
	public void run() {
        
		int len = 0;
		
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
				//Log.e(TAG,"Atom skipped: "+printBuffer(rtphl+4,rtphl+8)+" size: "+len);
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

		new Thread(new Runnable() {
			public void run() {
				try {
					nbChunks.acquire(1);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				while (running) {
					chunk = chunks.getFirst();
					while (cursor<chunk.size) send();
					if (cursor==chunk.size) {
						try {
							nbChunks.acquire(1);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					chunks.pop();
					cursor = 0;
				}
			}
		}).start();
		
		while (running) { 
			
			try {
				
				oldtime = SystemClock.elapsedRealtime();
				oldavailable = available = fis.available();
				while (available<=oldavailable) {
					Thread.sleep(10);
					available = fis.available();
				}					
				
				duration = SystemClock.elapsedRealtime() - oldtime;
				size = fillFifo(available);
				Log.e(TAG,"New chunk -> delay: "+duration+" s1: "+size+" s2: "+available+" available: "+fifo.available()+" chunks: "+chunks.size());
				chunks.add(new Chunk(size,duration));
				nbChunks.release();

			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}
	
	/**
	 * Reads a NAL unit in the FIFO and sends it
	 * If it is too big, we split it in FU-A units (RFC 3984)
	 */
	private void send() {

		int sum = 1, len = 0;
		
		// Read nal unit length (4 bytes) and nal unit header (1 byte)
		fifo.read(buffer, rtphl, 5); cursor += 5;
		naluLength = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;

		if (naluLength+cursor<=chunk.size) {
			
			delay = chunk.duration*naluLength/chunk.size;
			
		} else {
			
			Log.e(TAG,"nal unit cut: cursor: "+cursor+" naluLength: "+naluLength+" len: "+(naluLength-(chunk.size-cursor)));
			
			try {
				nbChunks.acquire(1);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			len = naluLength-(chunk.size-cursor);
			Chunk c = chunks.get(1);
			delay = chunk.duration*(chunk.size-cursor)/chunk.size + c.duration*len/c.size;
			c.duration -= c.duration*len/c.size;
			c.size -= len; 
			
		}
		
		cursor += naluLength - 1;
		ts += delay;
		
		Log.d(TAG,"- Nal unit length: " + naluLength+" cursor: "+cursor+" delay: "+delay);

		try {
			Thread.sleep(5*delay/6);
		} catch (InterruptedException e) {
			return;
		}
		
		rsock.updateTimestamp(ts*90);

		// Small nal unit => Single nal unit 
		if (naluLength<=packetSize-rtphl-2) {

			buffer[rtphl] = buffer[rtphl+4];
			len = fifo.read(buffer, rtphl+1,  naluLength-1  );
			rsock.markNextPacket();
			rsock.send(naluLength+rtphl);

			//Log.e(TAG,"----- Single NAL unit read:"+len+" header:"+printBuffer(rtphl,rtphl+3));

		}

		// Large nal unit => Split nal unit 
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

				/* Last packet before next NAL */
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl+1] += 0x40;
					rsock.markNextPacket();
				}

				rsock.send(len+rtphl+2);

				/* Switch start bit */
				buffer[rtphl+1] = (byte) (buffer[rtphl+1] & 0x7F); 

				//Log.d(TAG,"--- FU-A unit, end:"+(boolean)(sum>=naluLength));

			}


		}
		
	}
	
	private int fillFifo(int length) {
		
		int sum = 0, len = 0;
		
		try {
			while (sum<length) {
				len = fifo.write(fis, length-sum);
				sum += len;
				if (len==-1) return -1;
			}
		} catch (IOException e) {
			return -1;
		}
		
		return sum;
		
	}

}