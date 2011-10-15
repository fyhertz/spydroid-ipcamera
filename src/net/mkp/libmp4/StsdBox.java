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

package net.mkp.libmp4;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Base64;

public class StsdBox {

	private RandomAccessFile fis;
	private byte[] buffer = new byte[4];
	private long pos = 0;
	
	private byte[] pps = new byte[5];
	private byte[] sps = new byte[11];
	
	/*
	 * fis: proper mp4 file
	 * pos: stsd box's position in the file
	 * 
	 */
	
	public StsdBox (RandomAccessFile fis, long pos) {
		
		this.fis = fis;
		this.pos = pos;
		
		findPPS();
		findSPS();
		
	}
	
	public String getProfileLevel() {
		return toHexString(sps,1,3);
	}
	
	public String getB64PPS() {
		return Base64.encodeToString(pps, 0, 5, Base64.NO_WRAP);
	}
	
	public String getB64SPS() {
		return Base64.encodeToString(sps, 0, 9, Base64.NO_WRAP);
	}
	
	private byte[] findPPS() {
		
		if (!findBoxAvcc()) return null;
		
		// Find PPS field in avcC box
		try {
			fis.skipBytes(20);
			fis.read(pps,0,5);
		} catch (IOException e) {
			return null;
		}  
		
		return pps;
		
	}	
	
	private byte[] findSPS() {
		
		if (!findBoxAvcc()) return null;
		
		// Find SPS field in avcC box
		try {
			fis.skipBytes(8);
			fis.read(sps,0,11);
		} catch (IOException e) {
			return null;
		}  
		
		return sps;
		
	}
	
	private boolean findBoxAvcc() {
		
		try {
			
			fis.seek(pos+8);
			
			while (true) {
				
				while (fis.read() != 'a');
				fis.read(buffer,0,3);
				if (buffer[0] == 'v' && buffer[1] == 'c' && buffer[2] == 'C') break;
				
			}
		
		} catch (IOException e) {
			return false;
		}
		
		return true;
		
	}
	
	static private String toHexString(byte[] buffer,int start, int len) {
		String c;
		StringBuilder s = new StringBuilder();
		for (int i=start;i<start+len;i++) {
			c = Integer.toHexString(buffer[i]&0xFF);
			s.append( c.length()<2 ? "0"+c : c );
		}
		return s.toString();
	}
	
}
