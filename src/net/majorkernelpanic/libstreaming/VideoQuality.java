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

package net.majorkernelpanic.libstreaming;

public class VideoQuality {

	public VideoQuality() {}
	
	public VideoQuality(int resX, int resY, int frameRate, int bitRate) {
		
		this.frameRate = frameRate;
		this.bitRate = bitRate;
		this.resX = resX;
		this.resY = resY;
		
	}
	
	public int frameRate = 0;
	public int bitRate = 0;
	public int resX = 0;
	public int resY = 0;
	
	public boolean equals(VideoQuality quality) {
		if (quality==null) return false;
		return (quality.resX == this.resX 				&
				 quality.resY == this.resY 				&
				 quality.frameRate == this.frameRate	&
				 quality.bitRate == this.bitRate 		);
	}
	
}
