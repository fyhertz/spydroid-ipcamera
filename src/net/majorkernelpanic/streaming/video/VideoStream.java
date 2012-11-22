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
import android.hardware.Camera.CameraInfo;
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
	protected int videoEncoder, cameraId = 0;
	protected Camera camera;

	/** 
	 * Don't use this class directly
	 * @param cameraId Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
	 */
	public VideoStream(int camera) {
		super();
		CameraInfo cameraInfo = new CameraInfo();
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i=0;i<numberOfCameras;i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == camera) {
				this.cameraId = i;
				break;
			}
		}
	}
	
	public synchronized void stop() {
		super.stop();
		if (camera != null) {
			try {
				camera.reconnect();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
			}
			camera.stopPreview();
			try {
				camera.release();
			} catch (Exception e) {
				Log.e(TAG,e.getMessage()!=null?e.getMessage():"unknown error");
			}
			camera = null;
		}
	}

	// Don't forget to deal with the RuntimeExceptions !
	// Camera.open, Camera.setParameter, Camera.unlock may throw one
	public void prepare() throws IllegalStateException, IOException, RuntimeException {
		if (camera == null) {
			camera = Camera.open(cameraId);
			camera.setErrorCallback(new Camera.ErrorCallback() {
				@Override
				public void onError(int error, Camera camera) {
					// On some phones when trying to use the camera facing front the media server will die
					// Whether or not this callback may be called really depends on the phone
					if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
						// In this case the application must release the camera and instantiate a new one
						Log.e(TAG,"Media server died !");
						// We don't know in what thread we are so stop needs to be synchronized
						stop();
					}	
				}
			});
		}

		// If an exception is thrown after the camera was open, we must absolutly release it !
		try {
		
			// We reconnect to camera to change flash state if needed
			Parameters parameters = camera.getParameters();
			if (parameters.getFlashMode()==null && flashState) {
				// The phone has no flash
				throw new IllegalStateException("Can't turn the flash on !");
			} else {
				parameters.setFlashMode(flashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
				camera.setParameters(parameters);
			}
			camera.setDisplayOrientation(quality.orientation);
			camera.unlock();
			super.setCamera(camera);

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
		
		} catch (RuntimeException e) {
			camera.release();
			camera = null;
			throw e;
		} catch (IOException e) {
			camera.release();
			camera = null;
			throw e;
		}

	}
	
	public void setPreviewDisplay(Surface surface) {
		this.surface = surface;
	}
	
	/** Turn flash on or off if phone has one */
	public void setFlashState(boolean state) {
		flashState = state;
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
