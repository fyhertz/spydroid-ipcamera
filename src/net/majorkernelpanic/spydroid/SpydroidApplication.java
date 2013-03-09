/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
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

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.BRAND;
import static org.acra.ReportField.DEVICE_FEATURES;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.PRODUCT;
import static org.acra.ReportField.SHARED_PREFERENCES;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.USER_APP_START_DATE;
import static org.acra.ReportField.USER_CRASH_DATE;
import net.majorkernelpanic.http.TinyHttpServer;
import net.majorkernelpanic.http.TinyHttpServer.CallbackListener;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionManager;
import net.majorkernelpanic.streaming.video.H264Stream;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.acra.annotation.ReportsCrashes;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

@ReportsCrashes(formKey = "dGhWbUlacEV6X0hlS2xqcmhyYzNrWlE6MQ", customReportContent = { APP_VERSION_NAME, PHONE_MODEL, BRAND, PRODUCT, ANDROID_VERSION, STACK_TRACE, USER_APP_START_DATE, USER_CRASH_DATE, LOGCAT, DEVICE_FEATURES, SHARED_PREFERENCES })
public class SpydroidApplication extends android.app.Application {

	public final static String TAG = "SpydroidApplication";
	
	/** Default listening port for the RTSP server. */
	public int mRtspPort = 8086;

	/** Default listening port for the HTTP server. */
	public int mHttpPort = 8080;
	
	/** Default quality of video streams. */
	public VideoQuality mVideoQuality = new VideoQuality(640,480,15,500000);

	/** By default AMR is the audio encoder. */
	public int mAudioEncoder = Session.AUDIO_AMRNB;

	/** By default H.263 is the video encoder. */
	public int mVideoEncoder = Session.VIDEO_H263;

	/** Set this flag to true to disable the ads. */
	public final boolean DONATE_VERSION = false;

	/** Default state for the RTSP server. */
	public boolean mRtspEnabled = true;

	/** Default state for the HTTP server. */
	public boolean mHttpEnabled = true;

	/** Default state for the HTTPS server. */
	public boolean mHttpsEnabled = false;		
	
	/** The HTTP/S server. */
	public CustomHttpServer mHttpServer = null;
	
	/** If the notification is enabled in the status bar of the phone. */
	public boolean mNotificationEnabled = true;

	/** The HttpServer will use those variables to send reports about the state of the app to the web interface. */
	public boolean mApplicationForeground = true;
	public Exception mLastCaughtException = null;
	
	/** Prevent garbage collection of the Surface */
	public boolean mHackEnabled = false;
	
	/** Contains an approximation of the battery level */
	public int mBatteryLevel = 0;
	
	private static SpydroidApplication sApplication;
	
