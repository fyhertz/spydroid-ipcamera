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
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.acra.annotation.ReportsCrashes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

@ReportsCrashes(formKey = "dGhWbUlacEV6X0hlS2xqcmhyYzNrWlE6MQ", customReportContent = { APP_VERSION_NAME, PHONE_MODEL, BRAND, PRODUCT, ANDROID_VERSION, STACK_TRACE, USER_APP_START_DATE, USER_CRASH_DATE, LOGCAT, DEVICE_FEATURES, SHARED_PREFERENCES })
public class SpydroidApplication extends android.app.Application {

	public final static String TAG = "SpydroidApplication";
	
	/** Default quality of video streams. */
	public VideoQuality videoQuality = new VideoQuality(320,240,20,500000);

	/** By default AMR is the audio encoder. */
	public int audioEncoder = SessionBuilder.AUDIO_AAC;

	/** By default H.263 is the video encoder. */
	public int videoEncoder = SessionBuilder.VIDEO_H264;

	/** Set this flag to true to disable the ads. */
	public final boolean DONATE_VERSION = false;

	/** If the notification is enabled in the status bar of the phone. */
	public boolean notificationEnabled = true;

	/** The HttpServer will use those variables to send reports about the state of the app to the web interface. */
	public boolean applicationForeground = true;
	public Exception lastCaughtException = null;

	/** Contains an approximation of the battery level. */
	public int batteryLevel = 0;
	
	private static SpydroidApplication sApplication;

	@Override
	public void onCreate() {

		// The following line triggers the initialization of ACRA
		// Please do not uncomment this line unless you change the form id or I will receive your crash reports !
		//ACRA.init(this);

		sApplication = this;

		super.onCreate();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		notificationEnabled = settings.getBoolean("notification_enabled", true);
		
		// On android 3.* AAC ADTS is not supported so we set the default encoder to AMR-NB, on android 4.* AAC is the default encoder
		audioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? SessionBuilder.AUDIO_AMRNB : SessionBuilder.AUDIO_AAC;
		audioEncoder = Integer.parseInt(settings.getString("audio_encoder", String.valueOf(audioEncoder)));
		videoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(videoEncoder)));

		// Read video quality settings from the preferences 
		videoQuality = new VideoQuality(
						settings.getInt("video_resX", videoQuality.resX),
						settings.getInt("video_resY", videoQuality.resY), 
						Integer.parseInt(settings.getString("video_framerate", String.valueOf(videoQuality.framerate))), 
						Integer.parseInt(settings.getString("video_bitrate", String.valueOf(videoQuality.bitrate/1000)))*1000);

		SessionBuilder.getInstance() 
		.setContext(getApplicationContext())
		.setAudioEncoder(!settings.getBoolean("stream_audio", true)?0:audioEncoder)
		.setVideoEncoder(!settings.getBoolean("stream_video", false)?0:videoEncoder)
		.setVideoQuality(videoQuality);

		// Listens to changes of preferences
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

		registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
	}

	public static SpydroidApplication getInstance() {
		return sApplication;
	}

	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals("video_resX") || key.equals("video_resY")) {
				videoQuality.resX = sharedPreferences.getInt("video_resX", 0);
				videoQuality.resY = sharedPreferences.getInt("video_resY", 0);
			}

			else if (key.equals("video_framerate")) {
				videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
			}

			else if (key.equals("video_bitrate")) {
				videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
			}

			else if (key.equals("audio_encoder") || key.equals("stream_audio")) { 
				audioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", String.valueOf(audioEncoder)));
				SessionBuilder.getInstance().setAudioEncoder( audioEncoder );
				if (!sharedPreferences.getBoolean("stream_audio", false)) 
					SessionBuilder.getInstance().setAudioEncoder(0);
			}

			else if (key.equals("stream_video") || key.equals("video_encoder")) {
				videoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", String.valueOf(videoEncoder)));
				SessionBuilder.getInstance().setVideoEncoder( videoEncoder );
				if (!sharedPreferences.getBoolean("stream_video", true)) 
					SessionBuilder.getInstance().setVideoEncoder(0);
			}

			else if (key.equals("notification_enabled")) {
				notificationEnabled  = sharedPreferences.getBoolean("notification_enabled", true);
			}

		}  
	};

	private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			batteryLevel = intent.getIntExtra("level", 0);
		}
	};

}
