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
import java.util.HashMap;


public class MP4Parser {

	private HashMap<String, Long> boxes = new HashMap<String, Long>();
	private RandomAccessFile fis;
	private long pos = 0;
	private byte[] buffer = new byte[8];
	
	public MP4Parser(RandomAccessFile fis) throws IOException {
	
		this.fis = fis;
		
		long length = 0;
		try {
			length = fis.length();
		} catch (IOException e) {
			throw new IOException("Wrong size");
		}
		
		if (!parse("",length)) throw new IOException("Parsing error");
		
	}
	
	public long getBoxPos(String box) throws IOException {
		
		Long r = boxes.get(box);
		if (r==null) throw new IOException("Error: box not found");
		return boxes.get(box);
	}
	
	public StsdBox getStsdBox() throws IOException {
		try {
			return new StsdBox(fis,getBoxPos("/moov/trak/mdia/minf/stbl/stsd"));
		} catch (IOException e) {
			throw new IOException("Error: stsd box could not be found");
		}
	}
	
	private boolean parse(String path, long len) {
		
		String name="";
		long sum = 0, newlen = 0;

		boxes.put(path, pos-8);
		
		try {

			while (sum<len) {
				
				fis.read(buffer,0,8); sum += 8; pos +=8;
				if (validBoxName()) {
					
					newlen = (buffer[1]&0xFF)*65536 + (buffer[2]&0xFF)*256 + (buffer[3]&0xFF) - 8;
					name = new String(buffer,4,4);
					sum += newlen;
					if (!parse(path+'/'+name,newlen)) return false;
					
				}
				else {
					fis.skipBytes((int) (len-8)); pos += len-8;
					sum += len-8;
				}
				
				
			}
			
		} catch (IOException e) {
			return false;
		}
		return true;
		
	}

	private boolean validBoxName() {
		for (int i=0;i<4;i++) {
			if ((buffer[i+4]<97 || buffer[i+4]>122) && (buffer[i+4]<48 || buffer[i+4]>57) ) return false;
		}
		return true;
	}
	
	
	
}
