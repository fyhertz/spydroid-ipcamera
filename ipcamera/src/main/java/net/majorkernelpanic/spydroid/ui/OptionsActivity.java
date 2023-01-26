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

package net.majorkernelpanic.spydroid.ui;

import static net.majorkernelpanic.http.TinyHttpServer.KEY_HTTPS_ENABLED;
import static net.majorkernelpanic.http.TinyHttpServer.KEY_HTTPS_PORT;
import static net.majorkernelpanic.http.TinyHttpServer.KEY_HTTP_ENABLED;
import static net.majorkernelpanic.http.TinyHttpServer.KEY_HTTP_PORT;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

@SuppressWarnings("deprecation")
public class OptionsActivity extends PreferenceActivity {

	private SpydroidApplication mApplication = null;

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		mApplication = (SpydroidApplication) getApplication();

		addPreferencesFromResource(R.xml.preferences);

		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final Preference videoEnabled = findPreference("stream_video");
		final Preference audioEnabled = findPreference("stream_audio");
		final ListPreference audioEncoder = (ListPreference) findPreference("audio_encoder");
		final ListPreference videoEncoder = (ListPreference) findPreference("video_encoder");
		final ListPreference videoResolution = (ListPreference) findPreference("video_resolution");
		final ListPreference videoBitrate = (ListPreference) findPreference("video_bitrate");
		final ListPreference videoFramerate = (ListPreference) findPreference("video_framerate");
		final CheckBoxPreference httpEnabled = (CheckBoxPreference) findPreference("http_server_enabled");
		final CheckBoxPreference  httpsEnabled = (CheckBoxPreference) findPreference("use_https");
		final Preference httpPort = findPreference(KEY_HTTP_PORT);
		final Preference httpsPort = findPreference(KEY_HTTPS_PORT);

		boolean videoState = settings.getBoolean("stream_video", true);
		videoEncoder.setEnabled(videoState);
		videoResolution.setEnabled(videoState);
		videoBitrate.setEnabled(videoState);
		videoFramerate.setEnabled(videoState);        

		videoEncoder.setValue(String.valueOf(mApplication.videoEncoder));
		audioEncoder.setValue(String.valueOf(mApplication.audioEncoder));
		videoFramerate.setValue(String.valueOf(mApplication.videoQuality.framerate));
		videoBitrate.setValue(String.valueOf(mApplication.videoQuality.bitrate/1000));
		videoResolution.setValue(mApplication.videoQuality.resX+"x"+mApplication.videoQuality.resY);

		videoResolution.setSummary(getString(R.string.settings0)+" "+videoResolution.getValue()+"px");
		videoFramerate.setSummary(getString(R.string.settings1)+" "+videoFramerate.getValue()+"fps");
		videoBitrate.setSummary(getString(R.string.settings2)+" "+videoBitrate.getValue()+"kbps");

		audioEncoder.setEnabled(settings.getBoolean("stream_audio", false));

		httpEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean state = (Boolean)newValue;
				if (httpsEnabled != null) httpsEnabled.setEnabled(state);
				httpPort.setEnabled(state);
				if (httpsPort != null) httpsPort.setEnabled(state);
				Editor editor = settings.edit();
				// Updates the HTTP server
				if (!state) {
					editor.putBoolean(KEY_HTTP_ENABLED, false);
					editor.putBoolean(KEY_HTTPS_ENABLED, false);
				} else {
					// HTTP/HTTPS, it's one or the other
					if (httpsEnabled != null && httpsEnabled.isChecked()) {
						editor.putBoolean(KEY_HTTPS_ENABLED, true);
						editor.putBoolean(KEY_HTTP_ENABLED, false);
					} else {
						editor.putBoolean(KEY_HTTPS_ENABLED, false);
						editor.putBoolean(KEY_HTTP_ENABLED, true);
					}
				}
				editor.commit();
				return true;
			}
		});

		if (httpsEnabled != null) { 
			httpsEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean state = (Boolean)newValue;
					Editor editor = settings.edit();
					// Updates the HTTP server
					if (!httpEnabled.isChecked()) {
						editor.putBoolean(KEY_HTTP_ENABLED, false);
						editor.putBoolean(KEY_HTTPS_ENABLED, false);
					} else {
						// HTTP/HTTPS, it's one or the other
						if (state) {
							editor.putBoolean(KEY_HTTPS_ENABLED, true);
							editor.putBoolean(KEY_HTTP_ENABLED, false);
						} else {
							editor.putBoolean(KEY_HTTPS_ENABLED, false);
							editor.putBoolean(KEY_HTTP_ENABLED, true);
						}
					}
					editor.commit();
					return true;
				}
			});
		}

		videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Editor editor = settings.edit();
				Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
				Matcher matcher = pattern.matcher((String)newValue);
				matcher.find();
				editor.putInt("video_resX", Integer.parseInt(matcher.group(1)));
				editor.putInt("video_resY", Integer.parseInt(matcher.group(2)));
				editor.commit();
				videoResolution.setSummary(getString(R.string.settings0)+" "+(String)newValue+"px");
				return true;
			}
		});

		videoFramerate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				videoFramerate.setSummary(getString(R.string.settings1)+" "+(String)newValue+"fps");
				return true;
			}
		});

		videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				videoBitrate.setSummary(getString(R.string.settings2)+" "+(String)newValue+"kbps");
				return true;
			}
		});

		videoEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean state = (Boolean)newValue;
				videoEncoder.setEnabled(state);
				videoResolution.setEnabled(state);
				videoBitrate.setEnabled(state);
				videoFramerate.setEnabled(state);
				return true;
			}
		});

		audioEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean state = (Boolean)newValue;
				audioEncoder.setEnabled(state);
				return true;
			}
		});

	}

}
