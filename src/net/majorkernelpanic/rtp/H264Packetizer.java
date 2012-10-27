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
import java.io.InputStream;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import android.os.SystemClock;
import android.util.Log;

/**
 *   RFC 3984
 *   
 *   H264 Streaming over RTP
 *   
 *   Must be fed with an InputStream containing h.264
 *   NAL units must be preceded by their length (4 bytes)
 *   Stream must start with mpeg4 or 3gpp header, it will be skipped
 *   
 */
public class H264Packetizer extends AbstractPacketizer {
	
	public final static String TAG = "H264Packetizer";
	
	private final static int MAXPACKETSIZE = 1400;
	private final SimpleFifo fifo = new SimpleFifo(500000);
	private ConcurrentLinkedQueue<Chunk> chunks;
	private Semaphore sync;
	private Producer producer;
	private Consumer consumer;
	
	private static class Chunk {
		public Chunk(int size,long duration) {
			this.size= size;
			this.duration = duration;
			
		}
		public int size;
		public long duration;
	}
	
	public H264Packetizer() {
		super();
	}
	
	public void start() {
		
		// We reinitialize everything so that the packetizer can be reused
		sync = new Semaphore(0);
		chunks = new ConcurrentLinkedQueue<Chunk>();
		fifo.flush();
		
		// This will skip the MPEG4 header if this step fails we can't stream anything :(
		try {
			skipHeader();
		}
		catch (IOException e)  {
			return;
		}
		
		// We start the two threads of the packetizer
		long[] sleep = new long[1];
		producer = new Producer(is, fifo, chunks, sync, sleep);
		consumer = new Consumer(socket, fifo, chunks, sync, sleep);
	}
	
	public void stop() {
		producer.running = false;
		consumer.running = false;
	}
	
	/*************************************************************************************/
	/*** This thread waits for the camera to deliver data and queue work for the other ***/
	/*************************************************************************************/
	private static class Producer extends Thread implements Runnable {

		public boolean running = true;
		private final SimpleFifo fifo;
		private final Semaphore sync;
		private final ConcurrentLinkedQueue<Chunk> chunks;
		private final InputStream is;
		private final long[] sleep;
		
		public Producer(InputStream is, SimpleFifo fifo, ConcurrentLinkedQueue<Chunk> chunks, Semaphore sync, long[] sleep) {
			this.fifo = fifo;
			this.chunks = chunks;
			this.sync = sync;
			this.is = is;
			this.sleep = sleep;
			this.start();
		}
		
		public void run() {
			int sum;
			long oldtime, duration;
			
			try {
				while (running) {
					
					// We try to read as much as we can from the camera buffer and measure how long it takes
					// In a better world we could just wait until an entire nal unit is received and directly send it and one thread would be enough !
					// But some cameras have this annoying habit of delivering more than one NAL unit at once: 
					// for example if 10 NAL units are delivered every 10 sec we have to guess the duration of each nal unit
					// And BECAUSE InputStream.read blocks, another thread is necessary to send the stuff :/
					oldtime = SystemClock.elapsedRealtime(); sum = 0;
					try {
						Thread.sleep(2*sleep[0]/3);
					} catch (InterruptedException e) {
						break;
					}
					sum = fifo.write(is,100000);
					duration = SystemClock.elapsedRealtime() - oldtime;
					
					//Log.d(TAG,"New chunk -> sleep: "+sleep[0]+" duration: "+duration+" sum: "+sum+" chunks: "+chunks.size());
					chunks.offer(new Chunk(sum,duration));
					sync.release();
				}
			} catch (IOException ignore) {
			} finally {
				running = false;
			}
		}
	}

	/*************************************************************************************/
	/***************** This thread waits for new NAL units and send them *****************/
	/*************************************************************************************/
	private static class Consumer extends Thread implements Runnable {

		public boolean running = true;
		private final SimpleFifo fifo;
		private final Semaphore sync;
		private final ConcurrentLinkedQueue<Chunk> chunks;
		private final RtpSocket socket;
		private final byte[] buffer;
		private boolean splitNal;
		private long newDelay, ts, delay = 10;
		private int cursor, naluLength = 0;
		private Chunk chunk = new Chunk(0,0), tmpChunk = null;
		private final long[] sleep;
		
		public Consumer(RtpSocket socket, SimpleFifo fifo, ConcurrentLinkedQueue<Chunk> chunks, Semaphore sync, long[] sleep) {
			this.fifo = fifo;
			this.chunks = chunks;
			this.sync = sync;
			this.socket = socket;
			this.buffer = socket.getBuffer();
			this.sleep = sleep;
			this.start();
		}
		
