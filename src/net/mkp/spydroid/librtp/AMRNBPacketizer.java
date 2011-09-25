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
		
		buffer[rtphl] = (byte) 0xF0; // ?
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
