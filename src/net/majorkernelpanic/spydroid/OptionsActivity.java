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
        final Preference audioEnabled = findPreference("stream_audio");
        final Preference audioEncoder = findPreference("audio_encoder");
        
        videoEncoder.setEnabled(settings.getBoolean("stream_video", true));
        audioEncoder.setEnabled(settings.getBoolean("stream_audio", true));
        
        videoEnabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
        	public boolean onPreferenceChange(Preference preference, Object newValue) {
        		boolean state = (Boolean)newValue;
        		videoEncoder.setEnabled(state);
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
