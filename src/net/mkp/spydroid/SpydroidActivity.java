/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
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

package net.mkp.spydroid;

import java.io.IOException;

import net.mkp.librtp.SessionDescriptor;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/*
 * Main activity
 */

public class SpydroidActivity extends Activity {
    
    public ViewGroup topLayout;
    public Button startButton, quitButton;
    public TextView console;
    
    static final public String LOG_TAG = "SPYDROID";
    static final public String VERSION = "1.2";
    
    private SharedPreferences settings;
    private String ip;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        topLayout = (ViewGroup) findViewById(R.id.mainlayout);
        console = (TextView) findViewById(R.id.console);
        startButton = (Button) findViewById(R.id.streambutton);
        startButton.setOnClickListener(new OnClickListener() 
        {
        	public void onClick(View v) {
        		
        		ip = ((EditText) findViewById(R.id.ip)).getText().toString();
        		
        		SharedPreferences.Editor editor = settings.edit();
        		editor.putString("ip", ip);
        	    editor.commit();
        		
        		Intent intent = new Intent(v.getContext(),SecondActivity.class);
        		intent.putExtra("ip", ip );
        		startActivityForResult(intent, 0);
        		
        	}
        });
   
        settings = getSharedPreferences("spydroid-ipcamera-prefs", 0);
        ip = settings.getString("ip", "192.170.0.1");
        ((EditText) findViewById(R.id.ip)).setText(ip);
        
    	log("<b>Hi folks :)</b>");

    	if (settings.contains("sps")) {
    		// Phone has already been tested
    		log("Phone supported !");
    	    log("Upload /sdcard/spydroid.sdp on your computer");
    	    log("And open it with vlc");
    		return;
    	}
    	
    	log("Testing H.264 Support, please wait...");
    	
    	// Test H.264 Support
    	SurfaceView cv = (SurfaceView) findViewById(R.id.smallcameraview);
    	SurfaceHolder sh = cv.getHolder();
		TestH264.RunTest(this.getCacheDir(),sh, new TestH264.Callbacks() {
			
			@Override
			public void onSuccess(String res) {

				String[] params = res.split(":");
				
				log("Phone supported ! ");
        	    
        	    // H.264 is supported and we have everything we need to generate a proper SDP file
        	    SessionDescriptor sd = new SessionDescriptor();
        	    sd.addH264Track(params[1], params[3], params[2]);
        	    sd.addAMRNBTrack();
        	    
        	    try {
        	    	
					sd.saveToFile("/sdcard/spydroid.sdp");
					
					// Store H264 parameters
	        		SharedPreferences.Editor editor = settings.edit();
	        		editor.putString("profile", params[1]);
	        		editor.putString("pps", params[2]);
	        		editor.putString("sps", params[3]);
	        	    editor.commit();
					
	        	    log("Upload /sdcard/spydroid.sdp on your computer");
	        	    log("And open it with vlc");
					
				} catch (IOException e) {
					log("Could not save sdp file to /sdcard/spydroid.sdp");
				}
				
			}
			
			@Override
			public void onError(String error) {

				log(error);
				log("Something went wrong, the app may not work properly :/");
				
			}
		});
        
    }
    
    public void onStart() {
    	super.onStart();
        
    }
    
    public synchronized void log(String s) {
    	console.append(Html.fromHtml(s+"<br />"));
    }
    
    
}