		public void run() {
			int len = 0; 
			
			try {
				while (running) {

					sync.acquire(1);

					// This may happen if a chunk contains only a part of a NAL unit
					if (splitNal) {
						len = naluLength-(cursor-chunk.size);
						tmpChunk = chunk;
						chunk = chunks.poll();
						chunk.duration += (tmpChunk.size>naluLength) ? tmpChunk.duration*len/tmpChunk.size : tmpChunk.duration;
						chunk.size += len;
						//Log.d(TAG,"Nal unit cut: duration: "+chunk.duration+" size: "+chunk.size+" contrib: "+chunk.duration*len/chunk.size+" naluLength: "+naluLength+" cursor: "+cursor+" len: "+len);
					} else {
						len = chunk.size-cursor;
						chunk = chunks.poll();
						chunk.size += len;
					}
					cursor = 0;
					//Log.d(TAG,"Sending chunk: "+chunk.size);
					while (chunk.size-cursor>3) send();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG,"IOException: "+e.getMessage());
				e.printStackTrace();
			}
			
			Log.d(TAG,"H264 packetizer stopped !");
			
		}
		
		// Reads a NAL unit in the FIFO and sends it
		// If it is too big, we split it in FU-A units (RFC 3984)
		private void send() throws IOException {
			int sum = 1, len = 0, type;

			// Read NAL unit length (4 bytes)
			if (!splitNal) {
				fifo.read(buffer, rtphl, 4);
				naluLength = buffer[rtphl+3]&0xFF | (buffer[rtphl+2]&0xFF)<<8 | (buffer[rtphl+1]&0xFF)<<16 | (buffer[rtphl]&0xFF)<<24;
			} else {
				splitNal = false;
			}
			cursor += naluLength+4;

			if (cursor<=chunk.size) {
				newDelay = chunk.duration*naluLength/chunk.size;
			}
			// This may happen if a chunk contains only a part of a NAL unit
			else {
				splitNal = true;
				return;
			}

			// Read NAL unit header (1 byte)
			fifo.read(buffer, rtphl, 1);
			// NAL unit type
			type = buffer[rtphl]&0x1F;
			
			delay = ( newDelay>100 || newDelay<5 ) ? delay:newDelay;
			ts += delay;
			sleep[0] = delay;
			/*try {
				Thread.sleep(3*delay/6);
			} catch (InterruptedException e) {
				return;
			}*/
			socket.updateTimestamp(ts*90);

			//Log.d(TAG,"- Nal unit length: " + naluLength+" cursor: "+cursor+" delay: "+delay+" type: "+type+" newDelay: "+newDelay);

			// Small NAL unit => Single NAL unit 
			if (naluLength<=MAXPACKETSIZE-rtphl-2) {
				len = fifo.read(buffer, rtphl+1,  naluLength-1  );
				socket.markNextPacket();
				socket.send(naluLength+rtphl);
				//Log.d(TAG,"----- Single NAL unit - len:"+len+" header:"+printBuffer(rtphl,rtphl+3)+" delay: "+delay+" newDelay: "+newDelay);
			}
			// Large NAL unit => Split nal unit 
			else {

				// Set FU-A header
				buffer[rtphl+1] = (byte) (buffer[rtphl] & 0x1F);  // FU header type
				buffer[rtphl+1] += 0x80; // Start bit
				// Set FU-A indicator
				buffer[rtphl] = (byte) ((buffer[rtphl] & 0x60) & 0xFF); // FU indicator NRI
				buffer[rtphl] += 28;

				while (sum < naluLength) {
					if ((len = fifo.read(buffer, rtphl+2,  naluLength-sum > MAXPACKETSIZE-rtphl-2 ? MAXPACKETSIZE-rtphl-2 : naluLength-sum  ))<0) return; sum += len;
					// Last packet before next NAL
					if (sum >= naluLength) {
						// End bit on
						buffer[rtphl+1] += 0x40;
						socket.markNextPacket();
					}
					socket.send(len+rtphl+2);
					// Switch start bit
					buffer[rtphl+1] = (byte) (buffer[rtphl+1] & 0x7F); 
					//Log.d(TAG,"--- FU-A unit, sum:"+sum);
				}
			}
		}
	}
	
	/********************************************************************************/
	/******** Simple fifo that will contain the NAL units waiting to be sent ********/
	/********************************************************************************/
	private static class SimpleFifo {

		private final static String TAG = "SimpleFifo";
		private int length = 0;
		private byte[] buffer;
		private int tail = 0, head = 0;
		
		public SimpleFifo(int length) {
			this.length = length;
			buffer = new byte[length];
		}

		public int write(InputStream is, int length) throws IOException {
			int len = 0;
			
			if (tail+length<this.length) {
				if ((len = is.read(buffer,tail,length)) == -1) return -1;
				tail += len;
			}
			else {
				int u = this.length-tail;
				if ((len = is.read(buffer,tail,u)) == -1) return -1;
				if (len<u) {
					tail += len;
				} else {
					if ((len = is.read(buffer,0,length-u)) == -1) return -1;
					tail = len;
					len = len+u;
				}
			}

			return len;
		}
		
		public int read(byte[] buffer, int offset, int length) {

			//length = length>available() ? available() : length;
			if (head+length<this.length) {
				System.arraycopy(this.buffer, head, buffer, offset, length);
				head += length;
			}
			else {
				int u = this.length-head;	
				System.arraycopy(this.buffer, head, buffer, offset, u);
				System.arraycopy(this.buffer, 0, buffer, offset+u, length-u);
				head = length-u;
			}
			//Log.d(TAG,"head: "+head+" tail: "+tail);
			return length;
		}
		
		public void flush() {
			tail = head = 0;
		}
		
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