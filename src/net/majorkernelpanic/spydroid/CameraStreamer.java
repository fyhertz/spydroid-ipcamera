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
import android.util.Log;
import android.view.SurfaceHolder;

/*
 * 
 * Instantiates two MediaStreamer objects, one for audio streaming and the other for video streaming
 * then it uses the H264Packetizer and the AMRNBPacketizer to generate two RTP streams
 * 
 */

public class CameraStreamer {

	private MediaStreamer sound = new MediaStreamer(), video = new MediaStreamer();
	private AMRNBPacketizer sstream = null;
	private H264Packetizer2 vstream = null;
	
	public void setup(SurfaceHolder holder, String ip, int resX, int resY, int fps) throws IOException {
	
		// AUDIO
		
		sound.reset();
		
		sound.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		sound.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
		sound.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
		try {
			sound.prepare();
		} catch (IOException e) {
			throw new IOException("Can't stream sound :("); 
		}
		
		try {
			sstream = new AMRNBPacketizer(sound.getInputStream(), InetAddress.getByName(ip), 5004);
		} catch (IOException e) {
			Log.e(SpydroidActivity.LOG_TAG,"Unknown host");
			throw new IOException("Can't resolve host :(");
		}
		
		// VIDEO
		
		video.reset();
		
		video.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		video.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		video.setVideoFrameRate(fps);
		video.setVideoSize(resX, resY);
		video.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		video.setPreviewDisplay(holder.getSurface());
		
		try {
			video.prepare();
		} catch (IOException e) {
			throw new IOException("Can't stream video :(");
		}
		
		try {
			vstream = new H264Packetizer2(video.getInputStream(), InetAddress.getByName(ip), 5006);
		} catch (IOException e) {
			Log.e(SpydroidActivity.LOG_TAG,"Unknown host");
			throw new IOException("Can't resolve host :(");
		}
		
	}
	
	public void start() {
	
		// Start sound streaming
		sound.start();
		sstream.startStreaming();
		
		// Start video streaming
		video.start();
		vstream.startStreaming();
		
	}
	
	public void stop() {
		
		// Stop sound streaming
		sstream.stopStreaming();
		sound.stop();
	
		// Stop video streaming
		vstream.stopStreaming();
		video.stop();
		
	}
	
}
