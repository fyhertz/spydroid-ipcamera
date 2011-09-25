package net.mkp.spydroid.librtp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;

import net.mkp.spydroid.SpydroidActivity;
import android.os.SystemClock;
import android.util.Log;

/*
 *   RFC 3984
 *   
 *   H264 Streaming over RTP
 *   
 */

public class H264Packetizer extends AbstractPacketizer {

	private final int packetSize = 1400;
	private final int mpeg4HeaderLength = 40; 	// 40 Bytes

	private long oldtime = SystemClock.elapsedRealtime(), delay = 18, oldavailable;
	
	public H264Packetizer(InputStream fis, InetAddress dest) throws SocketException {
		super(fis, dest, 5006);
	}

	public void run() {
		
		int naluLength, sum, len;
		long now = 12000, timestamp = 0;
        
		// Skip mpeg4 header (all bytes preceding the mdat atom)
		fill(rtphl,mpeg4HeaderLength);
		
		while (running) { 
		 
			// Read nal unit length (4 bytes) and nal unit header (1 byte)
			fill(rtphl, 5);   
			naluLength = (buffer[rtphl+3]&0xFF) + (buffer[rtphl+2]&0xFF)*256 + (buffer[rtphl+1]&0xFF)*65536;
			
			//Log.e(SpydroidActivity.LOG_TAG,"- Nal unit length: " + naluLength);
			
			rsock.updateTimestamp(SystemClock.elapsedRealtime()*90);
			
			sum = 1;
			
			// RFC 3984
			// Packetization mode = 1
			
			// Small nal unit => Single nal unit
			if (naluLength<=packetSize-rtphl-2) {
				
				buffer[rtphl] = buffer[rtphl+4];
				len = fill(rtphl+1,  naluLength-1  );
				rsock.markNextPacket();
				send(naluLength+rtphl);
				
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
		    		
					len = fill( rtphl+2,  naluLength-sum > packetSize-rtphl-2 ? packetSize-rtphl-2 : naluLength-sum  ); sum += len;
					
					// Last packet before next nal
					if (sum >= naluLength) {
						// End bit on
						buffer[rtphl+1] += 0x40;
						rsock.markNextPacket();
					}
						
					send(len+rtphl+2);
					
					// Switch start bit 
					buffer[rtphl+1] = (byte) (buffer[rtphl+1] & 0x7F); 
					
					//Log.e(SpydroidActivity.LOG_TAG,"--- FU-A unit, end:"+(boolean)(sum>=naluLength));
					
		    	}
			}
			
		}
		
		
	}
	
	private int fill(int offset,int length) {
		
		int sum = 0, len, available;
		
		while (sum<length) {
			try { 
				available = fis.available();
				len = fis.read(buffer, offset+sum, length-sum);
				Log.e(SpydroidActivity.LOG_TAG,"Data read: "+fis.available()+","+len);
				
				if (oldavailable<available) {
					// We don't want fis.available to reach 0 because it provokes choppy streaming (which is logical because it causes fis.read to block the thread periodically).
					// So here, we increase the delay between two send calls to induce more buffering (and the buffer is basically the fis input stream) 
					if (oldavailable<10000) {
						delay++;
						//Log.e(SpydroidActivity.LOG_TAG,"Inc delay: "+delay);
					}
					// But we don't want to much buffering either:
					else if (oldavailable>10000) {						
						delay--;
						//Log.e(SpydroidActivity.LOG_TAG,"Dec delay: "+delay);
					}
				}
				oldavailable = available;
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
	
	private void send(int size) {
		
		long now = SystemClock.elapsedRealtime();
		
		if (now-oldtime<delay)
			try {
				Thread.sleep(delay-(now-oldtime));
			} catch (InterruptedException e) {}
		oldtime = SystemClock.elapsedRealtime();
		rsock.send(size);
		
	}
	

}