	@Override
	public void onCreate() {

		// The following line triggers the initialization of ACRA
		// Please do not uncomment this line unless you change the form id or I will receive your crash reports !
		//ACRA.init(this);

		sApplication = this;
		
		super.onCreate();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		mNotificationEnabled = settings.getBoolean("notification_enabled", true);
		mHackEnabled = settings.getBoolean("video_hack", false);
		
		// Sets the ports of the RTSP and HTTP/S servers according to user settings
		mRtspPort = Integer.parseInt(settings.getString("rtsp_port", String.valueOf(mRtspPort)));
		mHttpPort = Integer.parseInt(settings.getString("http_port", String.valueOf(mHttpPort)));

		// Sets the ports of the RTSP and HTTP/S servers according to user settings
		mRtspEnabled = settings.getBoolean("rtsp_enabled", mRtspEnabled);
		mHttpEnabled = settings.getBoolean("http_enabled", mHttpEnabled);
		mHttpsEnabled = settings.getBoolean("https_enabled", mHttpsEnabled);
		
		// On android 3.* AAC ADTS is not supported so we set the default encoder to AMR-NB, on android 4.* AAC is the default encoder
		mAudioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? Session.AUDIO_AMRNB : Session.AUDIO_AAC;
		mAudioEncoder = Integer.parseInt(settings.getString("audio_encoder", String.valueOf(mAudioEncoder)));
		mVideoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(mVideoEncoder)));

		// Read video quality settings from the preferences 
		mVideoQuality = VideoQuality.merge(
				new VideoQuality(
						settings.getInt("video_resX", 0),
						settings.getInt("video_resY", 0), 
						Integer.parseInt(settings.getString("video_framerate", "0")), 
						Integer.parseInt(settings.getString("video_bitrate", "0"))*1000),
						mVideoQuality);

		SessionManager manager = SessionManager.getManager(); 
		manager.setDefaultAudioEncoder(!settings.getBoolean("stream_audio", true)?0:mAudioEncoder);
		manager.setDefaultVideoEncoder(!settings.getBoolean("stream_video", false)?0:mVideoEncoder);
		manager.setDefaultVideoQuality(mVideoQuality);

		H264Stream.setPreferences(settings);
		
		// Listens to changes of preferences
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
		
		// Starts the HTTP server service
		bindService(new Intent(this, CustomHttpServer.class), mConnection, Context.BIND_AUTO_CREATE);
		
	}
		
	public static SpydroidApplication getInstance() {
		return sApplication;
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {

			CustomHttpServer httpServer = (CustomHttpServer) ((TinyHttpServer.LocalBinder)service).getService();
			httpServer.setHttpPort(mHttpPort);
			httpServer.setHttpsPort(mHttpPort);
			if (!mHttpEnabled) {
				httpServer.setHttpEnabled(false);
				httpServer.setHttpsEnabled(false);
			} else if (mHttpsEnabled) {
				httpServer.setHttpEnabled(false);
				httpServer.setHttpsEnabled(true);
			} else {
				httpServer.setHttpEnabled(true);
				httpServer.setHttpsEnabled(false);
			}
			httpServer.setCallbackListener(new CallbackListener() {
				@Override
				public void onError(TinyHttpServer server, Exception e) {
					Log.e(TAG, "An error occured with the HTTP/S server");
					e.printStackTrace();
					mLastCaughtException = e;
				}

			});
			httpServer.start();
			mHttpServer = httpServer;
		}

		public void onServiceDisconnected(ComponentName className) {

		}
		
	};

	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("video_resX") || key.equals("video_resY")) {
				mVideoQuality.resX = sharedPreferences.getInt("video_resX", 0);
				mVideoQuality.resY = sharedPreferences.getInt("video_resY", 0);
			}
			
			else if (key.equals("video_framerate")) {
				mVideoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
			}
			
			else if (key.equals("video_bitrate")) {
				mVideoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
			}
			
			else if (key.equals("audio_encoder") || key.equals("stream_audio")) { 
				mAudioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", "0"));
				SessionManager.getManager().setDefaultAudioEncoder( mAudioEncoder );
				if (!sharedPreferences.getBoolean("stream_audio", false)) 
					SessionManager.getManager().setDefaultAudioEncoder(0);
			}
			
			else if (key.equals("stream_video") || key.equals("video_encoder")) {
				mVideoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", "0"));
				SessionManager.getManager().setDefaultVideoEncoder( mVideoEncoder );
				if (!sharedPreferences.getBoolean("stream_video", true)) 
					SessionManager.getManager().setDefaultVideoEncoder(0);
			}
			
			else if (key.equals("http_port")) {
				mHttpPort = Integer.parseInt(sharedPreferences.getString("http_port", String.valueOf(mHttpPort)));
				mHttpServer.setHttpPort(mHttpPort);
				mHttpServer.setHttpsPort(mHttpPort);
				mHttpServer.start();
			}
			
			else if (key.equals("https_enabled")) {
				if (mHttpServer != null) {
					mHttpsEnabled = sharedPreferences.getBoolean("https_enabled", true);
					if (!mHttpEnabled) {
						mHttpServer.setHttpEnabled(false);
						mHttpServer.setHttpsEnabled(false);
					} else if (mHttpsEnabled) {
						mHttpServer.setHttpEnabled(false);
						mHttpServer.setHttpsEnabled(true);
					} else {
						mHttpServer.setHttpEnabled(true);
						mHttpServer.setHttpsEnabled(false);
					}
					mHttpServer.start();
				}
			}			
			
			else if (key.equals("http_enabled")) {
				if (mHttpServer != null) {
					mHttpEnabled = sharedPreferences.getBoolean("http_enabled", true);
					if (!mHttpEnabled) {
						mHttpServer.setHttpEnabled(false);
						mHttpServer.setHttpsEnabled(false);
					} else if (mHttpsEnabled) {
						mHttpServer.setHttpEnabled(false);
						mHttpServer.setHttpsEnabled(true);
					} else {
						mHttpServer.setHttpEnabled(true);
						mHttpServer.setHttpsEnabled(false);
					}
					mHttpServer.start();
				}
			}
			
			else if (key.equals("notification_enabled")) {
				mNotificationEnabled  = sharedPreferences.getBoolean("notification_enabled", true);
			}
			
			else if (key.equals("video_hack")) {
				mHackEnabled = sharedPreferences.getBoolean("video_hack", false);
				SurfaceHolder holder = SessionManager.getManager().getSurfaceHolder();
				SessionManager.getManager().setSurfaceHolder(holder,!mHackEnabled);
			}

		}  
	};
	
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
	    @Override
	    public void onReceive(Context arg0, Intent intent) {
	      mBatteryLevel = intent.getIntExtra("level", 0);
	    }
	  };
	
}
