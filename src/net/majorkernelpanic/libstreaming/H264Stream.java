package net.majorkernelpanic.libstreaming;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import net.majorkernelpanic.libmp4.MP4Config;
import net.majorkernelpanic.librtp.H264Packetizer;
import net.majorkernelpanic.spydroid.SpydroidActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * This will stream H264 from the camera over RTP
 * Call setDestination() & setVideoSize() & setVideoFrameRate() & setVideoEncodingBitRate() and you're good to go
 * You can then call prepare() & start()
 */
public class H264Stream extends MediaStream {

	// Default quality for the stream
	private final static VideoQuality defaultQuality = new VideoQuality(320,240,15,500); 
	
	private VideoQuality quality = defaultQuality.clone();
	private SurfaceHolder.Callback surfaceHolderCallback = null;
	private SurfaceHolder surfaceHolder = null;
	private boolean flashState = false, h264Tested = false;
	private MP4Config mp4Config;
	private Context context;
	private Camera camera;
	
	public H264Stream(Context context, int cameraId) {
		super();
		this.context = context;
		this.packetizer = new H264Packetizer();
		this.camera = Camera.open(cameraId);
	}

	public void release() {
		super.release();
		camera.release();
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
				camera.unlock();
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
		super.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		super.setPreviewDisplay(surfaceHolder.getSurface());
		super.setVideoSize(quality.resX,quality.resY);
		super.setVideoFrameRate(quality.frameRate);
		super.setVideoEncodingBitRate(quality.bitRate);
		super.setOrientationHint(quality.orientation); // FIXME: wrong orientation of the stream and setOrientationHint doesn't help
		super.prepare();
		
		// Reset flash state to ensure that default behavior is to turn it off
		flashState = false;
		
	}
	
	/**
	 * Call this one instead of setPreviewDisplay(Surface sv) and don't worry about the SurfaceHolder.Callback
	 * Streaming will be automatically resumed when the surface is recreated
	 */
	public void setPreviewDisplay(SurfaceHolder sh) {
		surfaceHolder = sh;
		surfaceHolderCallback = new SurfaceHolder.Callback() {
			//private boolean wasStreaming = false;
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				// ignore
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
			h264Tested = false;
		}
	}
	
	public void setVideoFrameRate(int rate) {
		if (quality.frameRate != rate) {
			quality.frameRate = rate;
			h264Tested = false;
		}
	}
	
	public void setVideoEncodingBitRate(int bitRate) {
		if (quality.bitRate != bitRate) {
			quality.bitRate = bitRate;
			h264Tested = false;
		}
	}
	
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!quality.equals(videoQuality)) {
			quality = videoQuality;
			h264Tested = false;
		}
	}
	
	// Should not be called by the UI thread
	private MP4Config testH264() throws IllegalStateException, IOException {
		if (h264Tested) return mp4Config;
		
		final String TESTFILE = "test.mp4";
		
		Log.i(TAG,"Testing H264 support...");
		
		// Save flash state & set it to false so that led remains off while testing h264
		boolean savedFlashState = flashState;
		flashState = false;
		
		// That means the H264Stream will behave as a regular MediaRecorder object
		// it will not start the packetizer thread and can be used to save the video
		// in a file
		setMode(MODE_DEFAULT);
		
		setOutputFile(context.getCacheDir().getPath()+'/'+TESTFILE);
		
		// Start recording
		prepare();
		start();
		
		// We wait a little and stop recording
		try {
			Thread.sleep(1500);
		} catch (InterruptedException ignore) {}
		stop();
		
		// Retrieve SPS & PPS & ProfileId with MP4Config
		mp4Config = new MP4Config(context.getCacheDir().getPath()+'/'+TESTFILE);

		// Delete dummy video
		File file = new File(context.getCacheDir().getPath()+'/'+TESTFILE);
		if (!file.delete()) Log.e(SpydroidActivity.TAG,"Temp file could not be erased");
		
		// Back to streaming mode & prepare
		h264Tested = true;
		setMode(MODE_STREAMING);
		
		// Restore flash state
		flashState = savedFlashState;
		
		Log.i(TAG,"H264 Test succeded...");
		
		return mp4Config;
		
	}
	
	public String generateSdpDescriptor() throws IllegalStateException, IOException {
		testH264();
		return "m=video "+String.valueOf(getDestinationPort())+" RTP/AVP 96\r\n" +
				   "b=RR:0\r\n" +
				   "a=rtpmap:96 H264/90000\r\n" +
				   "a=fmtp:96 packetization-mode=1;profile-level-id="+mp4Config.getProfileLevel()+";sprop-parameter-sets="+mp4Config.getB64SPS()+","+mp4Config.getB64PPS()+";\r\n";
	}
	
	protected void finalize() {
		if (surfaceHolderCallback != null && surfaceHolder != null) {
			surfaceHolder.removeCallback(surfaceHolderCallback);
		}
		super.finalize();
	}
	
}
