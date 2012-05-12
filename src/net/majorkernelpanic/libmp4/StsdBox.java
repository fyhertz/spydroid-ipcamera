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

package net.majorkernelpanic.libmp4;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Base64;

public class StsdBox {

	private RandomAccessFile fis;
	private byte[] buffer = new byte[4];
	private long pos = 0;
	
	private byte[] pps;
	private byte[] sps;
	private int spsLength, ppsLength;
	
	/*
	 * fis: proper mp4 file
	 * pos: stsd box's position in the file
	 * 
	 */
	
	public StsdBox (RandomAccessFile fis, long pos) {

		this.fis = fis;
		this.pos = pos;
		
		findBoxAvcc();
		findSPSandPPS();
		
	}
	
	public String getProfileLevel() {
		return toHexString(sps,1,3);
	}
	
	public String getB64PPS() {
		return Base64.encodeToString(pps, 0, ppsLength, Base64.NO_WRAP);
	}
	
	public String getB64SPS() {
		return Base64.encodeToString(sps, 0, spsLength, Base64.NO_WRAP);
	}
	
	private boolean findSPSandPPS() {
		
		/*
		 *  SPS and PPS parameters are stored in the avcC box
		 *  You may find really useful information about this box 
		 *  in the document ISO-IEC 14496-15, part 5.2.4.1.1
		 *  The box's structure is described there
		 *  
		 *  aligned(8) class AVCDecoderConfigurationRecord {
		 *		unsigned int(8) configurationVersion = 1;
		 *		unsigned int(8) AVCProfileIndication;
		 *		unsigned int(8) profile_compatibility;
		 *		unsigned int(8) AVCLevelIndication;
		 *		bit(6) reserved = ‘111111’b;
		 *		unsigned int(2) lengthSizeMinusOne;
		 *		bit(3) reserved = ‘111’b;
		 *		unsigned int(5) numOfSequenceParameterSets;
		 *		for (i=0; i< numOfSequenceParameterSets; i++) {
		 *			unsigned int(16) sequenceParameterSetLength ;
		 *			bit(8*sequenceParameterSetLength) sequenceParameterSetNALUnit;
		 *		}
		 *		unsigned int(8) numOfPictureParameterSets;
		 *		for (i=0; i< numOfPictureParameterSets; i++) {
		 *			unsigned int(16) pictureParameterSetLength;
		 *			bit(8*pictureParameterSetLength) pictureParameterSetNALUnit;
		 *		}
		 *	}
		 *
		 *  
		 *  
		 */

		try {
			
			// TODO: Here we assume that numOfSequenceParameterSets = 1, numOfPictureParameterSets = 1 !
			
			// Here we extract the SPS parameter
			fis.skipBytes(7);
			spsLength  = 0xFF&fis.readByte();
			sps = new byte[spsLength];
			fis.read(sps,0,spsLength);
			// Here we extract the PPS parameter
			fis.skipBytes(2);
			ppsLength = 0xFF&fis.readByte();
			pps = new byte[ppsLength];
			fis.read(pps,0,ppsLength);
			
			
		} catch (IOException e) {
			return false;
		}
		
		return true;
		
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
