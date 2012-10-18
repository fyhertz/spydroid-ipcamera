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

import net.majorkernelpanic.streaming.audio.AACStream;
import net.majorkernelpanic.streaming.video.H264Stream;
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
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class SpydroidActivity extends Activity implements OnSharedPreferenceChangeListener {
    
    static final public String TAG = "SpydroidActivity"; 
    
    private HttpServer httpServer = null;
    private ImageView logo, led;
    private PowerManager.WakeLock wl;
    private RtspServer rtspServer = null;
    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView console, status;
    private VideoQuality defaultVideoQuality = new VideoQuality();
    private Display display;
    private Context context;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        logo = (ImageView)findViewById(R.id.logo);
        //console = (TextView) findViewById(R.id.console);
        status = (TextView) findViewById(R.id.status);
        display = getWindowManager().getDefaultDisplay();
        context = this.getApplicationContext();
        led = (ImageView)findViewById(R.id.led);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        H264Stream.setPreferences(settings);
        AACStream.setAACSupported(android.os.Build.VERSION.SDK_INT>=14);
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
        if (settings.getBoolean("enable_http", true)) httpServer = new HttpServer(8080, this.getApplicationContext(), handler);
        
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
    			httpServer =  new HttpServer(8080, this.getApplicationContext(), handler);
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
    	displayIpAddress();
    	
    	startServers();
    	
    	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    	//handler.postDelayed(logoAnimation, 7000);
    	
    }
    
    public void onPause() {
    	super.onPause();
    	stopServers();
    	unregisterReceiver(wifiStateReceiver);
    	//handler.removeCallbacks(logoAnimation);
    }
    
    private void stopServers() {
    	if (rtspServer != null) rtspServer.stop();
    	if (httpServer != null) {
    		httpServer.setScreenState(false);
    		//httpServer.stop();
    	}
    }
    
    private void startServers() {
    	if (rtspServer != null) {
    		try {
    			rtspServer.start();
    		} catch (IOException e) {
    			log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    	if (httpServer != null) {
    		httpServer.setScreenState(true);
    		try {
    			httpServer.start();
    		} catch (IOException e) {
    			log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    }
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		displayIpAddress();
        	}
        } 
    };
    
    private boolean streaming = false;
    
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
    			if (!streaming) handler.postDelayed(ledAnimation, 100);
    			streaming = true;
    			status.setText(R.string.streaming);
    			break;
    		case Session.MESSAGE_STOP:
    			streaming = false;
    			handler.removeCallbacks(ledAnimation);
    			displayIpAddress();
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
    
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	status.setText("http://");
	    	status.append(String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff));
	    	status.append(":8080/");
	    	led.setImageResource(R.drawable.led_green);
    	} else {
    		led.setImageResource(R.drawable.led_red);
    		status.setText(R.string.warning);
    	}
    }
    
    public void log(String s) {
    	/*String t = console.getText().toString();
    	if (t.split("\n").length>8) {
    		console.setText(t.substring(t.indexOf("\n")+1, t.length()));
    	}
    	console.append(Html.fromHtml(s+"<br />"));*/
    	Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

	private Runnable logoAnimation = new Runnable() {
		public void run() {
			runLogoAnimation();
			handler.postDelayed(this,7000);
		}
	};
    
	private boolean ledState = true; 
	
	private void toggleLed() {
		if (ledState) {
			ledState = false;
			led.setImageResource(R.drawable.led_green);
		} else {
			ledState = true;
			led.setImageResource(getResources().getColor(android.R.color.transparent));
		}
	}
	
	private Runnable ledAnimation = new Runnable() {
		public void run() {
			toggleLed();
			handler.postDelayed(this,900);
		}
	};
	
	private void runLogoAnimation() { 
		int width = display.getWidth(), height = display.getHeight();
		int side = (int) (Math.random()*4);
		int position = (int) (side<2?(width-256)*Math.random():(height-256)*Math.random());
		
		RotateAnimation rotateAnimation = new RotateAnimation(0, side==0?180:(side==1?0:(side==2?90:270)),Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
		TranslateAnimation translateAnimation = new TranslateAnimation(
				Animation.ABSOLUTE, side<2?position:(side==2?-200:width), 
				Animation.ABSOLUTE, side<2?position:(side==2?-100:width-80), 
				Animation.ABSOLUTE, side>=2?position:(side==0?-200:height), 
				Animation.ABSOLUTE, side>=2?position:(side==0?-110:height-80));
		
		rotateAnimation.setDuration(0);
		rotateAnimation.setFillAfter(true);
		translateAnimation.setStartOffset(1500);
		translateAnimation.setDuration(1500);
		translateAnimation.setRepeatCount(1);
		translateAnimation.setRepeatMode(Animation.REVERSE);
		translateAnimation.setFillAfter(true);
		
		AnimationSet animationSet = new AnimationSet(true);
		
		animationSet.setAnimationListener(new AnimationListener() {
			public void onAnimationEnd(Animation animation) {
				logo.setVisibility(View.INVISIBLE);
			}
			public void onAnimationRepeat(Animation animation) {}
			public void onAnimationStart(Animation animation) {}
		});
		
		animationSet.addAnimation(rotateAnimation);
		animationSet.addAnimation(translateAnimation);
		
		logo.startAnimation(animationSet);
		logo.setVisibility(View.VISIBLE);
		
	}
    
    
}