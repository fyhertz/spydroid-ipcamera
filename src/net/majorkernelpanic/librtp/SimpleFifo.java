/*
 * Copyright (C) 2011-2012 GUIGUI Simon, fyhertz@gmail.com
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

public class SimpleFifo {

	private int length = 0, tail = 0, head = 0;
	private byte[] buffer;
	
	public SimpleFifo(int length) {
		this.length = length;
		buffer = new byte[length];
	}
	
	public void write(byte[] buffer, int offset, int length) {
		
		if (tail+length<this.length) {
			System.arraycopy(buffer, offset, this.buffer, tail, length);
			tail += length;
		}
		else {
			int u = this.length-tail;
			System.arraycopy(buffer, offset, this.buffer, tail, u);
			System.arraycopy(buffer, offset+u, this.buffer, 0, length-u);
			tail = length-u;
		}

	}
	
	public int read(byte[] buffer, int offset, int length) {
		
		length = length>available() ? available() : length;
		
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
		
		return length;
	}
	
	public int available() {
		return (tail>=head) ? tail-head : this.length-(head-tail) ; 
	}
	
}
