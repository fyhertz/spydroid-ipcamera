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
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionManager;
import net.majorkernelpanic.streaming.video.H264Stream;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.acra.annotation.ReportsCrashes;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

@ReportsCrashes(formKey = "dGhWbUlacEV6X0hlS2xqcmhyYzNrWlE6MQ", customReportContent = { APP_VERSION_NAME, PHONE_MODEL, BRAND, PRODUCT, ANDROID_VERSION, STACK_TRACE, USER_APP_START_DATE, USER_CRASH_DATE, LOGCAT, DEVICE_FEATURES, SHARED_PREFERENCES })
public class SpydroidApplication extends android.app.Application {

	/** Default listening port for the RTSP server. **/
	public static int sRtspPort = 8086;

	/** Default listening port for the HTTP server. **/
	public static int sHttpPort = 8080;

	/** Default quality of video streams **/
	public static VideoQuality sVideoQuality = new VideoQuality(640,480,15,500000);

	/** By default AMR is the audio encoder **/
	public static int sAudioEncoder = Session.AUDIO_AMRNB;

	/** By default H.263 is the video encoder **/
	public static int sVideoEncoder = Session.VIDEO_H263;

	/** Set this flag to true to disable the ads **/
	public final static boolean DONATE_VERSION = false;

	private static Context sContext;

	@Override
	public void onCreate() {

		// The following line triggers the initialization of ACRA
		// Please do not uncomment this line unless you change the form id or I will receive your crash reports !
		//ACRA.init(this);
		SpydroidApplication.sContext = getApplicationContext();

		super.onCreate();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		// Sets the ports of the RTSP and HTTP server according to user settings
		sRtspPort = Integer.parseInt(settings.getString("rtsp_port", String.valueOf(sRtspPort)));
		sHttpPort = Integer.parseInt(settings.getString("http_port", String.valueOf(sHttpPort)));

		// On android 3.* AAC ADTS is not supported so we set the default encoder to AMR-NB, on android 4.* AAC is the default encoder
		sAudioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? Session.AUDIO_AMRNB : Session.AUDIO_AAC;
		sAudioEncoder = Integer.parseInt(settings.getString("audio_encoder", String.valueOf(sAudioEncoder)));
		sVideoEncoder = Integer.parseInt(settings.getString("video_encoder", String.valueOf(sVideoEncoder)));

		// Read video quality settings from the preferences 
		sVideoQuality = VideoQuality.merge(
				new VideoQuality(
						settings.getInt("video_resX", 0),
						settings.getInt("video_resY", 0), 
						Integer.parseInt(settings.getString("video_framerate", "0")), 
						Integer.parseInt(settings.getString("video_bitrate", "0"))*1000),
						sVideoQuality);

		SessionManager manager = SessionManager.getManager(); 
		manager.setDefaultAudioEncoder(!settings.getBoolean("stream_audio", true)?0:sAudioEncoder);
		manager.setDefaultVideoEncoder(!settings.getBoolean("stream_video", false)?0:sVideoEncoder);
		manager.setDefaultVideoQuality(sVideoQuality);

		H264Stream.setPreferences(settings);

	}

	public static Context getContext() {
		return SpydroidApplication.sContext;
	}

}
