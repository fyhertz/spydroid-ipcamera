/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
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

import java.io.IOException;

import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.spydroid.api.CustomHttpServer;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.misc.HttpServer;
import net.majorkernelpanic.streaming.misc.RtspServer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.widget.Toast;

/** 
 * Spydroid launches an RtspServer, clients can then connect to it and receive audio/video streams from the phone
 */
public class SpydroidActivity extends FragmentActivity implements OnSharedPreferenceChangeListener {
    
    static final public String TAG = "SpydroidActivity";
    
    static private CustomHttpServer mHttpServer = null;
    static private RtspServer mRtspServer = null;
    
    // The HttpServer will use those variables to send reports about the state of the app to the http interface
    public static boolean activityPaused = true;
    public static Exception lastCaughtException;

    // Prevent garbage collection of the Surface
    private boolean videoHackEnabled = false;
    private Surface videoHackSurface;

    private ViewPager mViewPager;
    private PowerManager.WakeLock mWakeLock;
    private SectionsPagerAdapter mAdapter;
    private boolean streaming = false, notificationEnabled = true;
    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.spydroid);
        
        if (findViewById(R.id.handset_pager) != null) {
        	// Handset detected
            mAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),SectionsPagerAdapter.HANDSET);
        	mViewPager = (ViewPager) findViewById(R.id.handset_pager);
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
        	// Tablet detected
        	mAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),SectionsPagerAdapter.TABLET);
        	mViewPager = (ViewPager) findViewById(R.id.tablet_pager);
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        
        mViewPager.setAdapter(mAdapter);
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "net.majorkernelpanic.spydroid.wakelock");

        Session.setHandler(mHandler);
        
        if (mRtspServer == null) mRtspServer = new RtspServer(SpydroidApplication.RtspPort, mHandler);
        if (mHttpServer == null) mHttpServer = new CustomHttpServer(SpydroidApplication.HttpPort, this.getApplicationContext(), mHandler);    	
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);        
        notificationEnabled = settings.getBoolean("notification_enabled", true);
        
    }
    
    class SectionsPagerAdapter extends FragmentPagerAdapter {

    	public static final int HANDSET = 0x01;
    	public static final int TABLET = 0x02;
    	
    	private int mode = 1; 
    	private Fragment[] fragmentList;
    	
        public SectionsPagerAdapter(FragmentManager fm, int mode) {
            super(fm);
            this.mode = mode;
            if (mode == HANDSET) fragmentList = new Fragment[] {new HandsetFragment(),new PreviewFragment(),new AboutFragment()};
            else fragmentList = new Fragment[] {new TabletFragment(),new AboutFragment()};
        }

        @Override
        public Fragment getItem(int i) {
            return fragmentList[i];
        }

        @Override
        public int getCount() {
            return mode==HANDSET ? 3 : 2;
        }

        public HandsetFragment getHandsetFragment() {
        	return (getItem(0).getView() != null) ? (HandsetFragment) getItem(0) : null;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
        	if (mode == HANDSET) {
        		switch (position) {
        		case 0: return "Streaming";
        		case 1: return "Preview";
        		case 2: return "Help";
        		}        		
        	} else {
        		switch (position) {
        		case 0: return "Streaming";
        		case 1: return "Help";
        		}
        	}
            return null;
        }
    }
	
    public void onStart() {
    	super.onStart();
    	
    	// Lock screen
    	mWakeLock.acquire();
    	
    	// Did the user disabled the notification ?
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
    	if (mWakeLock.isHeld()) mWakeLock.release();
    }
    
    public void onResume() {
    	super.onResume();
    	// Determines if user is connected to a wireless network & displays ip 
    	if (!streaming) {
    		if (mAdapter.getHandsetFragment() != null) mAdapter.getHandsetFragment().displayIpAddress();
    	}
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
    	Log.d(TAG,"SpydroidActivity destroyed");
    	super.onDestroy();
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (key.equals("video_resX") || key.equals("video_resY")) {
    		SpydroidApplication.videoQuality.resX = sharedPreferences.getInt("video_resX", 0);
    		SpydroidApplication.videoQuality.resY = sharedPreferences.getInt("video_resY", 0);
    	}
    	else if (key.equals("video_framerate")) {
    		SpydroidApplication.videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
    	}
    	else if (key.equals("video_bitrate")) {
    		SpydroidApplication.videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
    	}
    	else if (key.equals("audio_encoder") || key.equals("stream_audio")) { 
    		SpydroidApplication.audioEncoder = Integer.parseInt(sharedPreferences.getString("audio_encoder", "0"));
    		Session.setDefaultAudioEncoder( SpydroidApplication.audioEncoder );
    		if (!sharedPreferences.getBoolean("stream_audio", false)) Session.setDefaultAudioEncoder(0);
    	}
    	else if (key.equals("stream_video") || key.equals("video_encoder")) {
    		SpydroidApplication.videoEncoder = Integer.parseInt(sharedPreferences.getString("video_encoder", "0"));
    		Session.setDefaultVideoEncoder( SpydroidApplication.videoEncoder );
    		if (!sharedPreferences.getBoolean("stream_video", true)) Session.setDefaultVideoEncoder(0);
    	}
    	else if (key.equals("enable_http")) {
    		if (sharedPreferences.getBoolean("enable_http", true)) {
    			if (mHttpServer == null) mHttpServer = new CustomHttpServer(SpydroidApplication.HttpPort, this.getApplicationContext(), mHandler);
    		} else {
    			if (mHttpServer != null) {
    				mHttpServer.stop();
    				mHttpServer = null;
    			}
    		}
    	}
    	else if (key.equals("enable_rtsp")) {
    		if (sharedPreferences.getBoolean("enable_rtsp", true)) {
    			if (mRtspServer == null) mRtspServer = new RtspServer(SpydroidApplication.RtspPort, mHandler);
    		} else {
    			if (mRtspServer != null) {
    				mRtspServer.stop();
    				mRtspServer = null;
    			}
    		}
    	}
    	else if (key.equals("notification_enabled")) {
    		notificationEnabled  = sharedPreferences.getBoolean("notification_enabled", true);
    		removeNotification();
    	}
    	else if (key.equals("video_hack")) {
    		videoHackEnabled = sharedPreferences.getBoolean("video_hack", false);
    		//Session.setSurfaceHolder(holder,!videoHackEnabled); 
    	}
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
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.quit), 1);
        MenuItemCompat.setShowAsAction(menu.findItem(R.id.options), 1);
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
    	if (mRtspServer != null) {
    		try {
    			mRtspServer.start();
    		} catch (IOException e) {
    			log("RtspServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    	if (mHttpServer != null) {
    		try {
    			mHttpServer.start();
    		} catch (IOException e) {
    			log("HttpServer could not be started : "+(e.getMessage()!=null?e.getMessage():"Unknown error"));
    		}
    	}
    }

    private void stopServers() {
    	if (mHttpServer != null) {
    		mHttpServer.stop();
    		mHttpServer = null;
    	}
    	if (mRtspServer != null) {
    		mRtspServer.stop();
    		mRtspServer = null;
    	}
    }
    
    
    // BroadcastReceiver that detects wifi state changements
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	// This intent is also received when app resumes even if wifi state hasn't changed :/
        	if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
        		if (!streaming && mAdapter.getHandsetFragment() != null) mAdapter.getHandsetFragment().displayIpAddress();
        	}
        } 
    };
    
    // The Handler that gets information back from the RtspServer and Session
    private final Handler mHandler = new Handler() {
    	
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
    			if (mAdapter.getHandsetFragment() != null) mAdapter.getHandsetFragment().streamingState(1);
    			break;
    		case Session.MESSAGE_STOP:
    			streaming = false;
    			if (mAdapter.getHandsetFragment() != null) mAdapter.getHandsetFragment().displayIpAddress();
    			break;
    		}
    	}
    	
    };
    
	private void removeNotification() {
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
	}
    
    public void log(String s) {
    	Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
    
}