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

package net.majorkernelpanic.streaming.video;

import java.io.IOException;

import net.majorkernelpanic.streaming.MediaStream;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

public abstract class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";

	protected VideoQuality quality = VideoQuality.defaultVideoQualiy.clone();
	protected SurfaceHolder.Callback surfaceHolderCallback = null;
	protected Surface surface = null;
	protected boolean flashState = false,  qualityHasChanged = false;
	protected int videoEncoder, cameraId;
	protected Camera camera;

	public VideoStream(int cameraId) {
		super();
		this.cameraId = cameraId;
	}
	
	public void stop() {
		if (streaming) {
			try {
				super.stop();
				// We reconnect to camera just to stop the preview
				if (camera != null) {
					camera.reconnect();
					camera.stopPreview();
					camera.release();
					camera = null;
				}
			} catch (Exception e) {
				Log.e(TAG,e.getMessage());
			}
		}
	}

	public void prepare() throws IllegalStateException, IOException {

		if (camera == null) {
			camera = Camera.open(cameraId);
			// We reconnect to camera to change flash state if needed
			Parameters parameters = camera.getParameters();
			parameters.setFlashMode(flashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
			camera.setParameters(parameters);
			camera.setDisplayOrientation(quality.orientation);
			camera.unlock();
			super.setCamera(camera);
		}

		// MediaRecorder should have been like this according to me:
		// all configuration methods can be called at any time and
		// changes take effects when prepare() is called
		super.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		super.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		if (mode==MODE_DEFAULT) {
			super.setMaxDuration(1000);
			super.setMaxFileSize(Integer.MAX_VALUE);
		} else {
			// On some phones a RuntimeException might be thrown :/
			try {
				super.setMaxDuration(0);
				super.setMaxFileSize(Integer.MAX_VALUE); 
			} catch (RuntimeException e) {
				Log.e(TAG,"setMaxDuration or setMaxFileSize failed !");
			}
		}
		super.setVideoEncoder(videoEncoder);
		super.setPreviewDisplay(surface);
		super.setVideoSize(quality.resX,quality.resY);
		super.setVideoFrameRate(quality.frameRate);
		super.setVideoEncodingBitRate(quality.bitRate);
		super.prepare();
		
		// Reset flash state to ensure that default behavior is to turn it off
		flashState = false;
		
		// Quality has been updated
		qualityHasChanged = false;

	}
	
	public void setPreviewDisplay(Surface surface) {
		this.surface = surface;
	}
	
	/** Turn flash on or off if phone has one */
	public void setFlashState(boolean state) {
		// Test if phone has a flash
		//if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			// Takes effect when configure() is called
			flashState = state;
		//}
	}
	
	public void setVideoSize(int width, int height) {
		if (quality.resX != width || quality.resY != height) {
			quality.resX = width;
			quality.resY = height;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoFrameRate(int rate) {
		if (quality.frameRate != rate) {
			quality.frameRate = rate;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoEncodingBitRate(int bitRate) {
		if (quality.bitRate != bitRate) {
			quality.bitRate = bitRate;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!quality.equals(videoQuality)) {
			quality = videoQuality;
			qualityHasChanged = true;
		}
	}
	
	public void setVideoEncoder(int videoEncoder) {
		this.videoEncoder = videoEncoder;
	}
	
	public abstract String generateSessionDescriptor() throws IllegalStateException, IOException;

	public void release() {
		stop();
		super.release();
	}
	
}
