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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
 * It will also test H.264 support on the phone
 */

public class SpydroidActivity extends Activity {
    
    public ViewGroup topLayout;
    public Button startButton, quitButton;
    public TextView console;
    
    
    static final public String LOG_TAG = "SPYDROID";
    
    private SharedPreferences settings;
    private String ip;
    private SurfaceView cv;
    private SurfaceHolder sh;
    
    private int resX,resY,fps, oldResX, oldResY, oldFps;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        topLayout = (ViewGroup) findViewById(R.id.mainlayout);
        console = (TextView) findViewById(R.id.console);
        startButton = (Button) findViewById(R.id.streambutton);
        startButton.setOnClickListener(new OnClickListener() 
        {
        	
        	// Called when pressing the "Stream" button
        	public void onClick(View v) {
        		
        		ip = ((EditText) findViewById(R.id.ip)).getText().toString();
        		
        		SharedPreferences.Editor editor = settings.edit();
        		editor.putString("ip", ip);
        	    editor.commit();
        		
        		Intent intent = new Intent(v.getContext(),SecondActivity.class);
        		intent.putExtra("net.mpk.spydroid.ip", ip );
        		intent.putExtra("net.mpk.spydroid.resX", resX );
        		intent.putExtra("net.mpk.spydroid.resY", resY );
        		intent.putExtra("net.mpk.spydroid.fps", fps );
        		startActivityForResult(intent, 0);
        		
        	}
        });
   
        settings = getSharedPreferences("spydroid-ipcamera-prefs", 0);
        ip = settings.getString("ip", "192.170.0.1");
       	resX = settings.getInt("resX", 640);
       	resY = settings.getInt("resY", 480);
       	fps = settings.getInt("fps", 15);
       	oldResX = resX; oldResY = resY; oldFps = fps;
        ((EditText) findViewById(R.id.ip)).setText(ip);
        
    }
    
    public void onResume() {
    	super.onResume();
    	
    	console.setText("");
    	log("<b>Hi folks :)</b>");
    	log("Quality is "+String.valueOf(resX)+"x"+String.valueOf(resY)+", "+String.valueOf(fps)+"fps");
    	
    	if (settings.contains("sps") && (oldResX==resX && oldResY==resY && oldFps==fps) ) {
    		// Phone has already been tested
    		log("Phone supported !");
    	    log("Upload /sdcard/spydroid.sdp on your computer");
    	    log("And open it with vlc");
    		return;
    	}
    	/*
    	// Test H.264 Support
    	cv = (SurfaceView) findViewById(R.id.smallcameraview);
    	sh = cv.getHolder();
		TestH264.RunTest(this.getCacheDir(),sh, resX, resY, fps, new TestH264.Callback() {
			
			@Override
			public void onStart() {
				log("<b>Testing H.264 Support, please wait...</b>");
			}
			
			// Called if H.264 isn't supported with chosen quality settings
			public void onError(String error) {
				log(error);
				log("Something went wrong, the app may not work properly :/");
			}
			
			// Called if H.264 is supported with chosen quality settings
			public void onSuccess(String result) {

				String[] params = result.split(":");
				
				log("Phone supported ! ");
			    
			    // we have everything we need to generate a proper SDP file
			    SessionDescriptor sd = new SessionDescriptor();
			    sd.addH264Track(params[0], params[2], params[1]);
			    sd.addAMRNBTrack();
			    
			    try {
			    	
					sd.saveToFile("/sdcard/spydroid.sdp");
					
					// Store H264 parameters
		    		SharedPreferences.Editor editor = settings.edit();
		    		editor.putString("profile", params[0]);
		    		editor.putString("pps", params[1]);
		    		editor.putString("sps", params[2]);
		    	    editor.commit();
					
		    	    log("Upload /sdcard/spydroid.sdp on your computer");
		    	    log("And open it with vlc");
					
				} catch (IOException e) {
					log("Could not save sdp file to /sdcard/spydroid.sdp");
				}
				
				oldResX = resX; oldResY = resY; oldFps = fps;
				
			}
			
		});
		*/
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.quality:
        	// Starts QualityListActivity where user can change the streaming quality
    		Intent intent = new Intent(this.getBaseContext(),QualityListActivity.class);
    		intent.putExtra("ip", ip );
    		startActivityForResult(intent, 0);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	// User has changed quality
    	if (resultCode==1000) {

    		resX = data.getIntExtra("net.mpk.spydroid.resX", 0);
    		resY = data.getIntExtra("net.mpk.spydroid.resY", 0);
    		fps = data.getIntExtra("net.mpk.spydroid.fps", 0);

    		SharedPreferences.Editor editor = settings.edit();
    		editor.putInt("resX", resX);
    		editor.putInt("resY", resY);
    		editor.putInt("fps", fps);
    	    editor.commit();
    		
    	}
    	
    }
    
    public synchronized void log(String s) {
    	console.append(Html.fromHtml(s+"<br />"));
    }
    
    
}