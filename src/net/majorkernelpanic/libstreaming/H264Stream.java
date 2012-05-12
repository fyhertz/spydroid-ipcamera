package net.majorkernelpanic.libstreaming;

import java.io.File;
import java.io.IOException;
import java.nio.channels.IllegalSelectorException;

import net.majorkernelpanic.libmp4.MP4Config;
import net.majorkernelpanic.librtp.H264Packetizer;
import net.majorkernelpanic.spydroid.SpydroidActivity;
import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * This will stream H264 from the camera over RTP
 * Call setDestination(), setVideoSize(), setVideoFrameRate(), setVideoEncodingBitRate() and you're all set up
 * You can then call prepare() & start()
 */
public class H264Stream extends MediaStream {

	private final H264Stream that = this;
	private boolean h264Tested = false;
	private VideoQuality quality = new VideoQuality(320,240,15,500);
	private SurfaceHolder.Callback surfaceHolderCallback = null;
	private SurfaceHolder surfaceHolder = null;
	private MP4Config mp4Config;
	private Context context;
	
	public H264Stream(Context context) {
		super();

		this.context = context;
		this.packetizer = new H264Packetizer();
		configure();
	
	}
	
	public MP4Config getMP4Config() throws IllegalStateException {
		if (!h264Tested) throw new IllegalStateException("testH264() must be called before getMP4Config() !");
		return mp4Config;
	}
	
	/**
	 * Call this one instead of setPreviewDisplay(Surface sv) and don't worry about the SurfaceHolder.Callback
	 * Streaming will be automatically resumed when the surface is destroyed & recreated
	 */
	public void setPreviewDisplay(SurfaceHolder sh) {
		setPreviewDisplay(sh.getSurface());
		surfaceHolder = sh;
		surfaceHolderCallback = new SurfaceHolder.Callback() {
			private boolean wasStreaming = false;
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				// ignore
			}
			public void surfaceCreated(SurfaceHolder holder) {
				// If it was streaming, we try to restart it
				synchronized (that) {
					if (wasStreaming) {
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
					}
				}
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				synchronized (that) {
					if (streaming) {
						wasStreaming = true;
						stop();
					}
				}
			}
		};
		
		sh.addCallback(surfaceHolderCallback);
		
	}
	
	private final static String TESTFILE = "net.mpk.spydroid-test.mp4";
	
	public void setVideoSize(int width, int height) {
		if (quality.resX != width || quality.resY != height) {
			quality.resX = width;
			quality.resY = height;
			h264Tested = false;
		}
		super.setVideoSize(width, height);
	}
	
	public void setVideoFrameRate(int rate) {
		if (quality.frameRate != rate) {
			quality.frameRate = rate;
			h264Tested = false;
		}
		super.setVideoFrameRate(rate);
	}
	
	public void setVideoEncodingBitRate(int bitRate) {
		if (quality.bitRate != bitRate) {
			quality.bitRate = bitRate;
			h264Tested = false;
		}
		super.setVideoEncodingBitRate(bitRate);
	}
	
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!quality.equals(videoQuality)) {
			quality = videoQuality;
			h264Tested = false;
		}
		setVideoSize(quality.resX,quality.resY);
		setVideoFrameRate(quality.frameRate);
		setVideoEncodingBitRate(quality.bitRate);
	}
	
	private void configure() {
		setVideoSource(MediaRecorder.VideoSource.CAMERA);
		setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		if (quality != null) setVideoQuality(quality);
	}
	
	// Should not be called by the UI thread
	public MP4Config testH264() throws IllegalStateException, IOException {
		if (h264Tested) return mp4Config;
		
		// That means the H264Stream will behave as a regular MediaRecorder object
		// it will not start the packetizer thread and can be used to save the video
		// in a file
		setMode(MODE_DEFAULT);
		
		setOutputFile(context.getCacheDir().getPath()+'/'+TESTFILE);
		
		// Start test
		prepare();
		start();
		
		// We Wait a little, to record a short video
		try {
			Thread.sleep(700);
		} catch (InterruptedException ignore) {}
		
		synchronized (that) {
			stop();
		}
		
		// Retrieve SPS & PPS & ProfileId with MP4Config
		mp4Config = new MP4Config(context.getCacheDir().getPath()+'/'+TESTFILE);

		// Delete dummy video
		File file = new File(context.getCacheDir().getPath()+'/'+TESTFILE);
		if (!file.delete()) Log.e(SpydroidActivity.TAG,"Temp file could not be erased");
		
		// Back to streaming mode & prepare
		h264Tested = true;
		setMode(MODE_STREAMING);
		
		// MediaRecorder returns to the state it was before testH264() was called
		configure();
		
		return mp4Config;
		
	}
	
	protected void finalize() {
		if (surfaceHolderCallback != null && surfaceHolder != null) {
			surfaceHolder.removeCallback(surfaceHolderCallback);
		}
		super.finalize();
	}
	
}
