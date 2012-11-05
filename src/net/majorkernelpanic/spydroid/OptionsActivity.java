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

package net.majorkernelpanic.spydroid;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.majorkernelpanic.networking.Session;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class OptionsActivity extends PreferenceActivity {

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
        
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final Preference videoEnabled = findPreference("stream_video");
        final Preference videoEncoder = findPreference("video_encoder");
        final Preference videoResolution = findPreference("video_resolution");
        final Preference videoBitrate = findPreference("video_bitrate");
        final Preference videoFramerate = findPreference("video_framerate");
        final Preference audioEnabled = findPreference("stream_audio");
        final Preference audioEncoder = findPreference("audio_encoder");
        
        boolean videoState = settings.getBoolean("stream_video", true);
        videoEncoder.setEnabled(videoState);
		videoResolution.setEnabled(videoState);
		videoBitrate.setEnabled(videoState);
		videoFramerate.setEnabled(videoState);        
        audioEncoder.setEnabled(settings.getBoolean("stream_audio", true));
        
        videoResolution.setSummary("Current resolution is "+
        		settings.getInt("video_resX", Session.defaultVideoQuality.resX)+"x"+
        		settings.getInt("video_resY", Session.defaultVideoQuality.resY)+"px");
        videoFramerate.setSummary("Current framerate is "+Integer.parseInt(settings.getString("video_framerate", "15"))+"fps");
        videoBitrate.setSummary("Current bitrate is "+Integer.parseInt(settings.getString("video_bitrate", "500"))+"kbps");
        
        videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        	public boolean onPreferenceChange(Preference preference, Object newValue) {
        		Editor editor = settings.edit();
        		Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
        		Matcher matcher = pattern.matcher((String)newValue);
        		matcher.find();
        		editor.putInt("video_resX", Integer.parseInt(matcher.group(1)));
        		editor.putInt("video_resY", Integer.parseInt(matcher.group(2)));
        		editor.commit();
        		videoResolution.setSummary("Current resolution is "+(String)newValue+"px");
        		return true;
			}
        });
        
        videoFramerate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        	public boolean onPreferenceChange(Preference preference, Object newValue) {
        		videoFramerate.setSummary("Current framerate is "+(String)newValue+"fps");
        		return true;
			}
        });

        videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        	public boolean onPreferenceChange(Preference preference, Object newValue) {
        		videoBitrate.setSummary("Current bitrate is "+(String)newValue+"kbps");
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
