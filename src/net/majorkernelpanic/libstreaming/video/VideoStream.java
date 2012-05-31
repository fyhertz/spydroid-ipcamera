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

package net.majorkernelpanic.libstreaming.video;

import java.io.IOException;

import net.majorkernelpanic.libstreaming.MediaStream;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

public abstract class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";
	
	// Default quality for the stream
	private final static VideoQuality defaultQuality = new VideoQuality(320,240,15,500); 
	
	protected VideoQuality quality = defaultQuality.clone();
	protected SurfaceHolder.Callback surfaceHolderCallback = null;
	protected SurfaceHolder surfaceHolder = null;
	protected boolean flashState = false,  qualityHasChanged = false;
	protected int videoEncoder;
	protected Context context;
	protected Camera camera;

	public VideoStream(Context context, int cameraId) {
		super();
		this.context = context;
		this.camera = Camera.open(cameraId);
	}
	
	public void stop() {
		if (streaming) {
			try {
				super.stop();
			} catch (RuntimeException e) {
				// stop() can throw a RuntimeException when called too quickly after start() !
				Log.d(TAG,"stop() called too quickly after start() but it's okay");
			} 
			try {
				// We reconnect to camera just to stop the preview
				camera.reconnect();
				camera.stopPreview();
			} catch (IOException ignore) {}
		}
	}
	
	public void prepare() throws IllegalStateException, IOException {
		
		// We reconnect to camera to change flash state if needed
		camera.reconnect();
		Parameters parameters = camera.getParameters();
		parameters.setFlashMode(flashState?Parameters.FLASH_MODE_TORCH:Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
		camera.setDisplayOrientation(quality.orientation);
		camera.stopPreview();
		camera.unlock();
		
		// MediaRecorder should have been like this according to me:
		// all configuration methods can be called at any time and
		// changes take effects when prepare() is called
		super.setCamera(camera);
		super.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		super.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		if (mode==MODE_DEFAULT) {
			super.setMaxDuration(2000);
			super.setMaxFileSize(Integer.MAX_VALUE);
		} else {
			// On some phones a RuntimeException might be thrown :/
			try {
				super.setMaxDuration(0);
				super.setMaxFileSize(Integer.MAX_VALUE); 
			} catch (RuntimeException ignore) {}
		}
		super.setVideoEncoder(videoEncoder);
		super.setPreviewDisplay(surfaceHolder.getSurface());
		super.setVideoSize(quality.resX,quality.resY);
		super.setVideoFrameRate(quality.frameRate);
		super.setVideoEncodingBitRate(quality.bitRate);
		super.setOrientationHint(quality.orientation); // FIXME: wrong orientation of the stream and setOrientationHint doesn't help
		super.prepare();
		
		// Reset flash state to ensure that default behavior is to turn it off
		flashState = false;
		
		// Quality has been updated
		qualityHasChanged = false;

	}
	
	/**
	 * Call this one instead of setPreviewDisplay(Surface sv) and don't worry about the SurfaceHolder.Callback
	 */
	public void setPreviewDisplay(SurfaceHolder sh) {
		surfaceHolder = sh;
		surfaceHolderCallback = new SurfaceHolder.Callback() {
			//private boolean wasStreaming = false;
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.d(TAG,"Surface changed !");
				surfaceHolder = holder;
			}
			public void surfaceCreated(SurfaceHolder holder) {
				// If it was streaming, we try to restart it
				/*if (wasStreaming) {
					try {
						prepare();
						start();
					} catch (IllegalStateException e) {
						stop();
					} catch (IOException e) {
						stop();
					} finally {
						wasStreaming = false;
					}
				}*/
				Log.d(TAG,"Surface created !");
				surfaceHolder = holder;
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (streaming) {
					//wasStreaming = true;
					stop();
				}
				Log.d(TAG,"Surface destroyed !");
			}
		};
		sh.addCallback(surfaceHolderCallback);
	}
	
	/** Turn flash on or off if phone has one */
	public void setFlashState(boolean state) {
		// Test if phone has a flash
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
			// Takes effect when configure() is called
			flashState = true;
		}
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
	
	public abstract String generateSdpDescriptor() throws IllegalStateException, IOException;

	public void release() {
		stop();
		camera.release();
		if (surfaceHolderCallback != null && surfaceHolder != null) {
			surfaceHolder.removeCallback(surfaceHolderCallback);
		}
		super.release();
	}
	
}
