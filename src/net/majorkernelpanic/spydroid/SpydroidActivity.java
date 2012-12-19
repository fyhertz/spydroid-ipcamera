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

import net.majorkernelpanic.networking.HttpServer;
import net.majorkernelpanic.networking.RtspServer;
import net.majorkernelpanic.networking.Session;
import net.majorkernelpanic.streaming.video.H264Stream;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class SpydroidActivity extends Activity implements OnSharedPreferenceChangeListener {
    
    static final public String TAG = "SpydroidActivity";
    
    /** Default listening port for the RTSP server **/
    private final int defaultRtspPort = 8086;
    
    /** Default listening port for the HTTP server **/
    private final int defaultHttpPort = 8080;
    
    /** Default quality of video streams **/
	public static VideoQuality defaultVideoQuality = new VideoQuality(640,480,15,500000);
	
	/** By default AMR is the audio encoder **/
	public static int defaultAudioEncoder = Session.AUDIO_AMRNB;
	
	/** By default H.263 is the video encoder **/
	public static int defaultVideoEncoder = Session.VIDEO_H263;

    /** The HttpServer will use those variables to send reports about the state of the app to the http interface **/
    public static boolean activityPaused = true, notificationEnabled = true;
    public static Exception lastCaughtException;

    static private CustomHttpServer httpServer = null;
    static private RtspServer rtspServer = null;
    private PowerManager.WakeLock wl;
    private SurfaceHolder holder;
    private SurfaceView camera;
    private TextView line1, line2, version, signWifi, signStreaming;
    private ImageView buttonSettings, buttonClient, buttonAbout;
    private LinearLayout signInformation;
    private Context context;
    private Animation pulseAnimation;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        camera = (SurfaceView)findViewById(R.id.smallcameraview);
        context = this.getApplicationContext();
        line1 = (TextView)findViewById(R.id.line1);
        line2 = (TextView)findViewById(R.id.line2);
        version = (TextView)findViewById(R.id.version);
        buttonSettings = (ImageView)findViewById(R.id.button_settings);
        //buttonClient = (ImageView)findViewById(R.id.button_client);
        buttonAbout = (ImageView)findViewById(R.id.button_about);
        signWifi = (TextView)findViewById(R.id.advice);
        signStreaming = (TextView)findViewById(R.id.streaming);
        signInformation = (LinearLayout)findViewById(R.id.information);
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        settings.registerOnSharedPreferenceChangeListener(this);
       	
        camera.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder = camera.getHolder();
		
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");
    
    	// Print version number
        try {
			version.setText("v"+this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName);
		} catch (Exception e) {
			version.setText("v???");
		}
        
        // On android 3.* AAC is not supported so we set the default encoder to AMR-NB, on android 4.* AAC is the default encoder
        defaultAudioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? Session.AUDIO_AMRNB : Session.AUDIO_AAC;
        
        Session.setSurfaceHolder(holder);
        Session.setHandler(handler);
        Session.setDefaultAudioEncoder(settings.getBoolean("stream_audio", false)?Integer.parseInt(settings.getString("audio_encoder", String.valueOf(defaultAudioEncoder))):0);
        Session.setDefaultVideoEncoder(settings.getBoolean("stream_video", true)?Integer.parseInt(settings.getString("video_encoder", "2")):0);
        H264Stream.setPreferences(settings);
        
        // Read video quality settings from the preferences 
        defaultVideoQuality = VideoQuality.merge(
        		new VideoQuality(
        				settings.getInt("video_resX", 0),
        				settings.getInt("video_resY", 0), 
        				Integer.parseInt(settings.getString("video_framerate", "0")), 
        				Integer.parseInt(settings.getString("video_bitrate", "0"))*1000
        		),
        		defaultVideoQuality);

        Session.setDefaultVideoQuality(defaultVideoQuality);

        if (rtspServer == null) rtspServer = new RtspServer(defaultRtspPort, handler);
        if (httpServer == null) httpServer = new CustomHttpServer(defaultHttpPort, this.getApplicationContext(), handler);

        buttonSettings.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
	            // Starts QualityListActivity where user can change the quality of the stream
				Intent intent = new Intent(context,OptionsActivity.class);
	            startActivityForResult(intent, 0);
			}
		});        
        /*buttonClient.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Starts ClientActivity, the user can then capture the stream from another phone running Spydroid
	            Intent intent = new Intent(context,ClientActivity.class);
	            startActivityForResult(intent, 0);
			}
		});*/
        buttonAbout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
	            // Display some information
	            Intent intent = new Intent(context,AboutActivity.class);
	            startActivityForResult(intent, 0);
			}
		});
        
        // Did the user disabled the notification ?
        notificationEnabled = settings.getBoolean("notification_enabled", true);
        
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (key.equals("video_resX") || key.equals("video_resY")) {
    		defaultVideoQuality.resX = sharedPreferences.getInt("video_resX", 0);
    		defaultVideoQuality.resY = sharedPreferences.getInt("video_resY", 0);
    	}
    	else if (key.equals("video_framerate")) {
    		defaultVideoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
    	}
    	else if (key.equals("video_bitrate")) {
    		defaultVideoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
    	}
    	else if (key.equals("stream_audio")) {
    		if (!sharedPreferences.getBoolean("stream_audio", true)) Session.setDefaultAudioEncoder(0);
    	}
    	else if (key.equals("audio_encoder")) { 
    		defaultAudioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", "0"));
    		Session.setDefaultAudioEncoder( defaultAudioEncoder );
    	}
    	else if (key.equals("stream_video")) {
    		if (!sharedPreferences.getBoolean("stream_video", true)) Session.setDefaultVideoEncoder(0);
    	}
    	else if (key.equals("video_encoder")) {
    		defaultVideoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", "0"));
    		Session.setDefaultVideoEncoder( defaultVideoEncoder );
    	}
    	else if (key.equals("enable_http")) {
    		if (sharedPreferences.getBoolean("enable_http", true)) {
    			if (httpServer == null) httpServer = new CustomHttpServer(defaultHttpPort, this.getApplicationContext(), handler);
    		} else {
    			if (httpServer != null) {
    				httpServer.stop();
    				httpServer = null;
    			}
    		}
    	}
    	else if (key.equals("enable_rtsp")) {
    		if (sharedPreferences.getBoolean("enable_rtsp", true)) {
    			if (rtspServer == null) rtspServer = new RtspServer(defaultRtspPort, handler);
    		} else {
    			if (rtspServer != null) {
    				rtspServer.stop();
    				rtspServer = null;
    			}
    		}
    	}
    	else if (key.equals("notification_enabled")) {
    		notificationEnabled  = sharedPreferences.getBoolean("notification_enabled", true);
    		removeNotification();
    	}
    }
    
    public void onStart() {
    	super.onStart();
    	
    	// Lock screen
    	wl.acquire();
    	
    	
    	if (notificationEnabled) {
    		Intent notificationIntent = new Intent(this, SpydroidActivity.class);
    		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
    		Notification notification = builder.setContentIntent(pendingIntent)
    				.setWhen(System.currentTimeMillis())
    				.setTicker(getText(R.string.notification_title))
    				.setSmallIcon(R.drawable.icon)
    				.setContentTitle(getText(R.string.notification_title))
    				.setContentText(getText(R.string.notification_content)).build();
    		notification.flags |= Notification.FLAG_ONGOING_EVENT;
    		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
    	}
    	
    }
    	
    public void onStop() {
    	super.onStop();
    	// A WakeLock should only be released when isHeld() is true !
    	if (wl.isHeld()) wl.release();
    }
    
    public void onResume() {
    	super.onResume();
    	// Determines if user is connected to a wireless network & displays ip 
    	if (!streaming) displayIpAddress();
    	activityPaused = true;
    	startServers();
    	registerReceiver(wifiStateReceiver,new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }
    
    public void onPause() {
    	super.onPause();
    	activityPaused = false;
    	unregisterReceiver(wifiStateReceiver);
    }
    
    public void onDestroy() {
    	super.onDestroy();
    }
    
    public void onBackPressed() {
    	Intent setIntent = new Intent(Intent.ACTION_MAIN);
    	setIntent.addCategory(Intent.CATEGORY_HOME);
    	setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(setIntent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	
        switch (item.getItemId()) {
        /*case R.id.client:
            // Starts ClientActivity where user can view stream from another phone
            intent = new Intent(this.getBaseContext(),ClientActivity.class);
            startActivityForResult(intent, 0);
            return true;*/
        case R.id.options:
            // Starts QualityListActivity where user can change the streaming quality
            intent = new Intent(this.getBaseContext(),OptionsActivity.class);
            startActivityForResult(intent, 0);
            return true;
        case R.id.quit:
        	// Quits Spydroid i.e. stops the HTTP & RTSP servers
        	stopServers();  
        	// Remove notification
        	if (notificationEnabled) removeNotification();          	
        	finish();	
            return true;
        default:
            return super.onOptionsItemSelected(item);
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
    		try {
    			httpServer.start();
    		} catch (IOException e) {
    			log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    }

    private void stopServers() {
    	if (httpServer != null) {
    		httpServer.stop();
    		httpServer = null;
    	}
    	if (rtspServer != null) {
    		rtspServer.stop();
    		rtspServer = null;
    	}
    }
    
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		if (!streaming) displayIpAddress();
        	}
        } 
    };
    
    private boolean streaming = false;
    
    // The Handler that gets information back from the RtspServer and Session
    private final Handler handler = new Handler() {
    	
    	public void handleMessage(Message msg) { 
    		switch (msg.what) {
    		case RtspServer.MESSAGE_ERROR:
    			Exception e1 = (Exception)msg.obj;
    			lastCaughtException = e1;
    			log(e1.getMessage()!=null?e1.getMessage():"An error occurred !");
    			break;
    		case RtspServer.MESSAGE_LOG:
    			//log((String)msg.obj);
    			break;
    		case HttpServer.MESSAGE_ERROR:
    			Exception e2 = (Exception)msg.obj;
    			lastCaughtException = e2;
    			break;    			
    		case Session.MESSAGE_START:
    			streaming = true;
    			streamingState(1);
    			break;
    		case Session.MESSAGE_STOP:
    			streaming = false;
    			displayIpAddress();
    			break;
    		}
    	}
    	
    };
    
    private void displayIpAddress() {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiManager.getConnectionInfo();
    	if (info!=null && info.getNetworkId()>-1) {
	    	int i = info.getIpAddress();
	    	String ip = String.format("%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
	    	line1.setText("HTTP://");
	    	line1.append(ip);
	    	line1.append(":"+defaultHttpPort);
	    	line2.setText("RTSP://");
	    	line2.append(ip);
	    	line2.append(":"+defaultRtspPort);
	    	streamingState(0);
    	} else {
    		line1.setText("HTTP://xxx.xxx.xxx.xxx:"+defaultHttpPort);
    		line2.setText("RTSP://xxx.xxx.xxx.xxx:"+defaultRtspPort);
    		streamingState(2);
    	}
    }
    
    public void log(String s) {
    	Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

	private void streamingState(int state) {
		// Not streaming
		if (state==0) {
			signStreaming.clearAnimation();
			signWifi.clearAnimation();
			signStreaming.setVisibility(View.GONE);
			signInformation.setVisibility(View.VISIBLE);
			signWifi.setVisibility(View.GONE);
		} else if (state==1) {
			// Streaming
			signWifi.clearAnimation();
			signStreaming.setVisibility(View.VISIBLE);
			signStreaming.startAnimation(pulseAnimation);
			signInformation.setVisibility(View.INVISIBLE);
			signWifi.setVisibility(View.GONE);
		} else if (state==2) {
			// No wifi !
			signStreaming.clearAnimation();
			signStreaming.setVisibility(View.GONE);
			signInformation.setVisibility(View.INVISIBLE);
			signWifi.setVisibility(View.VISIBLE);
			signWifi.startAnimation(pulseAnimation);
		}
	}
	
	private void removeNotification() {
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
		if (httpServer != null) {
			httpServer.stop();
			httpServer = null;
		}
		if (rtspServer != null) {
			rtspServer.stop();
			rtspServer = null;
		}
	}
    
}