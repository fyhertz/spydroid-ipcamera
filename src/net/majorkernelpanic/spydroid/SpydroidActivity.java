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

import java.io.IOException;

import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class SpydroidActivity extends Activity implements OnSharedPreferenceChangeListener {
    
    static final public String TAG = "SpydroidActivity";
    
    private HttpServer httpServer = null;
    private ImageView logo;
    private PowerManager.WakeLock wl;
    private RtspServer rtspServer = null;
    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView console, ip;
    private VideoQuality defaultVideoQuality = new VideoQuality();
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        console = (TextView) findViewById(R.id.console);
        ip = (TextView) findViewById(R.id.ip);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        defaultVideoQuality.resX = settings.getInt("video_resX", 640);
        defaultVideoQuality.resY = settings.getInt("video_resY", 480);
        defaultVideoQuality.frameRate = Integer.parseInt(settings.getString("video_framerate", "15"));
        defaultVideoQuality.bitRate = Integer.parseInt(settings.getString("video_bitrate", "500"))*1000; // 500 kb/s
        
        settings.registerOnSharedPreferenceChangeListener(this);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");
    
    	// Print version number
        /*try {
			log("<b>Spydroid v"+this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName+"</b>");
		} catch (NameNotFoundException e) {
			log("<b>Spydroid</b>");
		}*/
        
        Session.setSurfaceHolder(holder);
        Session.setDefaultVideoQuality(defaultVideoQuality);
        Session.setDefaultAudioEncoder(settings.getBoolean("stream_audio", true)?Integer.parseInt(settings.getString("audio_encoder", "1")):0);
        Session.setDefaultVideoEncoder(settings.getBoolean("stream_video", true)?Integer.parseInt(settings.getString("video_encoder", "1")):0);
        
        if (settings.getBoolean("enable_rtsp", true)) rtspServer = new RtspServer(8086, handler);
        if (settings.getBoolean("enable_http", true)) httpServer = new HttpServer(8080, this.getAssets(), handler);
        
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (key.equals("video_resX")) {
    		defaultVideoQuality.resX = sharedPreferences.getInt("video_resX", 640);
    		Session.setDefaultVideoQuality(defaultVideoQuality);
    	}
    	else if (key.equals("video_resY"))  {
    		defaultVideoQuality.resY = sharedPreferences.getInt("video_resY", 480);
    		Session.setDefaultVideoQuality(defaultVideoQuality);
    	}
    	else if (key.equals("video_framerate")) {
    		defaultVideoQuality.frameRate = Integer.parseInt(sharedPreferences.getString("video_framerate", "15"));
    		Session.setDefaultVideoQuality(defaultVideoQuality);
    	}
    	else if (key.equals("video_bitrate")) {
    		defaultVideoQuality.bitRate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "500"))*1000;
    		Session.setDefaultVideoQuality(defaultVideoQuality);
    	}
    	else if (key.equals("stream_audio") || key.equals("audio_encoder")) { 
    		Session.setDefaultAudioEncoder(sharedPreferences.getBoolean("stream_audio", true)?Integer.parseInt(sharedPreferences.getString("audio_encoder", "1")):0);
    	}
    	else if (key.equals("stream_video") || key.equals("video_encoder")) {
    		Session.setDefaultVideoEncoder(sharedPreferences.getBoolean("stream_video", true)?Integer.parseInt(sharedPreferences.getString("video_encoder", "1")):0);
    	}
    	else if (key.equals("enable_http")) {
    		if (sharedPreferences.getBoolean("enable_http", true)) {
    			httpServer =  new HttpServer(8080, this.getAssets(), handler);
    		} else {
    			if (httpServer != null) httpServer = null;
    		}
    	}
    	else if (key.equals("enable_rtsp")) {
    		if (sharedPreferences.getBoolean("enable_rtsp", true)) {
    			rtspServer =  new RtspServer(8086, handler);
    		} else {
    			if (rtspServer != null) rtspServer = null;
    		}
    	}	
    }
    
    public void onStart() {
    	super.onStart();
    	// Lock screen
    	wl.acquire();
    }
    
    public void onStop() {
    	super.onStop();
    	wl.release();
    }
    
    public void onResume() {
    	super.onResume();
    	
    	// Determines if user is connected to a wireless network & displays ip 
    	WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    	displayIpAddress(wifiInfo);
    	
    	startServers();
    	
    	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    	
    }
    
    public void onPause() {
    	super.onPause();
    	stopServers();
    	unregisterReceiver(wifiStateReceiver);
    }
    
    private void stopServers() {
    	if (rtspServer != null) rtspServer.stop();
    	if (httpServer != null) httpServer.stop();
    }
    
    private void startServers() {
    	if (rtspServer != null) {
    		try {
    			rtspServer.start();
    		} catch (IOException e) {
    			log("RtspServer could not be started : "+e.getMessage());
    		}
    	}
    	if (httpServer != null) {
    		try {
    			httpServer.start();
    		} catch (IOException e) {
    			log("HttpServer could not be started : "+e.getMessage());
    		}
    	}
    }
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		WifiInfo wifiInfo = (WifiInfo)intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
        		Log.d(TAG,"Wifi state has changed ! null?: "+(wifiInfo==null));
        		// Seems like wifiInfo is ALWAYS null on android 2
        		if (wifiInfo != null) {
        			Log.d(TAG,wifiInfo.toString());
        			displayIpAddress(wifiInfo);
        		}
        		else {
        	    	WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        	    	WifiInfo info = wifiManager.getConnectionInfo();
        	    	displayIpAddress(info);
        		}
        	}
        } 
    };
    
    // The Handler that gets information back from the RtspServer
    private final Handler handler = new Handler() {
    	
    	public void handleMessage(Message msg) {
    		
    		switch (msg.what) {
    			
    		case RtspServer.MESSAGE_LOG:
    			log((String)msg.obj);
    			break;

    		case RtspServer.MESSAGE_ERROR:
    			log((String)msg.obj);
    			break;
    			
    		case Session.MESSAGE_START:
    			// Sent when streaming starts
    			logo.setAlpha(100);
    			camera.setBackgroundDrawable(null);
    			break;
    			
    		case Session.MESSAGE_STOP:
    			// Sent when streaming ends
    			camera.setBackgroundResource(R.drawable.background);
    			logo.setAlpha(255);
    			break;

    		case Session.MESSAGE_ERROR:
    			log((String)msg.obj);
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
    	Intent intent;
    	
        switch (item.getItemId()) {
        case R.id.options:
            // Starts QualityListActivity where user can change the streaming quality
            intent = new Intent(this.getBaseContext(),OptionsActivity.class);
            startActivityForResult(intent, 0);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void displayIpAddress(WifiInfo wifiInfo) {
    	if (wifiInfo!=null && wifiInfo.getNetworkId()>-1) {
	    	int i = wifiInfo.getIpAddress();
	    	ip.setText("rtsp://");
	    	ip.append(String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff));
	    	ip.append(":8086/");
    	} else {
    		ip.setText("Wifi should be enabled !");
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