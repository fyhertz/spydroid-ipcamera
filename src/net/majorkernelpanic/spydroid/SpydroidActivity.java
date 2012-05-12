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

package net.majorkernelpanic.spydroid;

import net.majorkernelpanic.libstreaming.RtspServer;
import net.majorkernelpanic.libstreaming.VideoQuality;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * Main activity
 * It will test H.264 support on the phone and then launch the RTSP Server
 * 
 */
public class SpydroidActivity extends Activity implements OnSharedPreferenceChangeListener {
    
    static final public String TAG = "SPYDROID";
    
    private TextView console, ip;
    private ImageView logo;
    private SurfaceView camera;
    private SurfaceHolder holder;
    private VideoQuality defaultVideoQuality = new VideoQuality();
    private PowerManager.WakeLock wl;
    
    private static RtspServer rtspServer;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        console = (TextView) findViewById(R.id.console);
        ip = (TextView) findViewById(R.id.ip);
        
        SharedPreferences settings = getSharedPreferences("spydroid-ipcamera-prefs", 0);
        defaultVideoQuality.resX = settings.getInt("resX", 640);
        defaultVideoQuality.resY = settings.getInt("resY", 480);
        defaultVideoQuality.frameRate = settings.getInt("fps", 15);
        defaultVideoQuality.bitRate = settings.getInt("br", 1000);
        
        settings.registerOnSharedPreferenceChangeListener(this);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");
    
    	// Print version number
        try {
			log("<b>Spydroid v"+this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName+"</b>");
		} catch (NameNotFoundException e) {
			log("<b>Spydroid</b>");
		}
        
    	rtspServer = new RtspServer(this.getApplicationContext(), 8086, handler);
    	rtspServer.setSurfaceHolder(holder);
    	rtspServer.setDefaultVideoQuality(defaultVideoQuality);
    	rtspServer.start();
        
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	defaultVideoQuality.resX = sharedPreferences.getInt("resX", 640);
    	defaultVideoQuality.resY = sharedPreferences.getInt("resY", 480);
    	defaultVideoQuality.frameRate = sharedPreferences.getInt("fps", 15);
    	defaultVideoQuality.bitRate = sharedPreferences.getInt("br", 1000);
    	rtspServer.setDefaultVideoQuality(defaultVideoQuality);
    }
    
    public void onResume() {
    	super.onResume();
    	
    	wl.acquire();
    	
    	// Determines if user is connected to a wireless network & displays ip 
    	WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    	if (wifiInfo.getNetworkId()>-1) {
	    	int i = wifiInfo.getIpAddress();
	    	ip.setText("rtsp://");
	    	ip.append(String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff));
	    	ip.append(":8086/");
    	} else {
    		ip.setText("Wifi should be enabled !");
    	}
    	
    }
    
    public void onPause() {
    	super.onPause();
    	wl.release();
    }
    
    // The Handler that gets information back from the RtspServer
    private final Handler handler = new Handler() {
    	
    	public void handleMessage(Message msg) {
    		
    		switch (msg.what) {
    			
    		case RtspServer.MESSAGE_LOG:
    			// Sent when the streamingManager has something to report
    			log((String)msg.obj);
    			break;

    		case RtspServer.MESSAGE_START:
    			// Sent when streaming starts
    			logo.setAlpha(100);
    			break;
    			
    		case RtspServer.MESSAGE_STOP:
    			// Sent when streaming ends
    			logo.setAlpha(255);
    			break;

    		}
    		
    	}
    	
    };
    
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
                startActivityForResult(intent, 0);
                return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void log(String s) {
    	String t = console.getText().toString();
    	if (t.split("\n").length>8) {
    		console.setText(t.substring(t.indexOf("\n")+1, t.length()));
    	}
    	console.append(Html.fromHtml(s+"<br />"));
    }
    
    
}