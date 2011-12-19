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

package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.net.InetAddress;
import net.majorkernelpanic.librtp.AMRNBPacketizer;
import net.majorkernelpanic.librtp.H264Packetizer;
import net.majorkernelpanic.librtp.H264Packetizer2;
import android.media.MediaRecorder;
import android.view.SurfaceHolder;

/*
 * 
 * Instantiates two MediaStreamer objects, one for audio streaming and the other for video streaming
 * then it uses the H264Packetizer and the AMRNBPacketizer to generate two RTP streams
 * 
 */

public class CameraStreamer {

	private MediaStreamer sound = null, video = null;
	private AMRNBPacketizer sstream = null;
	private H264Packetizer2 vstream = null;
	
	public void setup(SurfaceHolder holder, String ip, int resX, int resY, int fps) throws IOException {
	
		// First we try to resolve the host
		InetAddress dest = null;
		try {
			dest = InetAddress.getByName(ip);
		}
		catch (IOException e) {
			throw new IOException("Can't resolve host");
		}
		
		// Then we prepare audio streaming
		
		sound = new MediaStreamer();
		
		sound.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		sound.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		sound.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
		try {
			sound.prepare();
		} catch (IOException e) {
			throw new IOException("Can't stream sound :(");
		}
		 
		sstream = new AMRNBPacketizer(sound.getInputStream(),dest,5004);
		
		// And finally we prepare video streaming
		
		video = new MediaStreamer();
		
		video.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		video.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		video.setVideoFrameRate(fps);
		video.setVideoSize(resX, resY);
		video.setVideoEncodingBitRate(10000);
		video.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		video.setPreviewDisplay(holder.getSurface());
		
		try {
			video.prepare();
		} catch (IOException e) {
			throw new IOException("Can't stream video :(");
		}
		
		vstream = new H264Packetizer2(video.getInputStream(),dest,5006);
		
	}
	
	public void start() {
	
		// Start sound streaming
		//sound.start();
		//sstream.startStreaming();

		// Start video streaming
		video.start();
		vstream.startStreaming();

	}
	
	public void stop() {
	
		// Stop sound streaming
		//sstream.stopStreaming();
		//sound.stop();
	
		// Stop video streaming
		vstream.stopStreaming();
		video.stop();
		
	}
	
}
