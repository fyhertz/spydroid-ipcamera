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

import java.net.InetAddress;

import net.majorkernelpanic.libstreaming.RtspServer;
import net.majorkernelpanic.libstreaming.TestH264;
import net.majorkernelpanic.libstreaming.VideoQuality;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.Html;
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
public class SpydroidActivity extends Activity {
    
    static final public String TAG = "SPYDROID";
    
    private ViewGroup topLayout;
    private TextView console, ip;
    private ImageView logo;
    private SharedPreferences settings;
    private SurfaceView camera;
    private SurfaceHolder holder;
    private int resX, resY, fps, br;
    private PowerManager.WakeLock wl;
    
    private static RtspServer rtspServer;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        topLayout = (ViewGroup) findViewById(R.id.mainlayout);
        console = (TextView) findViewById(R.id.console);
        ip = (TextView) findViewById(R.id.ip);
        
        settings = getSharedPreferences("spydroid-ipcamera-prefs", 0);
       	resX = settings.getInt("resX", 640);
       	resY = settings.getInt("resY", 480);
       	fps = settings.getInt("fps", 15);
       	br = settings.getInt("br", 1000);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");
        
        startRtspServer();
    
    	// Print version number
        try {
			log("<b>Spydroid v"+this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName+"</b>");
		} catch (NameNotFoundException e) {
			log("<b>Spydroid</b>");
		}
        
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
    		
    		// Sent when H264 support needs to be tested 
    		// it will then call rtspServer.h264TestResult to pass sps and pps parameters to streamingManager
    		case RtspServer.MESSAGE_H264_TEST:
    			log("Testing H264 support: "+resX+"x"+resY+","+fps+" fps");
    			TestH264.RunTest(SpydroidActivity.this.getCacheDir(),camera.getHolder(), resX, resY, fps, new TestH264.Callback() {
    				public void onError(String error) {
    					log(error);
    					log("Something went wrong !");
    				}
    				public void onSuccess(String result) {
    					log("H264 supported !");
    		    		// The test returns sps and pps parameters, the client needs them to decode the stream
    					VideoQuality videoQuality = new VideoQuality(resX, resY, fps, br*1000);
    					rtspServer.h264TestResult(videoQuality, result.split(":"), holder);
    				}
    			});
    			
    			break;
    			
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
    
    public void startRtspServer() {
    	rtspServer = new RtspServer(8086, handler);
    	rtspServer.start();
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
    		intent.putExtra("net.majorkernelpanic.spydroid.resX", resX );
    		intent.putExtra("net.majorkernelpanic.spydroid.resY", resY );
    		intent.putExtra("net.majorkernelpanic.spydroid.fps", fps );
    		intent.putExtra("net.majorkernelpanic.spydroid.br", br );
    		startActivityForResult(intent, 0);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	// User has changed quality
    	if (resultCode==1000) {

    		resX = data.getIntExtra("net.majorkernelpanic.spydroid.resX", 0);
    		resY = data.getIntExtra("net.majorkernelpanic.spydroid.resY", 0);
    		fps = data.getIntExtra("net.majorkernelpanic.spydroid.fps", 0);
    		br = data.getIntExtra("net.majorkernelpanic.spydroid.br", 0);

    		SharedPreferences.Editor editor = settings.edit();
    		editor.putInt("resX", resX);
    		editor.putInt("resY", resY);
    		editor.putInt("fps", fps);
    		editor.putInt("br", br);
    	    editor.commit();
    		
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